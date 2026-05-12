# 🚇 Cairo Smart Transportation Optimization System
**CSE112 – Design and Analysis of Algorithms | Alamein International University**

[![Java](https://img.shields.io/badge/Java-21-orange?logo=java)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.9-blue?logo=apachemaven)](https://maven.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?logo=docker)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green)](LICENSE)

A comprehensive algorithmic system for optimizing Greater Cairo's transportation network, implementing **MST**, **Dijkstra**, **A\***, **Dynamic Programming**, **Greedy Algorithms**, and an **ML Traffic Predictor** — all applied to real Cairo geographic and traffic data.

---

## 📋 Table of Contents
- [Features](#-features)
- [Architecture](#-architecture)
- [Quick Start](#-quick-start)
- [Running with Docker](#-running-with-docker)
- [Running Locally (Maven)](#-running-locally-maven)
- [Web Dashboard](#-web-dashboard)
- [Algorithm Implementations](#-algorithm-implementations)
- [Data](#-data)
- [Testing](#-testing)
- [Project Structure](#-project-structure)

---

## ✨ Features

| Component | Algorithm | Purpose |
|-----------|-----------|---------|
| Infrastructure Design | **Kruskal's MST** | Minimum-cost road network connecting all 25 nodes |
| Route Planning | **Dijkstra** | Time-dependent shortest paths (4 traffic periods) |
| Emergency Routing | **A\* Search** | Ambulance/fire routing with signal preemption |
| Transit Scheduling | **DP – Knapsack** | Optimal bus/metro fleet allocation |
| Maintenance | **DP – 0/1 Knapsack** | Road maintenance within budget |
| Traffic Signals | **Greedy** | Proportional green-time allocation |
| Emergency Priority | **Greedy PQ** | Priority-based vehicle preemption |
| Traffic Prediction | **ML – Linear Regression** | OLS model trained per road (BONUS) |
| Algorithm Comparison | **Dijkstra vs A\*** | Side-by-side race visualization (BONUS) |

---

## 🏗 Architecture

```
cairo-transport/
├── src/main/java/cairo/transport/
│   ├── model/              # Domain objects
│   │   ├── Node.java       # Graph node (district / facility)
│   │   ├── Edge.java       # Road with time-dependent traffic
│   │   ├── TransportGraph.java
│   │   ├── BusRoute.java
│   │   └── MetroLine.java
│   │
│   ├── algorithms/         # Core algorithm implementations
│   │   ├── KruskalMST.java
│   │   ├── DijkstraShortestPath.java
│   │   ├── AStarSearch.java
│   │   ├── DynamicProgramming.java
│   │   └── GreedyAlgorithms.java
│   │
│   ├── ml/                 # Bonus ML component
│   │   └── TrafficPredictor.java
│   │
│   ├── utils/
│   │   ├── DataLoader.java # All Cairo data hardcoded
│   │   └── UnionFind.java  # Union-Find with path compression
│   │
│   └── ui/
│       └── TransportationSystem.java  # Interactive CLI demo
│
├── src/test/java/cairo/transport/
│   └── TransportSystemTest.java
│
├── web/
│   └── index.html          # Web dashboard (Bonus visualization)
│
├── Dockerfile
├── docker-compose.yml
└── pom.xml
```

---

## 🚀 Quick Start

### Prerequisites
- **Java 21+**
- **Maven 3.8+** (for local build)  
- **Docker + Docker Compose** (for containerized run)

---

## 🐳 Running with Docker

This is the easiest method — runs identically on any machine.

```bash
# 1. Clone the repository
git clone https://github.com/<your-username>/cairo-transport-system.git
cd cairo-transport-system

# 2. Build and run the full demo (non-interactive)
docker compose up cairo-transport

# 3. OR run in interactive menu mode
docker compose run cairo-transport interactive

# 4. Run the web dashboard on localhost:8080
docker compose up cairo-web
# Then open: http://localhost:8080
```

**Build just the JAR image:**
```bash
docker build -t cairo-transport .
docker run -it cairo-transport              # auto demo
docker run -it cairo-transport interactive  # interactive menu
```

---

## 🔧 Running Locally (Maven)

```bash
# Compile + run tests
mvn compile

# Run tests
mvn test

# Build fat JAR
mvn package -DskipTests

# Run interactive demo
java -jar target/cairo-transport-system.jar

# Run full auto demo (no user input needed)
java -jar target/cairo-transport-system.jar --auto
```

---

## 🌐 Web Dashboard

The web dashboard is a **standalone HTML file** with zero dependencies — just open it in a browser:

```bash
# Option 1: Open directly
open web/index.html   # macOS
xdg-open web/index.html  # Linux

# Option 2: Via Docker (served by nginx on port 8080)
docker compose up cairo-web
# Visit http://localhost:8080

# Option 3: Any static file server
cd web && python3 -m http.server 8080
```

The dashboard includes:
- 🗺 Interactive Cairo network map
- 📊 Algorithm complexity charts
- 🚑 A\* emergency route visualization
- ⚡ Dijkstra vs A\* race animation
- 🤖 ML traffic prediction curves
- 📅 DP scheduling tables

---

## 📐 Algorithm Implementations

### A. Kruskal's MST — `O(E log E)`
- Population-weighted edges: high-population connections prioritized
- Critical facility guarantee: hospitals, airport, transit hubs always connected
- Road condition penalty: degraded roads have higher effective weight
- Runs on existing roads only **or** including 15 potential new roads

### B. Dijkstra's Shortest Path — `O((V+E) log V)`
- Time-dependent weights: 4 traffic periods (morning peak / afternoon / evening peak / night)
- Road condition factor: poor roads increase effective weight
- Alternate routing: excludes congested edges above threshold
- Memoization: caches `(source, period)` results for O(1) repeat queries

### C. A\* Emergency Routing — `O(E)` typical
- Admissible heuristic: Haversine geographic distance (never overestimates)
- Emergency signal preemption: 15% weight reduction on all roads
- Medical facility bias: extra 30% bonus toward critical facilities
- Avoids severely congested roads (ratio > 0.95) when alternatives exist
- Estimates ETA for emergency vs normal vehicles

### D. Dynamic Programming
**Vehicle Scheduling (Unbounded Knapsack) — `O(R × V)`**
- Allocates bus/metro fleet to maximize daily passenger coverage
- Diminishing returns model: `gain = base × (√(current+extra) − √(current)) / √current`
- Backtracking for allocation recovery

**Road Maintenance (0/1 Knapsack) — `O(E × B)`**
- Selects roads for maintenance within budget to maximize improvement
- Space-optimized: 1D DP array (O(B) space)
- Improvement metric: capacity × condition_improvement × distance / 100

**Memoization — `O(1)` cached queries**
- Wraps Dijkstra with HashMap cache keyed by `(source, target, period)`

### E. Greedy Algorithms
**Traffic Signal Optimization — `O(I × D)`**
- Proportional green time: flow/totalFlow × cycleTime per direction
- Minimum 5s green enforced
- Cycle sums exactly to 120s (adjusts last direction)
- Identifies suboptimal intersections needing green-wave coordination

**Emergency Preemption — `O(E log E)`**
- Priority queue sorted by urgency × vehicle-type bonus
- Ambulance: 2.0×, Fire: 1.8×, Police: 1.0×
- Conflict detection for overlapping corridors

### F. ML Traffic Prediction (Bonus) — `O(R × 9)`
- One linear regression model per road
- Features: `[1, hour_of_day, is_peak_hour]`
- Trained with OLS closed-form: `w = (XᵀX)⁻¹ Xᵀy`
- Solved via Gaussian elimination (no external libraries)
- Forecasts top-N congested roads at any hour

---

## 📊 Data

The system uses the official CSE112 project data for Greater Cairo:

| Dataset | Count |
|---------|-------|
| Districts & Neighborhoods | 15 |
| Important Facilities | 10 |
| Existing Roads | 36 (28 original + 8 facility connections) |
| Potential New Roads | 15 |
| Traffic Flow Periods | 4 (Morning Peak / Afternoon / Evening Peak / Night) |
| Bus Routes | 10 |
| Metro Lines | 3 |

---

## 🧪 Testing

```bash
# Run all tests
mvn test
```

The test suite (`TransportSystemTest.java`) covers:
- ✅ Graph data loading (25 nodes, 36 edges, 15 potential roads)
- ✅ Kruskal MST (no cycles, V-1 edges, critical facilities connected)
- ✅ Dijkstra (self-distance = 0, triangle inequality, path reconstruction)
- ✅ A\* (correct start/end, emergency < normal time, trivial same-node path)
- ✅ DP scheduling (passengers > 0, cost ≤ budget, condition filter)
- ✅ Greedy signals (phases non-empty, cycle sums to 120s)
- ✅ ML predictor (peak ≥ night, MAE < 1000 veh/h, top-5 forecast)

---

## 📁 Project Structure

```
.
├── Dockerfile                  # Multi-stage Docker build
├── docker-compose.yml          # Services: CLI demo + web dashboard
├── pom.xml                     # Maven build (Java 21, fat JAR)
├── README.md
├── web/
│   └── index.html              # Standalone web visualization
└── src/
    ├── main/java/cairo/transport/
    │   ├── algorithms/         # 5 algorithm files
    │   ├── ml/                 # ML predictor
    │   ├── model/              # 5 domain model files
    │   ├── ui/                 # Interactive demo
    │   └── utils/              # DataLoader + UnionFind
    └── test/java/cairo/transport/
        └── TransportSystemTest.java
```

---

## 🎓 Academic Info

- **Course**: CSE112 – Design and Analysis of Algorithms
- **Institution**: Alamein International University (AIU)
- **Components**: Practical + Theoretical + Bonus
- **Bonus items completed**:
  - ✅ ML-based traffic prediction (Linear Regression, OLS)
  - ✅ Side-by-side algorithm comparison (Dijkstra vs A\* race)
  - ✅ Enhanced Web Visualization & UI
  - ✅ Docker containerization
  - ✅ This README + organized repository

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.
