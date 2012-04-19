package org.graphexecutor

import bench.{BenchController, BenchMarker}
import org.scalatest.FunSuite
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import org.graphexecutor.signals.GetSolveCount
import org.scalatest.matchers.ShouldMatchers
import akka.actor.{PoisonPill, ActorRef}
import akka.dispatch.{Future, Await}
import akka.pattern.gracefulStop
import org.scalatest.Assertions._


class NodeRunnerBaseTest  extends FunSuite with ShouldMatchers {
  implicit val timeout = Timeout(5 seconds)

  test("node creation with class") {
    val node = new NodeRunner("testnode1")
    assert(node.isInstanceOf[NodeRunner] === true, "NodeRunner Type check")
    assert(node.actor.isInstanceOf[ActorRef] === true, "NodeRunner Actor type check")
    assert( node.isSolvable === true )  //no connection in graph

    val fu1 = node.actor ? GetSolveCount()
    val c1 = Await.result( fu1 , 5 seconds).asInstanceOf[Int]
    c1 should be (0)

    node.actor ! "solve"
    val fu2 = node.actor ? GetSolveCount()
    val c2 = Await.result( fu2 , 5 seconds).asInstanceOf[Int]
    c2 should be (1)

    val stopped: Future[Boolean] = gracefulStop(node.actor, 5 seconds)(NodeControl.system)
    val stop = Await.result(stopped, 6 seconds)
    stop should be (true)
  }

  test("create nodes through companion") {
    val n = NodeRunner("n1")
    assert( n.isInstanceOf[NodeRunner] === true, "NodeRunner Type check")
    assert( n.actor.isInstanceOf[ActorRef] === true, "NodeRunner Actor type check")
    assert( n.isSolvable === true )
    n.actor ! PoisonPill
  }

  test("simple dependency config test") {
    val node1 = new NodeRunner("testnode1")
    val node2 = new NodeRunner("testnode2")

    node1 -> node2

    val f1 = node1.actor ? "solve"
    val act = Await.result(f1,2 seconds).asInstanceOf[String]
    act should equal ("testnode1 solved")

    val f2 = node2.actor ? GetSolveCount()
    val solved  = Await.result(f2,2 seconds).asInstanceOf[Int]
    solved should be (1)

    node1.actor ! PoisonPill
    node2.actor ! PoisonPill
  }


  test("mutiple node creation with BenchMarker mixed in and significant model load") {
    val n1 = NodeRunner("n1", SSsystem(1000, 0.01), true )
    val n2 = NodeRunner("n2", SSsystem(1000, 0.01), true )
    val n3 = NodeRunner("n3", SSsystem(1000, 0.01), true )
    val n4 = NodeRunner("n4", SSsystem(1000, 0.01), true )
    val n5 = NodeRunner("n5", SSsystem(1000, 0.01), true )
    val n6 = NodeRunner("n6", SSsystem(1000, 0.01), true )
    val n7 = NodeRunner("n7", SSsystem(1000, 0.01), true )
    val n8 = NodeRunner("n8", SSsystem(1000, 0.01), true )
    val n0 = NodeRunner("n0")

    n1 -> n0
    n2 -> n0
    n3 -> n0
    n4 -> n0
    n5 -> n0
    n6 -> n0
    n7 -> n0
    n8 -> n0

    assert(n1.isInstanceOf[NodeRunner] === true)
    val bnc = BenchController.listBenchers
    //info(">>Benchmarking nodes: \n" + bnc)

    BenchController clear

    n1.actor ! "solve"
    n2.actor ! "solve"
    n3.actor ! "solve"
    n4.actor ! "solve"
    n5.actor ! "solve"
    n6.actor ! "solve"
    n7.actor ! "solve"
    n8.actor ! "solve"

    val f = n8.actor ? "solve"
    val solved  = n0.blockUntilSolved()
    solved should be (true)

    n1.actor ! PoisonPill
    n2.actor ! PoisonPill
    n3.actor ! PoisonPill
    n4.actor ! PoisonPill
    n5.actor ! PoisonPill
    n6.actor ! PoisonPill
    n7.actor ! PoisonPill
    n0.actor ! PoisonPill

    BenchController reportData
  }
//
//
//  test("node configuration test - test for correct node connections") {
//    val n1 = NodeRunner("n1", new NoopWork())
//    val n2 = NodeRunner("n1", new NoopWork())
//    val n3 = NodeRunner("n1", new NoopWork())
//
//    n1 -> n2 -> n3
//
//    //correct dependents
//    assert( n1.dependents.contains(n2) === true )
//    assert( n2.dependents.contains(n3) === true )
//
//    //correct sources
//    assert( n2.sources.contains(n1) === true )
//    assert( n3.sources.contains(n2) === true )
//
//    //correct source init
//    assert( n2.sources.get(n1).get === false )
//    assert( n3.sources.get(n2).get === false )
//  }
//
//  test("multi-node creation, execution with BenchMarking and barrier wait ") {
//    val n1 = new NodeRunner("n1") with BenchMarker
//    val n2 = new NodeRunner("n2") with BenchMarker
//    val n3 = new NodeRunner("n3") with BenchMarker
//
//    n1 -> n2 -> n3
//    n1.start
//    n2.start
//    n3.start
//
//    BenchMarker clear
//
//    n1 ! "solve"
//    n3.barrier.await
//
//    //test for solve fire
//    assert( n2.sources.values.foldLeft(true)(_ && _) === true )
//    assert( n3.sources.values.foldLeft(true)(_ && _) === true )
//
//    //benchmarker output
//    BenchMarker reportData
//  }
//
//    test("multi-node creation, execution with observation from other actors") {
//    val n1 = new NodeRunner("n1") with BenchMarker
//    val n2 = new NodeRunner("n2") with BenchMarker
//    val n3 = new NodeRunner("n3") with BenchMarker
//    val observer = new NodeRunner("observer") with NodeObserver
//    n1 -> n2 -> n3
//    observer ~>> n1 ~>> n2 ~>> n3
//
//    BenchMarker clear
//
//    assert( n1.observers.contains(observer) === true )
//    assert( n2.observers.contains(observer) === true )
//    assert( n3.observers.contains(observer) === true )
//
//    observer.start
//    n1.start
//    n2.start
//    n3.start
//
//    n1 ! "solve"
//    n3.barrier.await
//
//    //test for solve fire
//    assert( n2.sources.values.foldLeft(true)(_ && _) === true )
//    assert( n3.sources.values.foldLeft(true)(_ && _) === true )
//
//    //benchmarker output
//    BenchMarker reportData
//  }
}