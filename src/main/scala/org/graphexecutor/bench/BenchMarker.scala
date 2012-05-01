package org.graphexecutor.bench

import scalala.tensor.dense._
import org.graphexecutor._
import akka.actor._
import akka.util.Timeout
import akka.util.duration._
import signals._
import compat.Platform
import akka.pattern._
import akka.dispatch.{ExecutionContext, Future, Await}

case class Register( b : ActorRef)
case class Unregister( b : ActorRef)
case class SolveReport( node : String, start : Long, end : Long , thread : Long  )

class BenchMarker extends Actor {

  var node : String = ""
  var solvestart : Long = 0
  var solvecomplete : Long = 0
  var threadId : Long = -1

  override def preStart() {
    BenchControl.accumulator ! Register(self)
  }

  override def postStop {
    BenchControl.accumulator ! Unregister(self)
  }

  def receive = {
    case s : SolveStartReport     => {
      solvestart  = Platform.currentTime
      threadId = s.threadId
      node = sender.toString()
    }
    case s : SolveCompleteReport  => {
      solvecomplete = Platform.currentTime
      assert(node == sender.toString())
      BenchControl.accumulator ! SolveReport(node, solvestart, solvecomplete, threadId)
    }
    case "reset" => {
      solvestart = 0
      solvecomplete = 0
      threadId = -1
    }
    case "listNode" => sender ! node
  }
}

class Accumulator extends Actor {

  import BenchControl._
  val system = NodeControl.system
  implicit val ec = ExecutionContext.defaultExecutionContext(system)
  implicit val timeout = Timeout(5 seconds)


  var registry = collection.mutable.ListBuffer[ActorRef]()
  var history = collection.immutable.Set[HistEntry]()

  def receive = {
    case r : Register => registry += r.b
    case u : Unregister => registry -= u.b
    case r : SolveReport => history += ((r.node, r.start, r.end, r.thread))
    case "report" => sender ! history
    case "reset" => {
      history = Set.empty
      registry foreach {
        a => a ! "reset"
      }
      sender ! registry.size
    }
    case "clear" => {
      println("clearing benchmarking accumulator from: " + registry.size + " entries")
      val flist = registry.map( a => gracefulStop(a, 5 seconds)(NodeControl.system) )
      Await.result(Future.sequence(flist), 10 seconds)
      registry.clear()
      println("\t to : " + registry.size + " entries")
      sender ! registry.size
    }
    case "size" => sender ! registry.size
    case "listBenchers" => {
      val sb = new StringBuilder()
      registry foreach { bencher =>
         val fu = bencher ? "listNode"
         val nodeString = Await.result(fu, 1 second).asInstanceOf[String]
         sb.append( nodeString + "\n" )
      }
      sender ! sb.result()
    }
  }

}


object BenchControl {

  import NodeControl._
  implicit val timeout = Timeout(5 seconds)
  type HistEntry = Tuple4[String, Long, Long, Long]
  var history : Set[HistEntry] = Set.empty

  val accumulator = NodeControl.system.actorOf(Props[Accumulator], name = "benchAccumulator")

  def report: String = {
    val fu = accumulator ? "report"
    val history = Await.result(fu, 10 seconds).asInstanceOf[Set[HistEntry]]
    var ret = ""
    history foreach { i =>
      ret = ret + "Node: %s \t start: %d  end: %d duation: %d  on thread %d\n".format( i._1, i._2, i._3, (i._3 - i._2), i._4 )
    }
    return ret
  }

  def clear  = {
    val fu = accumulator ? "clear"
    history = Set.empty
    Await.result( fu , 5 seconds )
  }

  def reset = {
    val fu = accumulator ? "reset"
    history = Set.empty
    Await.result( fu, 5 seconds )
  }

  def size : Int = {
    val fu = accumulator ? "size"
    Await.result(fu, 5 seconds).asInstanceOf[Int]
  }

  def listBenchers : String = {
    val fu = accumulator ? "listBenchers"
    Await.result(fu, 5 seconds).asInstanceOf[String]
  }

  def reportRawdata() : Set[HistEntry] = {
    history
  }

  def reportData() = {
    println( report )
  }

}