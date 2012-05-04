package org.graphexecutor

import bench.{BenchControl, BenchMarker}
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers
import akka.dispatch.Await
import akka.pattern.ask
import akka.util.duration._
import akka.util.Timeout
import org.graphexecutor.signals.{IsDependent, IsSource, GetSolveCount}
import scala.Int


class GraphRunnerTests extends FunSuite with ShouldMatchers {

  implicit val timeout = Timeout(5 seconds)

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

  test("graph with internal graph") {

    BenchControl.clear should be (true)

    val onode1 = NodeRunner("onode1", SSsystem(1000, 0.01), true)
    val onode2 = NodeRunner("onode2", SSsystem(1000, 0.01), true)
    val onode3 = NodeRunner("onode3", SSsystem(1000, 0.01), true)

    val inode1 = NodeRunner("inode1", SSsystem(1000, 0.01), true)
    val inode2 = NodeRunner("inode2", SSsystem(1000, 0.01), true)
    val inode3 = NodeRunner("inode3", SSsystem(1000, 0.01), true)
    val inode4 = NodeRunner("inode4", SSsystem(1000, 0.01), true)
    val igraph = GraphRunner("igraph", new NoopWork(), true)

    println( BenchControl.listBenchers )
    BenchControl.size should be (8)
    BenchControl.numreports should be (0)

    inode1 -> inode2 -> inode4
    inode1 -> inode3 -> inode4
    igraph ~> inode1 ~> inode2 ~> inode3 ~> inode4
    igraph.setStartNodes(Set(inode1))
    igraph.setEndNodes(Set(inode4))

    onode1 -> igraph -> onode3
    onode1 -> onode2 -> onode3

    //test config of outer graph
    onode1.hasDependent(onode2) should be(true)
    onode2.hasSource(onode1) should be(true)

    onode2.hasDependent(onode3) should be(true)
    onode3.hasSource(onode2) should be(true)

    onode1.hasDependent(igraph.snode) should be(true)
    igraph.hasSource(onode1) should be(true)

    igraph.hasDependent(onode3) should be(true)
    println( ">>> " + onode3.sources )
    onode3.hasSource(igraph.enode) should be(true)
    onode3.sources.size should equal (2)

    //test config of graph internals - virtual start and end nodes
    igraph.hasSource(igraph.snode)
    igraph.snode.hasDependent(igraph)

    igraph.hasDependent(igraph.enode)
    igraph.enode.hasSource(igraph)

    igraph.sources.size should equal (1)
    igraph.dependents.size should equal (1)

    //test internal graph config
    igraph.snode.hasDependent(inode1) should be (true)
    inode1.hasSource(igraph.snode) should be (true)

    inode1.hasDependents( inode2::inode3::Nil ) should be (true)

    //solve
    onode1.solveAsync()
    withClue("Wait for node3 solve time out: ") {
    onode3.blockUntilSolved(1,10000,1000) should be (true) }


    // solve status
    val f2 = igraph.actor ? GetSolveCount()
    val solved  = Await.result(f2, 2 seconds).asInstanceOf[Int]
    solved should be (1)

    BenchControl.reportData
    BenchControl.numreports should be (8)

    ~igraph
    ~onode1
    ~onode2
    ~onode3
  }
}