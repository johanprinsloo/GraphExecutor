GraphExecutor
=============

An Actor based framework for creating execution graphs with arbitrary workloads for each node.
The graph nodes define workloads and the links (edges) create a execution dependency graph. Nodes are executed deterministically as specified by the graph with as much parallelism as allowed by the graph.

Subgraphs can be created and encapsulated in nodes.
A Graphviz/dot based DSL allows simple programatic construction of graphs.

Usage
------
 
``` scala
	//create 4 nodes each with a numeric payload
	val benchmark = true
    val node1 = NodeRunner("node1", SSsystem(1000, 0.01), benchmark)
    val node2 = NodeRunner("node2", SSsystem(1000, 0.01), benchmark)
    val node3 = NodeRunner("node3", SSsystem(1000, 0.01), benchmark)
    val node4 = NodeRunner("node4", SSsystem(1000, 0.01), benchmark)
    
    //create a graph 
    val graph1 = GraphRunner("graph1", new Model(), benchmark)

    //define execution dependencies
    node1 -> node2 -> node4
    node1 -> node3 -> node4
    
    //subgraph under graph1
    graph1 ~> node1 ~> node2 ~> node3 ~> node4
    graph1.setStartNodes( Set(node1) )
    graph1.setEndNodes( Set(node4) )

    graph1 ! "solve"

    graph1.barrier.await()

    BenchMarker reportData

    graph1 ! "stop"
 
```
produces the following sequence of execution:

```scala

Node: graph1 start the solve             timestamp: 1304827821311.949000   on thread 27
Node: node1 start the solve              timestamp: 1304827821312.273000   on thread 35
Node: node1 complete solve               timestamp: 1304827821317.240000   on thread 35
Node: node3 start the solve              timestamp: 1304827821317.593000   on thread 33
Node: node2 start the solve              timestamp: 1304827821317.619000   on thread 32
Node: node2 complete solve               timestamp: 1304827821322.860000   on thread 32
Node: node3 complete solve               timestamp: 1304827821322.895000   on thread 33
Node: node4 start the solve              timestamp: 1304827821323.272000   on thread 32
Node: node4 complete solve               timestamp: 1304827821327.994100   on thread 32
Node: graph1 complete solve              timestamp: 1304827821328.472200   on thread 27
``` 



























