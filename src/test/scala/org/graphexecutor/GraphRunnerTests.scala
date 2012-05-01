package org.graphexecutor

import bench.{BenchControl, BenchMarker}
import org.scalatest.FunSuite
import scala.collection.mutable._
import org.scalatest.matchers.ShouldMatchers


class GraphRunnerTests extends FunSuite with ShouldMatchers {
  test("simple graph execution") {
    val graph = new GraphRunner("testgraph1")

    assert(graph.isInstanceOf[NodeRunner] === true, "Graph is of class NodeRunner")
    assert(graph.isInstanceOf[GraphRunner] === true, "Graph is of class GraphRunner")

    val t1 = graph.solveSync()
    t1 should be(true)
  }

  test("graph with internal nodes") {
    BenchControl clear
    val node1 = NodeRunner("node1", SSsystem(1000, 0.01), true)
    val node2 = NodeRunner("node2", SSsystem(1000, 0.01), true)
    val node3 = NodeRunner("node3", SSsystem(1000, 0.01), true)
    val graph1 = GraphRunner("graph", new NoopWork(), true)

    node1 -> node2 -> node3
    graph1 ~> node1 ~> node2 ~> node3
    graph1.setStartNodes(Set(node1))
    graph1.setEndNodes(Set(node3))

    //correct configuration
    //test correct configuration
    node1.hasDependent(node2) should be(true)
    node2.hasDependent(node3) should be(true)
    node2.hasSource(node1) should be(true)
    node3.hasSource(node2) should be(true)

    graph1.snode.hasDependent(node1) should be(true)
    graph1.enode.hasSource(node3) should be(true)
    graph1.enode.hasSource(graph1) should be(true)
    graph1.snode.hasDependent(graph1) should be(true)

    graph1.solveSync() should be(true)

    BenchControl reportData

    ~graph1

  }

  test("graph configuration with symbol operators") {
    BenchControl clear
    val node1 = NodeRunner("node1a", SSsystem(1000, 0.01), true)
    val node2 = NodeRunner("node2a", SSsystem(1000, 0.01), true)
    val node3 = NodeRunner("node3a", SSsystem(1000, 0.01), true)
    val graph1 = GraphRunner("graph1a", new NoopWork(), true)

    node1 -> node2 -> node3 //execution dependencies
    graph1 ~~> node1 ~> node2 ~> node3 //graph membership
    graph1 <~~ node3 //end node

    //test correct configuration
    node1.hasDependent(node2) should be(true)
    node2.hasDependent(node3) should be(true)
    node2.hasSource(node1) should be(true)
    node3.hasSource(node2) should be(true)

    graph1.snode.hasDependent(node1) should be(true)
    graph1.enode.hasSource(node3) should be(true)
    graph1.enode.hasSource(graph1) should be(true)
    graph1.snode.hasDependent(graph1) should be(true)

    graph1.solveSync() should be(true)

    BenchControl reportData

    ~graph1

  }


}

//

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