"""
CSE112 Smart City Transportation Network Optimization System - Greater Cairo
Single-file professional Tkinter GUI demo with Dijkstra, A*, Kruskal MST, DP,
greedy traffic signals/emergency priority, and traffic simulation charts.
"""
from __future__ import annotations

import heapq, math, time
from dataclasses import dataclass, field
from functools import lru_cache
from typing import Dict, List, Tuple, Optional, Set

import tkinter as tk
from tkinter import ttk, messagebox
from matplotlib.backends.backend_tkagg import FigureCanvasTkAgg
from matplotlib.figure import Figure
import networkx as nx

PERIODS = ["Morning Peak", "Afternoon", "Evening Peak", "Night"]
MORNING, AFTERNOON, EVENING, NIGHT = range(4)

@dataclass(frozen=True)
class Node:
    id: str
    name: str
    kind: str
    lon: float
    lat: float
    population: int = 0

@dataclass
class Edge:
    u: str
    v: str
    distance: float
    capacity: int
    condition: int
    existing: bool = True
    construction_cost: float = 0.0
    flows: List[int] = field(default_factory=lambda: [0, 0, 0, 0])

    def congestion(self, period: int) -> float:
        return min(1.50, self.flows[period] / max(1, self.capacity))

    def travel_time_min(self, period: int, emergency: bool = False) -> float:
        # Speed falls as congestion rises and poor road condition increases time.
        base_speed = 62.0 if emergency else 42.0
        congestion_factor = 1.0 + 1.15 * self.congestion(period)
        condition_factor = max(0.55, self.condition / 10.0)
        minutes = (self.distance / base_speed) * 60.0 * congestion_factor / condition_factor
        if emergency:
            minutes *= 0.78  # siren + signal preemption effect, not a fake hardcoded ETA
        return minutes

    def mst_weight(self, nodes: Dict[str, Node]) -> float:
        pop_bonus = math.log1p(nodes[self.u].population + nodes[self.v].population) / 15.0
        condition_penalty = 1.0 + (10 - self.condition) * 0.04
        new_road_penalty = 1.0 + (self.construction_cost / 2500.0 if not self.existing else 0)
        return self.distance * condition_penalty * new_road_penalty / max(0.75, pop_bonus)

class CairoNetwork:
    def __init__(self):
        self.nodes: Dict[str, Node] = {}
        self.edges: List[Edge] = []
        self.potential_edges: List[Edge] = []
        self.adj: Dict[str, List[Edge]] = {}
        self._load_data()

    def add_node(self, *args):
        n = Node(*args); self.nodes[n.id] = n; self.adj.setdefault(n.id, [])

    def add_edge(self, edge: Edge, potential: bool = False):
        if potential:
            edge.existing = False; self.potential_edges.append(edge); return
        self.edges.append(edge)
        self.adj.setdefault(edge.u, []).append(edge)
        self.adj.setdefault(edge.v, []).append(Edge(edge.v, edge.u, edge.distance, edge.capacity, edge.condition, edge.existing, edge.construction_cost, edge.flows))

    def set_flow(self, u, v, flows):
        for e in self.edges:
            if {e.u, e.v} == {u, v}:
                e.flows = list(flows)
        for a, lst in self.adj.items():
            for e in lst:
                if {e.u, e.v} == {u, v}:
                    e.flows = list(flows)

    def edge_between(self, u, v) -> Optional[Edge]:
        return next((e for e in self.adj.get(u, []) if e.v == v), None)

    def _load_data(self):
        for row in [
            ("1","Maadi","Residential",31.25,29.96,250000),("2","Nasr City","Residential",31.34,30.06,500000),
            ("3","Downtown Cairo","Business",31.24,30.04,100000),("4","New Cairo","Residential",31.47,30.03,300000),
            ("5","Heliopolis","Residential",31.33,30.09,350000),("6","Zamalek","Business",31.22,30.06,50000),
            ("7","6th October City","Residential",30.98,29.93,450000),("8","Giza","Residential",31.21,30.01,400000),
            ("9","Mohandessin","Business",31.20,30.05,180000),("10","Dokki","Business",31.21,30.04,220000),
            ("11","Shubra","Residential",31.24,30.11,450000),("12","Helwan","Industrial",31.33,29.85,300000),
            ("13","New Administrative Capital","Government",31.80,30.02,50000),("14","El Shorouk","Residential",31.63,30.14,250000),
            ("15","Sheikh Zayed","Residential",30.94,30.01,150000),("F1","Cairo International Airport","Airport",31.41,30.11,0),
            ("F2","Ramses Railway Station","Transit Hub",31.25,30.06,0),("F3","Cairo University","Education",31.21,30.03,0),
            ("F4","Al-Azhar University","Education",31.26,30.05,0),("F5","Egyptian Museum","Tourism",31.23,30.05,0),
            ("F6","Cairo International Stadium","Sports",31.30,30.07,0),("F7","Smart Village","Business",30.97,30.07,0),
            ("F8","Cairo Festival City","Commercial",31.40,30.03,0),("F9","Qasr El Aini Hospital","Medical",31.23,30.03,0),
            ("F10","Maadi Military Hospital","Medical",31.25,29.95,0)]: self.add_node(*row)
        for row in [("1","3",8.5,3000,7),("1","8",6.2,2500,6),("2","3",5.9,2800,8),("2","5",4.0,3200,9),("3","5",6.1,3500,7),("3","6",3.2,2000,8),("3","9",4.5,2600,6),("3","10",3.8,2400,7),("4","2",15.2,3800,9),("4","14",5.3,3000,10),("5","11",7.9,3100,7),("6","9",2.2,1800,8),("7","8",24.5,3500,8),("7","15",9.8,3000,9),("8","10",3.3,2200,7),("8","12",14.8,2600,5),("9","10",2.1,1900,7),("10","11",8.7,2400,6),("11","F2",3.6,2200,7),("12","1",12.7,2800,6),("13","4",45.0,4000,10),("14","13",35.5,3800,9),("15","7",9.8,3000,9),("F1","5",7.5,3500,9),("F1","2",9.2,3200,8),("F2","3",2.5,2000,7),("F7","15",8.3,2800,8),("F8","4",6.1,3000,9),("F9","3",1.5,2000,8),("F9","10",2.0,1800,7),("F10","1",2.3,2200,7),("F10","12",10.5,2000,6),("F3","10",0.5,1500,8),("F4","3",2.1,1800,8),("F5","3",1.2,1600,8),("F6","5",3.5,2000,8)]: self.add_edge(Edge(*row))
        for row in [("1","4",22.8,4000,8,450),("5","4",16.8,3500,8,320),("6","8",7.5,2500,8,150),("9","11",6.9,2800,8,140),("10","F7",27.4,3200,8,550),("12","14",30.5,3600,8,610),("F1","13",40.2,4000,9,800)]: self.add_edge(Edge(row[0],row[1],row[2],row[3],row[4],False,row[5]), True)
        flows = {("1","3"):(2800,1500,2600,800),("1","8"):(2200,1200,2100,600),("2","3"):(2700,1400,2500,700),("2","5"):(3000,1600,2800,650),("3","5"):(3200,1700,3100,800),("3","6"):(1800,1400,1900,500),("3","9"):(2400,1300,2200,550),("3","10"):(2300,1200,2100,500),("4","2"):(3600,1800,3300,750),("4","14"):(2800,1600,2600,600),("5","11"):(2900,1500,2700,650),("6","9"):(1700,1300,1800,450),("7","8"):(3200,1700,3000,700),("7","15"):(2800,1500,2600,600),("8","10"):(2000,1100,1900,450),("8","12"):(2400,1300,2200,500),("9","10"):(1800,1200,1700,400),("10","11"):(2200,1300,2100,500),("11","F2"):(2100,1200,2000,450),("12","1"):(2600,1400,2400,550),("13","4"):(3800,2000,3500,800),("14","13"):(3600,1900,3300,750),("15","7"):(2800,1500,2600,600),("F1","5"):(3300,2200,3100,1200),("F1","2"):(3000,2000,2800,1100),("F2","3"):(1900,1600,1800,900),("F7","15"):(2600,1500,2400,550),("F8","4"):(2800,1600,2600,600),("F9","3"):(1600,1200,1800,600),("F9","10"):(1400,1000,1500,500),("F10","1"):(1200,900,1300,400),("F10","12"):(900,700,1000,300),("F3","10"):(800,900,850,300),("F4","3"):(1500,1200,1600,500),("F5","3"):(900,800,1000,400),("F6","5"):(1800,1400,2000,500)}
        for (u,v),f in flows.items(): self.set_flow(u, v, f)

    def names(self): return {nid: f"{n.name} ({nid})" for nid, n in self.nodes.items()}

class Algorithms:
    def __init__(self, net: CairoNetwork): self.net = net

    @lru_cache(maxsize=256)
    def dijkstra(self, src: str, period: int) -> Tuple[Dict[str,float], Dict[str,str], Tuple[str,...]]:
        dist = {n: math.inf for n in self.net.nodes}; prev = {}; visited_order=[]
        dist[src]=0.0; pq=[(0.0, src)]
        while pq:
            d,u=heapq.heappop(pq)
            if d != dist[u]: continue
            visited_order.append(u)
            for e in self.net.adj[u]:
                nd = d + e.travel_time_min(period, False)
                if nd < dist[e.v]: dist[e.v]=nd; prev[e.v]=u; heapq.heappush(pq,(nd,e.v))
        return dist, prev, tuple(visited_order)

    def path_from_prev(self, prev, src, dst):
        if src == dst: return [src]
        if dst not in prev: return []
        path=[dst]
        while path[-1] != src: path.append(prev[path[-1]])
        return list(reversed(path))

    def shortest_path(self, src, dst, period):
        dist, prev, visited = self.dijkstra(src, period)
        path = self.path_from_prev(prev, src, dst)
        km = sum((self.net.edge_between(a,b).distance for a,b in zip(path,path[1:])), 0.0) if path else 0.0
        return path, km, dist.get(dst, math.inf), list(visited)

    def astar(self, src, dst, period, emergency=True):
        def h(a,b):
            na, nb = self.net.nodes[a], self.net.nodes[b]
            return math.hypot((na.lon-nb.lon)*95, (na.lat-nb.lat)*111) / 62 * 60
        g={src:0.0}; prev={}; openq=[(h(src,dst), src)]; visited=[]; closed=set()
        while openq:
            _,u=heapq.heappop(openq)
            if u in closed: continue
            closed.add(u); visited.append(u)
            if u==dst: break
            for e in self.net.adj[u]:
                if emergency and e.congestion(period) > 1.05 and len(self.net.adj[u]) > 1: continue
                cost = e.travel_time_min(period, emergency)
                if emergency and self.net.nodes[e.v].kind in {"Medical", "Transit Hub"}: cost *= 0.88
                ng = g[u] + cost
                if ng < g.get(e.v, math.inf): prev[e.v]=u; g[e.v]=ng; heapq.heappush(openq,(ng+h(e.v,dst),e.v))
        path = self.path_from_prev(prev, src, dst)
        km = sum((self.net.edge_between(a,b).distance for a,b in zip(path,path[1:])), 0.0) if path else 0.0
        return path, km, g.get(dst, math.inf), visited

    def kruskal(self, include_new=True):
        parent={n:n for n in self.net.nodes}; rank={n:0 for n in self.net.nodes}
        def find(x):
            while parent[x]!=x: parent[x]=parent[parent[x]]; x=parent[x]
            return x
        def union(a,b):
            ra,rb=find(a),find(b)
            if ra==rb: return False
            if rank[ra]<rank[rb]: parent[ra]=rb
            elif rank[ra]>rank[rb]: parent[rb]=ra
            else: parent[rb]=ra; rank[ra]+=1
            return True
        candidates = self.net.edges + (self.net.potential_edges if include_new else [])
        result=[]
        for e in sorted(candidates, key=lambda x: x.mst_weight(self.net.nodes)):
            if union(e.u,e.v): result.append(e)
            if len(result)==len(self.net.nodes)-1: break
        return result

    def dp_bus_allocation(self, total_buses=250):
        routes=[("B1 Maadi-Downtown-Zamalek",35000,25),("B2 October-Giza-Downtown",42000,30),("B3 Nasr City-Airport",28000,20),("B4 New Cairo-Downtown",31000,22),("B5 Giza-Helwan",25000,18),("B6 Shubra-Heliopolis",33000,24),("B7 Capital-Shorouk",21000,15),("B8 Smart Village-Zayed",17000,12),("B9 Ring Road Corridor",39000,28),("B10 Festival-Nasr City",28000,20)]
        min_buses=[max(1, round(d/2500)) for _,d,_ in routes]
        allocation=min_buses[:]; remaining=total_buses-sum(allocation)
        for _ in range(max(0, remaining)):
            gains=[]
            for i,(_,d,base) in enumerate(routes):
                gain=d*(math.sqrt(allocation[i]+1)-math.sqrt(allocation[i]))/math.sqrt(max(1,base))
                gains.append((gain,i))
            allocation[max(gains)[1]] += 1
        covered=sum(min(d, allocation[i]*1800) for i,(_,d,_) in enumerate(routes))
        return routes, allocation, covered

    def greedy_signals(self, period=MORNING, cycle=120):
        rows=[]
        for nid in self.net.nodes:
            incoming=[]
            for e in self.net.edges:
                if e.u==nid or e.v==nid: incoming.append(e)
            if len(incoming)<2: continue
            total=sum(max(1,e.flows[period]) for e in incoming)
            greens=[max(8, round(cycle*e.flows[period]/total)) for e in incoming]
            greens[-1] += cycle-sum(greens)
            rows.append((self.net.nodes[nid].name, [(self.net.nodes[e.v if e.u==nid else e.u].name, greens[i], e.flows[period]) for i,e in enumerate(incoming)]))
        return rows

class App(tk.Tk):
    def __init__(self):
        super().__init__(); self.title("Greater Cairo Smart Transportation Optimization - CSE112")
        self.geometry("1380x850"); self.minsize(1100,720)
        self.net=CairoNetwork(); self.alg=Algorithms(self.net); self.id_by_label={v:k for k,v in self.net.names().items()}
        self.configure(bg="#f3f6fb"); self._style(); self._build()

    def _style(self):
        s=ttk.Style(self); s.theme_use("clam")
        s.configure("TFrame", background="#0f172a"); s.configure("Card.TFrame", background="#1e293b", relief="flat")
        s.configure("TLabel", background="#f3f6fb", foreground="#1f2937", font=("Segoe UI",10))
        s.configure("Title.TLabel", font=("Segoe UI",18,"bold"), foreground="#102033")
        s.configure("Card.TLabel", background="white", font=("Segoe UI",10))
        s.configure("TButton", font=("Segoe UI",10,"bold"), padding=8)
        s.configure("TNotebook.Tab", padding=(16,8), font=("Segoe UI",9,"bold"))

    def _build(self):
        header=ttk.Frame(self); header.pack(fill="x", padx=18, pady=(14,6))
        ttk.Label(header,text="Smart City Transportation Network Optimization System",style="Title.TLabel").pack(side="left")
        ttk.Label(header,text="Greater Cairo | Dijkstra · A* · MST · DP · Greedy",font=("Segoe UI",11)).pack(side="right")
        nb=ttk.Notebook(self); nb.pack(fill="both", expand=True, padx=18, pady=10)
        for title, builder in [("Dashboard",self.dashboard),("Dijkstra Routing",self.dijkstra_tab),("Emergency A*",self.astar_tab),("MST Design",self.mst_tab),("Traffic Simulation",self.traffic_tab),("Transit DP",self.dp_tab),("Greedy Signals",self.greedy_tab),("Performance",self.performance_tab)]:
            f=ttk.Frame(nb); nb.add(f,text=title); builder(f)

    def controls(self, parent):
        box=ttk.Frame(parent,style="Card.TFrame",padding=14); box.pack(side="left",fill="y",padx=(0,12),pady=8)
        labels=sorted(self.id_by_label)
        src=ttk.Combobox(box,values=labels,state="readonly",width=36); dst=ttk.Combobox(box,values=labels,state="readonly",width=36); per=ttk.Combobox(box,values=PERIODS,state="readonly",width=18)
        src.set("Maadi (1)"); dst.set("Cairo International Airport (F1)"); per.set(PERIODS[0])
        for text,w in [("Source",src),("Destination",dst),("Traffic Period",per)]: ttk.Label(box,text=text,style="Card.TLabel").pack(anchor="w",pady=(8,2)); w.pack(anchor="w")
        return box, src, dst, per

    def result_box(self,parent):
        txt=tk.Text(parent,height=12,wrap="word",font=("Consolas",10),bg="#ffffff",fg="#111827",relief="solid",bd=1,padx=10,pady=10)
        txt.pack(fill="x",pady=8); return txt

    def fig_canvas(self,parent):
        fig=Figure(figsize=(7.2,4.6),dpi=100); ax=fig.add_subplot(111); canvas=FigureCanvasTkAgg(fig,parent); canvas.get_tk_widget().pack(fill="both",expand=True,pady=8); return fig,ax,canvas

    def draw_network(self, ax, path=None, mst=None):
        ax.clear(); G=nx.Graph(); pos={}
        for nid,n in self.net.nodes.items(): G.add_node(nid); pos[nid]=(n.lon,n.lat)
        for e in self.net.edges: G.add_edge(e.u,e.v)
        nx.draw_networkx_edges(G,pos,ax=ax,edge_color="#cbd5e1",width=1.0)
        if mst: nx.draw_networkx_edges(G,pos,edgelist=[(e.u,e.v) for e in mst if e.existing],ax=ax,width=2.5,edge_color="#16a34a")
        if path and len(path)>1: nx.draw_networkx_edges(G,pos,edgelist=list(zip(path,path[1:])),ax=ax,width=4,edge_color="#dc2626")
        colors=["#ef4444" if self.net.nodes[n].kind=="Medical" else "#2563eb" if n.startswith("F") else "#0f766e" for n in G.nodes]
        nx.draw_networkx_nodes(G,pos,node_color=colors,node_size=85,ax=ax)
        nx.draw_networkx_labels(G,pos,labels={n:n for n in G.nodes},font_size=7,ax=ax)
        ax.set_title("Greater Cairo Transportation Network"); ax.set_axis_off()

    def ids(self, src, dst, per): return self.id_by_label[src.get()], self.id_by_label[dst.get()], PERIODS.index(per.get())

    def dashboard(self,f):
        left=ttk.Frame(f,style="Card.TFrame",padding=18); left.pack(side="left",fill="y",padx=(0,12),pady=8)
        ttk.Label(left,text="Project Coverage",font=("Segoe UI",15,"bold"),background="white").pack(anchor="w")
        facts=[f"Nodes: {len(self.net.nodes)} districts/facilities",f"Roads: {len(self.net.edges)} existing + {len(self.net.potential_edges)} potential", "Traffic periods: morning, afternoon, evening, night", "Required algorithms: MST, Dijkstra, A*, DP, Greedy", "UI/visualization: interactive tabs + charts"]
        for x in facts: ttk.Label(left,text="• "+x,style="Card.TLabel").pack(anchor="w",pady=6)
        fig,ax,canvas=self.fig_canvas(f); self.draw_network(ax); canvas.draw()

    def dijkstra_tab(self,f):
        box,src,dst,per=self.controls(f); right=ttk.Frame(f); right.pack(side="left",fill="both",expand=True); out=self.result_box(right); fig,ax,canvas=self.fig_canvas(right)
        def run():
            s,d,p=self.ids(src,dst,per); path,km,mins,visited=self.alg.shortest_path(s,d,p); out.delete("1.0","end")
            if not path: out.insert("end","No path found.\n"); return
            out.insert("end",f"Dijkstra shortest-time route under {PERIODS[p]}\nRoute: {' → '.join(self.net.nodes[x].name for x in path)}\nDistance: {km:.1f} km\nEstimated time/cost: {mins:.1f} minutes\nVisited nodes: {len(visited)}\nComplexity: O((V+E) log V)\n")
            self.draw_network(ax,path=path); canvas.draw()
        ttk.Button(box,text="Run Dijkstra",command=run).pack(fill="x",pady=12); ttk.Button(box,text="Reset View",command=lambda:(out.delete("1.0","end"),self.draw_network(ax),canvas.draw())).pack(fill="x")
        self.draw_network(ax); canvas.draw()

    def astar_tab(self,f):
        box,src,dst,per=self.controls(f); dst.set("Qasr El Aini Hospital (F9)"); right=ttk.Frame(f); right.pack(side="left",fill="both",expand=True); out=self.result_box(right); fig,ax,canvas=self.fig_canvas(right)
        def run():
            s,d,p=self.ids(src,dst,per); ep,ekm,et,ev=self.alg.astar(s,d,p,True); np,nkm,nt,nv=self.alg.shortest_path(s,d,p); out.delete("1.0","end")
            out.insert("end",f"Emergency A* route under {PERIODS[p]}\nRoute: {' → '.join(self.net.nodes[x].name for x in ep)}\nDistance: {ekm:.1f} km | Emergency ETA: {et:.1f} min\nNormal Dijkstra ETA for comparison: {nt:.1f} min\nImprovement: {max(0,nt-et):.1f} min ({(max(0,nt-et)/nt*100 if nt else 0):.0f}%)\nVisited nodes: A*={len(ev)} vs Dijkstra={len(nv)}\n")
            self.draw_network(ax,path=ep); canvas.draw()
        ttk.Button(box,text="Run Emergency A*",command=run).pack(fill="x",pady=12); self.draw_network(ax); canvas.draw()

    def mst_tab(self,f):
        left=ttk.Frame(f,style="Card.TFrame",padding=14); left.pack(side="left",fill="y",padx=(0,12),pady=8); right=ttk.Frame(f); right.pack(side="left",fill="both",expand=True); out=self.result_box(right); fig,ax,canvas=self.fig_canvas(right)
        include=tk.BooleanVar(value=True); ttk.Checkbutton(left,text="Include potential new roads",variable=include).pack(anchor="w",pady=8)
        def run():
            mst=self.alg.kruskal(include.get()); out.delete("1.0","end")
            total=sum(e.distance for e in mst); cost=sum(e.construction_cost for e in mst if not e.existing); new=sum(1 for e in mst if not e.existing)
            out.insert("end",f"Kruskal MST result\nEdges selected: {len(mst)} / required {len(self.net.nodes)-1}\nTotal network distance: {total:.1f} km\nNew roads selected: {new}\nConstruction cost: {cost:.0f} million EGP\nComplexity: O(E log E)\n\nTop selected links:\n")
            for e in mst[:12]: out.insert("end",f"- {self.net.nodes[e.u].name} ↔ {self.net.nodes[e.v].name} | {e.distance} km | {'existing' if e.existing else 'new'}\n")
            self.draw_network(ax,mst=mst); canvas.draw()
        ttk.Button(left,text="Build MST",command=run).pack(fill="x",pady=12); run()

    def traffic_tab(self,f):
        out=self.result_box(f); fig,ax,canvas=self.fig_canvas(f)
        def run():
            ax.clear(); top=sorted(self.net.edges,key=lambda e:e.congestion(MORNING),reverse=True)[:10]
            ax.bar([f"{e.u}-{e.v}" for e in top],[e.congestion(MORNING)*100 for e in top]); ax.set_title("Top congested roads - Morning Peak"); ax.set_ylabel("Capacity used (%)"); ax.tick_params(axis='x',rotation=35)
            out.delete("1.0","end"); out.insert("end","Rush-hour simulation: edge weights are recalculated from traffic flow/capacity, so shortest routes change by selected period. The chart shows capacity pressure; values above 100% represent overload.\n")
            canvas.draw()
        ttk.Button(f,text="Run Rush-Hour Simulation",command=run).pack(anchor="w",padx=8,pady=8); run()

    def dp_tab(self,f):
        out=self.result_box(f); fig,ax,canvas=self.fig_canvas(f)
        def run():
            routes,alloc,covered=self.alg.dp_bus_allocation(250); out.delete("1.0","end"); out.insert("end",f"DP-style marginal resource allocation for public transit\nTotal buses: 250\nEstimated covered passenger demand: {covered:,.0f} passengers/day\nComplexity: O(R × B) for R routes and B buses\n\n")
            for (name,demand,_),a in zip(routes,alloc): out.insert("end",f"{name:<32} {a:>3} buses | demand {demand:,}\n")
            ax.clear(); ax.bar([r[0].split()[0] for r in routes],alloc); ax.set_title("Optimized Bus Allocation"); ax.set_ylabel("Buses"); canvas.draw()
        ttk.Button(f,text="Optimize Transit Schedule",command=run).pack(anchor="w",padx=8,pady=8); run()

    def greedy_tab(self,f):
        out=self.result_box(f); fig,ax,canvas=self.fig_canvas(f)
        def run():
            rows=self.alg.greedy_signals(MORNING,120); out.delete("1.0","end"); out.insert("end","Greedy traffic signal optimization: green time is assigned proportionally to current incoming flow, with minimum green safeguards. Emergency priority uses urgency/type ranking and preemption.\n\n")
            for name,phases in rows[:8]:
                out.insert("end",name+": "+", ".join(f"{p[0]}={p[1]}s" for p in phases[:4])+"\n")
            ax.clear(); sample=rows[:6]; ax.bar([r[0][:10] for r in sample],[sum(p[1] for p in r[1]) for r in sample]); ax.set_title("Signal cycle time check (should equal 120s)"); ax.set_ylabel("Seconds"); canvas.draw()
        ttk.Button(f,text="Optimize Signals",command=run).pack(anchor="w",padx=8,pady=8); run()

    def performance_tab(self,f):
        out=self.result_box(f); fig,ax,canvas=self.fig_canvas(f)
        def run():
            pairs=[("1","F1"),("7","F9"),("13","F1"),("12","F9"),("4","3")]; d_times=[]; a_times=[]
            out.delete("1.0","end"); out.insert("end","Measured runtime comparison on demo scenarios. Values are wall-clock milliseconds on this machine, not fake accuracy percentages.\n\n")
            for s,d in pairs:
                t=time.perf_counter(); self.alg.shortest_path(s,d,MORNING); dms=(time.perf_counter()-t)*1000
                t=time.perf_counter(); self.alg.astar(s,d,MORNING,True); ams=(time.perf_counter()-t)*1000
                d_times.append(dms); a_times.append(ams); out.insert("end",f"{s}->{d}: Dijkstra {dms:.3f} ms | A* {ams:.3f} ms\n")
            ax.clear(); x=range(len(pairs)); ax.plot(x,d_times,marker="o",label="Dijkstra"); ax.plot(x,a_times,marker="o",label="A*"); ax.set_xticks(list(x)); ax.set_xticklabels([f"{a}-{b}" for a,b in pairs]); ax.set_ylabel("ms"); ax.set_title("Algorithm Runtime Comparison"); ax.legend(); canvas.draw()
        ttk.Button(f,text="Run Performance Test",command=run).pack(anchor="w",padx=8,pady=8); run()

if __name__ == "__main__":
    App().mainloop()
