GraphExecutor
=============

An Actor based framework for creating execution graphs with arbitrary workloads for each node.
The graph nodes define workloads and the links (edges) create a execution dependency graph. Nodes are executed deterministically as specified by the graph with as much parallelism as allowed by the graph.

Subgraphs can be created and encapsulated in nodes.
A Graphviz based DSL allows simple programatic construction of graphs.

Usage
------
 
``` scala
  class TestActor extends Actor {
     var loadreport: List[Double] = List.empty
     var reportcount = 0
    
     def act = {
       loop {
         react {
           case load_report: CPUloadReport => {
             loadreport = load_report.cpuloads
             reportcount = reportcount + 1
           }
         }
       }
     }
  }  
 
```
 



























