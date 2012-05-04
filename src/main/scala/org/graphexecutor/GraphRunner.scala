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

trait ShadowGraph extends AdminGraph[GxNode,GxLink] {
  var nodes = List[GxNode]()
  var links = List[GxLink]()

  def addNode( n : GxNode )  = { nodes += n }
  def delNode( n : GxNode )  = { nodes -= n }
  def addLink( l : GxLink )  = { links += l }
  def delLink( l : GxLink )  = { links -= l }
}

class GraphRunner[G <: AdminGraph]( name: String, shadowgraph : G ) extends NodeRunner( name )  {
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

  def ~>( internalnode: NodeRunner ): GraphRunner = {
    nodes += internalnode
    return this
  }

  def ~~>( startnode: NodeRunner): GraphRunner = {
    snode -> startnode
    return this
  }

  def ~~>( startnodes: List[NodeRunner]): GraphRunner = {
    startnodes foreach { sn => snode -> sn }
    return this
  }

  def <~~( endnode: NodeRunner ): GraphRunner = {
    endnode -> enode
    return this
  }

  def <~~( endnodes: List[NodeRunner] ): GraphRunner = {
    endnodes foreach { en => en -> enode }
    return this
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
    val bnc = NodeControl.system.actorOf(Props[BenchMarker], name = aname+"_benchmarker")
    n.model = model
    Await.result( (n.actor ? AddBenchMarker( bnc )), 5 seconds)
    return n
  }
}