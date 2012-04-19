//package org.graphexecutor
//
//import bench.BenchMarker
//import org.scalatest.FunSuite
//import scala.collection.mutable._
//
//
//class GraphRunnerTests extends FunSuite {
//  test("simple graph execution") {
//    val node = new GraphRunner("testgraph1")
//    assert(node.isInstanceOf[NodeRunner] === true)
//    assert(node.isInstanceOf[GraphRunner] === true)
//    node.start
//
//    val t1 = node !? "solve"
//    assert( t1 === "testgraph1 solved" )
//
//  }
//
//  test("graph with internal nodes") {
//    val node1 = NodeRunner("node1", SSsystem(1000, 0.01), true )
//    val node2 = NodeRunner("node2", SSsystem(1000, 0.01), true )
//    val node3 = NodeRunner("node3", SSsystem(1000, 0.01), true )
//    val graph1 = GraphRunner("graph1", new NoopWork(), true )
//    BenchMarker clear
//
//    node1 -> node2 -> node3
//    graph1 ~> node1 ~> node2 ~> node3
//    graph1.setStartNodes( Set(node1) )
//    graph1.setEndNodes( Set(node3) )
//
//    //correct configuration
//    assert( graph1.snode.dependents.contains(node1) === true )
//    assert( node1.dependents.contains(node2) === true )
//    assert( node2.dependents.contains(node3) === true )
//    assert( node3.dependents.contains(graph1.enode) === true )
//    assert( graph1.enode.observers.contains(graph1) === true )
//
//    assert( node1.sources.contains(graph1.snode) === true )
//    assert( node2.sources.contains(node1) === true )
//    assert( node3.sources.contains(node2) === true )
//    assert( graph1.enode.sources.contains(node3) === true )
//
//
//
//    graph1 ! "solve"
//
//    graph1.barrier.await()
//
//    BenchMarker reportData
//
//    //test for solve fire
//    assert( node1.sources.values.foldLeft(true)(_ && _) === true )
//    assert( node2.sources.values.foldLeft(true)(_ && _) === true )
//    assert( node3.sources.values.foldLeft(true)(_ && _) === true )
//    assert( graph1.enode.sources.values.foldLeft(true)(_ && _) === true )
//
//    graph1 ! "stop"
//
//  }
//
//
//
//  test("graph with internal graph") {
//    val node1 = NodeRunner("node1", SSsystem(1000, 0.01), true )
//    val node2 = NodeRunner("node2", SSsystem(1000, 0.01), true )
//    val node3 = NodeRunner("node3", SSsystem(1000, 0.01), true )
//    val node4 = NodeRunner("node4", SSsystem(1000, 0.01), true )
//    val graph1 = GraphRunner("graph1", new NoopWork(), true )
//    BenchMarker clear
//
//    node1 -> node2 -> node4
//    node1 -> node3 -> node4
//    graph1 ~> node1 ~> node2 ~> node3 ~> node4
//    graph1.setStartNodes( Set(node1) )
//    graph1.setEndNodes( Set(node4) )
//
//    //correct configuration
//    assert( graph1.snode.dependents.contains(node1) === true )
//    assert( node1.dependents.contains(node2) === true )
//    assert( node2.dependents.contains(node4) === true )
//    assert( node3.dependents.contains(node4) === true )
//    assert( node4.dependents.contains(graph1.enode) === true )
//    assert( graph1.enode.observers.contains(graph1) === true )
//
//    assert( node1.sources.contains(graph1.snode) === true )
//    assert( node2.sources.contains(node1) === true )
//    assert( node3.sources.contains(node1) === true )
//    assert( node4.sources.contains(node2) === true )
//    assert( node4.sources.contains(node3) === true )
//    assert( graph1.enode.sources.contains(node4) === true )
//
//
//
//    graph1 ! "solve"
//
//    graph1.barrier.await()
//
//    BenchMarker reportData
//
//    //test for solve fire
//    assert( node1.sources.values.foldLeft(true)(_ && _) === true )
//    assert( node2.sources.values.foldLeft(true)(_ && _) === true )
//    assert( node3.sources.values.foldLeft(true)(_ && _) === true )
//    assert( graph1.enode.sources.values.foldLeft(true)(_ && _) === true )
//
//    graph1 ! "stop"
//
//  }
//
//
//}