package org.graphexecutor

import bench.BenchMarker
import org.scalatest.FunSuite
import scala.collection.mutable._


class GraphRunnerTests extends FunSuite {
  test("simple graph execution") {
    val node = new GraphRunner("testgraph1")
    assert(node.isInstanceOf[NodeRunner] === true)
    assert(node.isInstanceOf[GraphRunner] === true)
    node.start

    val t1 = node !? "solve"
    assert( t1 === "testgraph1 solved" )

  }

  test("graph with internal nodes") {
    val node1 = NodeRunner("node1", new Model(), true )
    val node2 = NodeRunner("node2", new Model(), true )
    val node3 = NodeRunner("node3", new Model(), true )
    val graph1 = GraphRunner("graph1", new Model(), true )


    node1 -> node2 -> node3
    graph1 ~> node1 ~> node2 ~> node3
    graph1.setStartNodes( Set(node1) )
    graph1.setEndNodes( Set(node3) )

    //correct configuration
    assert( graph1.snode.dependents.contains(node1) === true )
    assert( node1.dependents.contains(node2) === true )
    assert( node2.dependents.contains(node3) === true )
    assert( node3.dependents.contains(graph1.enode) === true )
    assert( graph1.enode.observers.contains(graph1) === true )

    assert( node1.sources.contains(graph1.snode) === true )
    assert( node2.sources.contains(node1) === true )
    assert( node3.sources.contains(node2) === true )
    assert( graph1.enode.sources.contains(node3) === true )



    graph1 ! "solve"

    graph1.barrier.await()

    BenchMarker.reportRawdata()

    //test for solve fire
    assert( node1.sources.values.foldLeft(true)(_ && _) === true )
    assert( node2.sources.values.foldLeft(true)(_ && _) === true )
    assert( node3.sources.values.foldLeft(true)(_ && _) === true )
    assert( graph1.enode.sources.values.foldLeft(true)(_ && _) === true )

    graph1 ! "stop"

  }
}