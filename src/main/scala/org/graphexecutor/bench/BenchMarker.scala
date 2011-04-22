package org.graphexecutor.bench

import scalala.tensor.dense._
import org.graphexecutor._

trait BenchMarker extends NodeRunner {
  override def markstartsolve = {
    println("mark start for " + name)
    BenchMarker.reportSolveStart( System.nanoTime() , name, currentThread.getId() )
  }

  override def markendsolve = {
    println("mark end for " + name)
    BenchMarker.reportSolveComplete( System.nanoTime() , name, currentThread.getId() )
  }
}


object BenchMarker {
  var history = scala.collection.mutable.ListBuffer.empty[Tuple3[Double, String, Long]]
  var timestamps = DenseVector(0,0)
  var activethreads = DenseVector(0,0)

  def report: String = {
    "report"
  }

  def clear = {
    history.clear()
  }

  def reportSolveStart(timestamp: Double, nodeId: String, threadId: Long) = {
    synchronized {
      history += new Tuple3((timestamp) / 1e6, nodeId, threadId)
    }
  }

  def reportSolveComplete(timestamp: Double, nodeId: String, threadId: Long) = {
    synchronized {
      history += new Tuple3((timestamp) / 1e6, nodeId, threadId)
    }
  }

  def reportRawdata() =  {
    println( history.toString() )
  }
}