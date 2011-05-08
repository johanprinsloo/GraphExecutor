package org.graphexecutor

import bench.BenchMarker
import org.scalatest.FunSuite


class NodeRunnerTests  extends FunSuite {
  test("node creation with class") {
    val node = new NodeRunner("testnode1")
    assert(node.isInstanceOf[NodeRunner] === true)
    node.start

    val t1 = node !? "solve"
    assert( t1 === "testnode1 solved" )
  }

  test("node creation with companion object") {
    val node = NodeRunner("testnode2", new Model())
    assert(node.isInstanceOf[NodeRunner] === true)
    node.start

    val t1 = node !? "solve"
    assert( t1 === "testnode2 solved" )
  }

  test("node creation with BenchMarker mixed in ") {
    val n1 = new NodeRunner("n1") with BenchMarker
    assert(n1.isInstanceOf[NodeRunner] === true)
    assert(n1.isInstanceOf[BenchMarker] === true)
    n1.start
    
    val t1 = n1 !? "solve"
    assert( t1 === "n1 solved" )
  }


  test("node creation with BenchMarker mixed in and significant model load") {
    val n1 = NodeRunner("n1", SSsystem(5000, 0.01), true )
    assert(n1.isInstanceOf[NodeRunner] === true)
    assert(n1.isInstanceOf[BenchMarker] === true)
        
    val t1 = n1 !? "solve"
    assert( t1 === "n1 solved" )
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
    
    assert(n1.isInstanceOf[NodeRunner] === true)
    assert(n1.isInstanceOf[BenchMarker] === true)
    BenchMarker clear
    
    n1 ! "solve"
    n2 ! "solve"
    n3 ! "solve"
    n4 ! "solve"
    n5 ! "solve"
    n6 ! "solve"
    n7 ! "solve"
    
    val t1 = n8 !? "solve"
    assert( t1 === "n8 solved" )
    Thread.sleep(1000)
    BenchMarker reportData
  }

  
  test("node configuration test - test for correct node connections") {
    val n1 = NodeRunner("n1", new Model())
    val n2 = NodeRunner("n1", new Model())
    val n3 = NodeRunner("n1", new Model())

    n1 -> n2 -> n3

    //correct dependents
    assert( n1.dependents.contains(n2) === true )
    assert( n2.dependents.contains(n3) === true )

    //correct sources
    assert( n2.sources.contains(n1) === true )
    assert( n3.sources.contains(n2) === true )

    //correct source init
    assert( n2.sources.get(n1).get === false )
    assert( n3.sources.get(n2).get === false )
  }

  test("multi-node creation, execution with BenchMarking and barrier wait ") {
    val n1 = new NodeRunner("n1") with BenchMarker
    val n2 = new NodeRunner("n2") with BenchMarker
    val n3 = new NodeRunner("n3") with BenchMarker
    
    n1 -> n2 -> n3
    n1.start
    n2.start
    n3.start

    BenchMarker clear
    
    n1 ! "solve"
    n3.barrier.await

    //test for solve fire
    assert( n2.sources.values.foldLeft(true)(_ && _) === true )
    assert( n3.sources.values.foldLeft(true)(_ && _) === true )

    //benchmarker output
    BenchMarker reportData
  }

    test("multi-node creation, execution with observation from other actors") {
    val n1 = new NodeRunner("n1") with BenchMarker
    val n2 = new NodeRunner("n2") with BenchMarker
    val n3 = new NodeRunner("n3") with BenchMarker
    val observer = new NodeRunner("observer") with NodeObserver
    n1 -> n2 -> n3
    observer ~>> n1 ~>> n2 ~>> n3

    BenchMarker clear
    
    assert( n1.observers.contains(observer) === true )
    assert( n2.observers.contains(observer) === true )
    assert( n3.observers.contains(observer) === true )

    observer.start
    n1.start
    n2.start
    n3.start

    n1 ! "solve"
    n3.barrier.await

    //test for solve fire
    assert( n2.sources.values.foldLeft(true)(_ && _) === true )
    assert( n3.sources.values.foldLeft(true)(_ && _) === true )

    //benchmarker output
    BenchMarker reportData
  }
}