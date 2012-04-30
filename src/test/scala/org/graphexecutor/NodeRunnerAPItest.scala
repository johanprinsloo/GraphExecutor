package org.graphexecutor

import bench.{BenchControl, BenchMarker}
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers


class NodeRunnerAPItest extends FunSuite with ShouldMatchers {

  test("node configuration test - test for correct node connections") {
    val n1 = NodeRunner("n1ctest", new NoopWork())
    val n2 = NodeRunner("n2ctest", new NoopWork())
    val n3 = NodeRunner("n3ctest", new NoopWork())

    n1 -> n2 -> n3

    //correct dependents
    assert( n1.hasDependent(n2) === true )
    assert( n2.hasDependent(n3) === true )

    //correct sources
    assert( n2.hasSource(n1) === true )
    assert( n3.hasSource(n2) === true )

    //stop actors
    ~n1
    ~n2
    ~n3
  }

  test("observation from other actors") {
    val n1 = new NodeRunner("n1")
    val n2 = new NodeRunner("n2")
    val n3 = new NodeRunner("n3")
    val observer = new NodeRunner("observer") with NodeObserver
    n1 -> n2 -> n3
    observer ~>> n1 ~>> n2 ~>> n3

    BenchControl.reset

    n1.hasObserver(observer) should be (true)
    n2.hasObserver(observer) should be (true)
    n3.hasObserver(observer) should be (true)

    n1.solveAsync
    n3.blockUntilSolved(1)

    //benchmarker output
    BenchControl.reportData()
    NodeControl stopNodes n1::n2::n3::Nil
  }

  test("multi-node execution order check with reset ") {
    val n1 = NodeRunner("n1mtest",SSsystem(100, 0.01), true)
    val n2 = NodeRunner("n2mtest",SSsystem(100, 0.01), true)
    val n3 = NodeRunner("n3mtest",SSsystem(100, 0.01), true)

    n1 -> n2 -> n3

    n1.getSolveCount should be (0)
    n2.getSolveCount should be (0)
    n3.getSolveCount should be (0)

    (BenchControl size) should be (3)

    n1.solveAsync
    n3.blockUntilSolved()

    n1.getSolveCount should be (1)
    n2.getSolveCount should be (1)
    n3.getSolveCount should be (1)

    BenchControl.reportData()
    BenchControl.reset

    NodeControl resetNodes( n1::n2::n3::Nil )
    n1.getSolveCount should be (0)
    n2.getSolveCount should be (0)
    n3.getSolveCount should be (0)

    n1.solveAsync
    n3.blockUntilSolved()

    n1.getSolveCount should be (1)
    n2.getSolveCount should be (1)
    n3.getSolveCount should be (1)

    BenchControl.reportData()

    BenchControl.clear
    NodeControl stopNodes n1::n2::n3::Nil

  }

  test("nodes should reset execution stataus correctly") {
    val testnode = NodeRunner("testnode")

    ( testnode getSolveCount ) should be (0)
    testnode.solveSync()
    testnode.blockUntilSolved(1)

    ( testnode getSolveCount ) should be (1)
    testnode.solveSync()
    testnode.blockUntilSolved(2)
    ( testnode getSolveCount ) should be (2)

    testnode.reset()
    ( testnode getSolveCount ) should be (0)
  }


}

