package org.graphexecutor

import bench.{BenchControl, BenchMarker}
import org.scalatest.FunSuite
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import org.graphexecutor.signals.GetSolveCount
import org.scalatest.matchers.ShouldMatchers
import akka.actor.{PoisonPill, ActorRef}
import akka.pattern.gracefulStop
import org.scalatest.Assertions._
import scala.Int
import akka.dispatch.{Await, Future}
import NodeControl._


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

    val fu2 = node.actor ? "solve"
    val c2 = Await.result( fu2 , 5 seconds).asInstanceOf[String]
    c2 should be ("testnode1 solved")

    val fu3 = node.actor ? GetSolveCount()
    val c3 = Await.result( fu3 , 5 seconds).asInstanceOf[Int]
    c3 should be (1)

    val stopped: Future[Boolean] = gracefulStop(node.actor, 5 seconds)(NodeControl.system)
    val stop = Await.result(stopped, 6 seconds)
    stop should be (true)
  }

  test("create nodes through companion") {
    val n = NodeRunner("n1tst")
    assert( n.isInstanceOf[NodeRunner] === true, "NodeRunner Type check")
    assert( n.actor.isInstanceOf[ActorRef] === true, "NodeRunner Actor type check")
    assert( n.isSolvable === true )
    ~n
  }

  test("basic benchmark test") {
    BenchControl.clear
    BenchControl.size should be (0)
    val n1 = NodeRunner("node1btest", new NoopWork(), true)
    val n2 = NodeRunner("node2btest", new NoopWork(), true)


    n1 -> n2
    n1.solveAsync
    n2.blockUntilSolved(1)

    BenchControl.size should be (2)

    val bdata = BenchControl.reportRawdata()

    val rs = for (
      i <- bdata
    ) yield i._1
    println(">>>bnchdata:" + rs)

    BenchControl.reportData()

    BenchControl.clear
    BenchControl.size should be (0)

    ~n1
    ~n2

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

    node1.stopActor()
    node2.stopActor()
  }


  test("mutiple node creation with BenchMarker mixed in and significant model load") {
    val scale = 20  //5000 -> 6 Gb heap
    val dt = 0.00001
    val benchmark = true
    val n1 = NodeRunner("n1", SSsystem(scale, dt), benchmark )
    val n2 = NodeRunner("n2", SSsystem(scale, dt), benchmark )
    val n3 = NodeRunner("n3", SSsystem(scale, dt), benchmark )
    val n4 = NodeRunner("n4", SSsystem(scale, dt), benchmark )
    val n5 = NodeRunner("n5", SSsystem(scale, dt), benchmark )
    val n6 = NodeRunner("n6", SSsystem(scale, dt), benchmark )
    val n7 = NodeRunner("n7", SSsystem(scale, dt), benchmark )
    val n8 = NodeRunner("n8", SSsystem(scale, dt), benchmark )
    val n0 = NodeRunner("n0", new NoopWork, benchmark )

    n1 -> n0
    n2 -> n0
    n3 -> n0
    n4 -> n0
    n5 -> n0
    n6 -> n0
    n7 -> n0
    n8 -> n0

    assert(n1.isInstanceOf[NodeRunner] === true)
    val bnc = BenchControl.listBenchers
    //info(">>Benchmarking nodes: \n" + bnc)

    n1.actor ! "solve"
    n2.actor ! "solve"
    n3.actor ! "solve"
    n4.actor ! "solve"
    n5.actor ! "solve"
    n6.actor ! "solve"
    n7.actor ! "solve"
    n8.actor ! "solve"

    val solved  = n0.blockUntilSolved()
    solved should be (true)

    BenchControl reportData

    BenchControl.clear
    BenchControl.listBenchers should be ("")

    NodeControl stopNodes( n1::n2::n3::n4::n5::n6::n7::Nil )
  }

}