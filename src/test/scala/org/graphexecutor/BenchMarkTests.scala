package org.graphexecutor

import bench.BenchControl
import org.scalatest.FunSuite
import org.scalatest.matchers.ShouldMatchers

class BenchMarkTests  extends FunSuite with ShouldMatchers {

  test("adding and removing nodes from the singleton controller") {

    BenchControl.clear()
    BenchControl.listBenchers should be ('empty)
    BenchControl.size should be (0)
    BenchControl.numreports should be (0)

    val nd1 = NodeRunner("nd1", new NoopWork() ,true)

    BenchControl.listBenchers should include ("nd1")
    BenchControl.size should be (1)
    BenchControl.numreports should be (0)

    BenchControl.clear
    ~nd1

    BenchControl.listBenchers should be ('empty)
    BenchControl.size should be (0)
    BenchControl.numreports should be (0)

    val n1 = NodeRunner("n1", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("n1")
    BenchControl.size should be (1)
    BenchControl.numreports should be (0)

    val n2 = NodeRunner("n2", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("n2")
    BenchControl.size should be (2)
    BenchControl.numreports should be (0)

    val n3 = NodeRunner("n3", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("n3")
    BenchControl.size should be (3)
    BenchControl.numreports should be (0)

    val n4 = NodeRunner("n4", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("n4")
    BenchControl.size should be (4)
    BenchControl.numreports should be (0)

    n1 -> n2 -> n3 -> n4

    n1.solveAsync()
    n4.blockUntilSolved(1)

    val report1 = BenchControl.reportData()
    BenchControl.numreports should be (4)
    println(report1)
    report1 should ( include ("n1") and include ("n2") and include ("n3") and include ("n4") )

    n1.solveAsync()
    n4.blockUntilSolved(2)


    BenchControl.clear()
    BenchControl.listBenchers should be ('empty)
    BenchControl.size should be (0)
    BenchControl.numreports should be (0)
    ~n1; ~n2; ~n3; ~n4

    val x1 = NodeRunner("x1", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("x1")
    BenchControl.size should be (1)
    BenchControl.numreports should be (0)

    val x2 = NodeRunner("x2", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("x2")
    BenchControl.size should be (2)
    BenchControl.numreports should be (0)

    val x3 = NodeRunner("x3", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("x3")
    BenchControl.size should be (3)
    BenchControl.numreports should be (0)

    val x4 = NodeRunner("x4", SSsystem(1000, 0.1), true )
    BenchControl.listBenchers should include ("x4")
    BenchControl.size should be (4)
    BenchControl.numreports should be (0)


    x1 -> x2 -> x3 -> x4

    x1.solveAsync()
    x4.blockUntilSolved(1)

    val report2 = BenchControl.reportData()
    BenchControl.numreports should be (4)

    println(report2)
    report2 should ( include ("x1") and include ("x2") and include ("x3") and include ("x4") )
    ~x1; ~x2; ~x3; ~x4

  }
}
