package org.graphexecutor

import scala.collection.mutable._
import org.graphexecutor.bench._
import org.graphexecutor.signals._
import akka.dispatch.{Future, Await}
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask
import akka.actor.{Props, ActorRef}

class GraphRunner( name: String ) extends NodeRunner( name ) {
  implicit val timeout = Timeout(5 seconds)
  var nodes: Set[NodeRunner] = Set()
  val snode = NodeRunner( name + "_startnode" )
  val enode = NodeRunner( name + "_endnode" )
  this ~>> enode
  this -> enode  //internal node is a default source to the enode

  nodes += ( snode, enode )

  def setStartNodes( snodes: Set[NodeRunner] ) = {
    //snode -> snodes  TODO
    snodes foreach { sn =>
      snode -> sn
    }
  }

  def setEndNodes( enodes: Set[NodeRunner] ) = {
    enodes.foreach( _ -> enode )
  }

  def ~>( internalnode: NodeRunner ): GraphRunner = {
    nodes += internalnode
    return this
  }

  override def toString() = name

  override def solveSync() = {
    actor ! "solve"
    snode.solveAsync()
    val sc = enode.getSolveCount
    enode.blockUntilSolved(sc+1,10000)
  }

  override def solveAsync() = {
    actor ! "solve"
    snode.solveAsync()
  }

  override def reset() = {
    val flist = nodes.map( nd => nd.actor ? "reset" )    //List[Future[Boolean]]
    val fc = for {
        f1 <- actor ? "reset"
        f2 <- snode.actor ? "reset"
        f3 <- snode.actor ? "reset"
        f4 <- Future.sequence(flist)
      } yield (f1,f2,f3,f4)

    Await.result(fc, 5 seconds)
  }

  override def unary_~ = {
    stopActor
    ~enode
    ~snode
    NodeControl.stopNodes( nodes.toList )
  }

}

object GraphRunner {
  implicit val timeout = Timeout(5 seconds)
  def apply( name: String, membernodes: Set[NodeRunner] ): GraphRunner = {
    var n = new GraphRunner( name )
    n.nodes = membernodes
    return n
  }

  def apply( name: String ): GraphRunner = {
    val n = new GraphRunner( name )
    return n
  }

  def apply( name: String, model: Work ): GraphRunner = {
    val n = new GraphRunner( name )
    n.model = model
    return n
  }

  def apply( aname: String, model: Work, benchmarker: Boolean ): GraphRunner = {
    val n = new GraphRunner( aname )
    val bnc = NodeControl.system.actorOf(Props[BenchMarker], name = aname+"benchmarker")
    n.model = model
    Await.result( (n.actor ? AddBenchMarker( bnc )), 5 seconds)
    return n
  }
}