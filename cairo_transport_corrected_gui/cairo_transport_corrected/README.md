# Greater Cairo Smart Transportation Optimization System - Corrected GUI

## Run
```bash
pip install -r requirements.txt
python main.py
```

Tkinter is included with most Python installations. On Linux, install it if needed:
```bash
sudo apt-get install python3-tk
```

## Included algorithms
- Dijkstra shortest-time routing with time-varying traffic weights.
- A* emergency routing with heuristic search, congestion avoidance, and preemption factor.
- Kruskal MST road network design with population/condition/new-road weighting.
- Public transit resource allocation using dynamic-programming style marginal optimization.
- Greedy traffic signal timing and emergency priority explanation.
- Rush-hour simulation and runtime charts.
