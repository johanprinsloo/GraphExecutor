package org.graphexecutor

import org.graphexecutor.bench._
import org.graphexecutor.signals._
import akka.dispatch.{Future, Await}
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.ask
import akka.actor.{Props, ActorRef}

case class GxNode(){}
case class GxLink(){}

trait AdminGraph[N,L] {

  def addNode( n : N )
  def delNode( n : N )

  def addLink( l : L )
  def delLink( l : L )
}

class ShadowGraph extends AdminGraph[GxNode,GxLink] {
  var nodes = Set[GxNode]()
  var links = Set[GxLink]()

  def addNode( n : GxNode )  = { nodes += n }
  def delNode( n : GxNode )  = { nodes -= n }
  def addLink( l : GxLink )  = { links += l }
  def delLink( l : GxLink )  = { links -= l }
}

class GraphRunner[N,L,T <: AdminGraph[N,L]]( name: String, shadowgraph : T) extends NodeRunner( name )  {
  implicit val timeout = Timeout(5 seconds)
  var nodes: Set[NodeRunner] = Set()
  val snode = NodeRunner( name + "_startnode" )
  val enode = NodeRunner( name + "_endnode" )

  //snode -> this the famous graph virtual entry node paradox
  snode.registerDependent( this )
  Await.result( actor ? RegisterSource( snode.actor ), 1 second)

  //this -> enode  //internal node is a default source to the enode
  enode.registerSource(this)
  Await.result(actor ? RegisterDependent( enode.actor ), 1 second)

  nodes += ( snode, enode )

  def setStartNodes( snodes: Set[NodeRunner] ) = {
    snodes foreach { sn =>
      snode -> sn
    }
  }

  def setEndNodes( enodes: Set[NodeRunner] ) = {
    enodes.foreach( _ -> enode )
  }

  def ~>( internalnode: NodeRunner ) = {
    nodes += internalnode
    this
  }

  def ~~>( startnode: NodeRunner) = {
    snode -> startnode
    this
  }

  def ~~>( startnodes: List[NodeRunner]) = {
    startnodes foreach { sn => snode -> sn }
    this
  }

  def <~~( endnode: NodeRunner ) = {
    endnode -> enode
    this
  }

  def <~~( endnodes: List[NodeRunner] ) = {
    endnodes foreach { en => en -> enode }
    this
  }

  /**
   * TODO remove bleed
   * @param that
   * @return
   */
  override def ->( that: NodeRunner ): NodeRunner = {
    that.registerSource(this.enode)
    this.enode.registerDependent(that)
    println("linked (g) " + this.enode.name + " to " + that.name)
    return that
  }

  override def registerSource( node : NodeRunner ) = {
    snode.registerSource( node )
  }

  override def registerDependent( node : NodeRunner ) = {
    enode.registerDependent( node )
  }

  override def hasDependent( n : NodeRunner ) : Boolean = {
    enode.hasDependent(n)
  }

  override def hasSource( n : NodeRunner ) : Boolean = {
    snode.hasSource(n)
  }

  override def dependents : Set[ActorRef] = {
    enode.dependents
  }

  override def sources : Set[ActorRef] = {
    println("graph sources")
    snode.sources
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
  def apply( name: String, membernodes: Set[NodeRunner] ) = {
    var n = new GraphRunner[GxNode, GxLink, ShadowGraph]( name, new ShadowGraph( ) )
    n.nodes = membernodes
    n
  }

  def apply( name: String ) = {
    val n = new GraphRunner[GxNode, GxLink, ShadowGraph]( name, new ShadowGraph( ) )
    n
  }

  def apply( name: String, model: Work ) = {
    val n = new GraphRunner[GxNode, GxLink, ShadowGraph]( name, new ShadowGraph( ) )
    n.model = model
    n
  }

  def apply( aname: String, model: Work, benchmarker: Boolean ) = {
    val n = new GraphRunner[GxNode, GxLink, ShadowGraph]( aname, new ShadowGraph( ) )
    val bnc = NodeControl.system.actorOf(Props[BenchMarker], name = aname+"_benchmarker")
    n.model = model
    Await.result( (n.actor ? AddBenchMarker( bnc )), 5 seconds)
    n
  }
}