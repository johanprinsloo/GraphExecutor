package org.graphexecutor

import scala.collection.mutable._
import org.graphexecutor.bench._


object runsimple {
  def main(args: Array[String]) {

    val n1 = new NodeRunner("n1") with BenchMarker
    val n2 = new NodeRunner("n2") with BenchMarker

    val observer = new NodeRunner("observer node") with NodeObserver
    observer observe n1
    observer ~>> n1 ~>> n2

    observer.start()
    n1.start()                 
    n2.start()

    n1 -> n2

    n1 ! "solve"

    n2.barrier.await()

    n1 ! "exit"
    n2 ! "exit"
    observer ! "exit"

    BenchMarker.reportRawdata()
    BenchMarker.clear


    val node1 = NodeRunner("node1", new Model(), true )
    val node2 = NodeRunner("node2", new Model(), true )
    val node3 = NodeRunner("node3", new Model(), true )
    val graph1 = GraphRunner("graph1", new Model(), true )


    node1 -> node2 -> node3
    graph1 ~> node1 ~> node2 ~> node3
    graph1.setStartNodes( Set(node1) )
    graph1.setEndNodes( Set(node3) )

    graph1 ! "solve"

    graph1.barrier.await()

    BenchMarker.reportRawdata()

    graph1 ! "stop"


  }
}