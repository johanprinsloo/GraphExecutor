package org.graphexecutor.bench

import scalala.tensor.dense._
import org.graphexecutor._

trait BenchMarker extends NodeRunner {

  var elapsedtime: Long = 0
  
  override def markstartsolve = {
    println("mark start for " + name)
    elapsedtime = System.nanoTime()
    BenchMarker.reportSolveStart( elapsedtime , name, currentThread.getId() )
  }

  override def markendsolve = {
    println("mark end for " + name)
    val temptime = elapsedtime
    elapsedtime = System.nanoTime()
    BenchMarker.reportSolveComplete( elapsedtime , name, currentThread.getId() )
    elapsedtime = elapsedtime - temptime
    println("\t elapsedtime for " + name + " : " + elapsedtime)
  }
}

object BenchMarker {
  var history = scala.collection.mutable.ListBuffer.empty[Tuple4[Double, String, String, Long]]
  var timestamps = DenseVector(0,0)
  var activethreads = DenseVector(0,0)

  def report: String = {
    // for(n <- history) yield { "Node: %s %s \t\t timestamp: %f   on thread %d\n".format(n._2, n._3, n._1, n._4) }
    var ret = ""
    history foreach { i => 
      ret = ret + "Node: %s %s \t\t timestamp: %f   on thread %d\n".format(i._2, i._3, i._1, i._4)
    }
    return ret
  }

  def clear = {
    history.clear()
  }

  def reportSolveStart(timestamp: Double, nodeId: String, threadId: Long) = {
    synchronized {
      history += new Tuple4((timestamp) / 1e6, nodeId, "start the solve", threadId)
    }
  }

  def reportSolveComplete(timestamp: Double, nodeId: String, threadId: Long) = {
    synchronized {
      history += new Tuple4((timestamp) / 1e6, nodeId, "complete solve", threadId)
    }
  }

  def reportRawdata() =  {
    println( history.toString() )
  }
  
  def reportData() = {
    println( report )
//    history foreach { i => 
//      println("Node: " + i._2 + " " + i._3   +  "\t\t timestamp : " + i._1 + "\t thread : " + i._4 )
//    }
  }
  
}