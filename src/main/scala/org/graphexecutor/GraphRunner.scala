//package org.graphexecutor
//
//import scala.actors._
//import scala.collection.mutable._
//import org.graphexecutor.bench._
//
//case class SubGraph( val timeout : Int ) extends Work {
//  def solve = {
//     //solving a subgraph asynchronously
//  }
//}
//
//class GraphRunner( name: String ) extends NodeRunner( name ) {
//  var nodes: Set[NodeRunner] = Set()
//  val snode = NodeRunner( name + "_startnode" )
//  val enode = NodeRunner( name + "_endnode" )
//  this ~>> enode
//
//  nodes += ( snode, enode )
//
//  def setStartNodes( snodes: Set[NodeRunner] ) = {
//    snode -> snodes
//  }
//
//  def setEndNodes( enodes: Set[NodeRunner] ) = {
//    enodes.foreach( _ -> enode )
//  }
//
//  def ~>( internalnode: NodeRunner ): GraphRunner = {
//    nodes += internalnode
//    return this
//  }
//
//  override def toString() = name
//
//  override def act = {
//    println( "GraphRunner " + name + " activated" )
//    loop {
//      react {
//        case goVote: GoVote              => canGo( goVote )
//        case observer: Actor             => registerObserver( observer );
//        case observed: ObserveNodeStatus => statusChanged( observed );
//        case "solve"                     => run; reply { name + " solved" }
//        case "run"                       => run
//        case "reset"                     => reset; reply { name + " reset" }
//        case "exit"                      => { println( name + " stopping" ); barrier.unlock; exit() }
//        case "stop"                      => stopandclear(); reply { name + " stopped" }
//        case m =>
//          println( "unknown message: " + m + " to " + name )
//      }
//    }
//  }
//
//  def run = {
//    startsolve
//    model.solve
//    snode ! "solve"
//  }
//
//  override def canGo( goVote: GoVote ) = {
//    sources( goVote.source ) = true
//    if ( sources.values.foldLeft( true )( _ && _ ) ) {
//      println( name + " OK to execute.." )
//      run
//    } else {
//      println( name + " reference count upvote" )
//    }
//  }
//
//  override def statusChanged( observed: ObserveNodeStatus ) = {
//    if ( observed.source == enode && observed.status == NodeStatus.complete ) {
//      completesolve
//      dependents foreach { dependent =>
//        println( name + " kicking " + dependent )
//        dependent ! new GoVote( this )
//      }
//      barrier.unlock
//      println( name + " complete" )
//    }
//  }
//
//  override def reset = {
//    nodes foreach { node => node ! "reset" }
//    super.reset
//  }
//
//  def stopandclear() {
//    println( "Graph " + name + " done " )
//    nodes foreach { node => node ! "exit" }
//    nodes.clear()
//    exit
//  }
//}
//
//object GraphRunner {
//  def apply( name: String, membernodes: Set[NodeRunner] ): GraphRunner = {
//    var n = new GraphRunner( name )
//    n.nodes = membernodes
//    n.start
//    return n
//  }
//
//  def apply( name: String ): GraphRunner = {
//    val n = new GraphRunner( name )
//    n.start()
//    return n
//  }
//
//  def apply( name: String, model: Work ): GraphRunner = {
//    val n = new GraphRunner( name )
//    n.model = model
//    n.start()
//    return n
//  }
//
//  def apply( name: String, model: Work, benchmarker: Boolean ): GraphRunner = {
//    val n = new GraphRunner( name ) with BenchMarker
//    n.model = model
//    n.start()
//    return n
//  }
//}