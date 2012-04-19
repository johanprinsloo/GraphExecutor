package org.graphexecutor.bench

import scala.actors._
import scala.collection.mutable.IndexedSeq
import scalala.tensor.dense._
import scalala.library._
import scalala.library.Library._
import scalala.library.LinearAlgebra._
import scalala.library.Statistics._
import scalala.library.Plotting._
//import scalala.library.Vectors
import scalala.operators.Implicits._

case class NodeStart( nodeId: String, threadId: Long )
case class NodeEnd( nodeId: String, threadId: Long )

object BenchMark extends Actor {
  var startstamp = System.nanoTime()
  var history = scala.collection.mutable.ListBuffer.empty[Tuple3[Double, String, Long]]
  var timestamps = DenseVector( 0, 0 )
  var activethreads = DenseVector( 0, 0 )

  def act {
    startstamp = System.nanoTime()
    history.clear()
    println( "Benchmark Started at " + startstamp )
    loop {
      react {
        case "mark start"  => startstamp = System.nanoTime()
        case ns: NodeStart => reportNodeStart( ns )
        case ne: NodeEnd   => reportNodeComplete( ne )
        case "report"      => reportHistory
        case "exit"        => println( "exit message to BenchMark..." ); exit()
        case _             => println( "garbage message to BenchMark..." )
      }
    }
  }

  private def reportNodeStart( ns: NodeStart ): Unit = {
    val timestamp = System.nanoTime()
    println( "Node Start: " + ns.nodeId + " thread: " + ns.threadId + " at nano: " + timestamp )
    history += new Tuple3( ( timestamp - startstamp ) / 1e6, ns.nodeId, ns.threadId )
  }

  private def reportNodeComplete( ne: NodeEnd ): Unit = {
    val timestamp = System.nanoTime()
    println( "Node Complete: " + ne.nodeId + " thread: " + ne.threadId + " at nano: " + timestamp )
    history += new Tuple3( ( timestamp - startstamp ) / 1e6, ne.nodeId, ne.threadId )
  }

  def reportHistory: Unit = {

    var timev = DenseVector[Double]( history.size, 0 )
    var nodev: Set[String] = Set()
    var threadv = DenseVector[Long]( history.size, 0 )
    var ttset: List[Long] = List()

    //    history.foreach{ entry =>
    //      println(entry._2 + ", " + entry._1 + ", "+ entry._3)
    //      timev += entry._1
    //      nodev += entry._2
    //      threadv += entry._3
    //    }

    history groupBy ( _._3 )

    history.zipWithIndex.foreach {
      case ( entry, i ) =>
        println( entry._2 + ", " + entry._1 + ", " + entry._3 )
        timev( i ) = entry._1
        nodev += entry._2
        threadv( i ) = entry._3
      //ttset + entry._3
    }

    val uThreads = ttset.distinct

    //timev = history._1
    //val x = Vectors.linspace(0,1);
    
    val x = DenseVector.range(0,100) / 100.0;
    plot( timev, threadv )
    //hold(true)
    //plot(x, x :^ 3, '.')
    xlabel( "Time Stamp" )
    ylabel( "Thread Occupancy" )
    saveas( "./Bench.png" ) // save current figure as a .png, eps and pdf also supported
  }
}