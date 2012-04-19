package org.graphexecutor

import scala.collection.mutable._
import org.graphexecutor.bench._
import org.graphexecutor.NodeStatus._
import akka.util.Timeout
import akka.util.duration._
import akka.actor.{Props, ActorSystem, ActorRef, Actor}
import akka.dispatch.Await
import akka.pattern.ask
import signals._

object NodeControl {
  val system = ActorSystem("GraphExecutor")
}

class Node(val config : NodeRunner, var model: Work ) extends Actor {
  var solveCount = 0
  var dependents: Set[ActorRef] = Set.empty
  var sources: scala.collection.mutable.HashMap[ActorRef, Boolean] = scala.collection.mutable.HashMap.empty
  val observers: Set[ActorRef] = Set.empty
  val benchmarkers : Set[ActorRef] = Set.empty

  def receive = {
        case goVote: GoVote              => canGo( goVote )
        case dep : RegisterDependent     => dependents += dep.actor
        case src : RegisterSource        => sources += ( src.actor -> false )
        case bnc : AddBenchMarker        => benchmarkers += bnc.actor
        case obs : RegisterObserver      => observers += obs.actor
        case observed: ObserveNodeStatus => statusChanged( observed ); config.statusChanged( observed )
        case "solve"                     => solve; sender ! { config.name + " solved" }
        case "reset"                     => reset; sender ! { config.name + " reset" }
        case m : Solveable               => sender ! isNodeSolveable
        case m : GetSolveCount           => sender ! solveCount
        case m                           => println( "unknown message " + m + " to " + config.name )
  }

  def canGo( goVote: GoVote ) = {

    sources( goVote.source ) = true

    if ( sources.values.foldLeft( true )( _ && _ ) ) {
      println( config.name + " OK to execute.." )
      solve
    } else {
      println( config.name + " reference count upvote" )
    }
  }

  def isConnected : Boolean = !dependents.isEmpty && !sources.isEmpty
  def isNodeSolveable : Boolean = true

  def solve = {
    startsolve
    model.solve
    solveCount = solveCount + 1
    completesolve
    dependents foreach { dependent =>
      println( config.name + " kicking " + dependent )
      dependent ! new GoVote( self )
    }
    println( config.name + " complete" )
  }

  def startsolve = {
    markstartsolve
    benchmarkers foreach { b => b ! SolveStartReport( Thread.currentThread.getId ) }
    notifyObservers( NodeStatus.start )
  }

  def completesolve = {
    markendsolve
    benchmarkers foreach { b => b ! SolveCompleteReport() }
    notifyObservers( NodeStatus.complete )
  }

  def markstartsolve = { println( "solve start " + config.name ) }
  def markendsolve = { println( "solve complete " + config.name ) }


  def notifyObservers( status: NodeStatus ) = {
    observers foreach { observer => observer ! new ObserveNodeStatus( this.asInstanceOf[NodeRunner], status ) }
  }

  def statusChanged( observed: ObserveNodeStatus ) = {
    println("observed status change to " + observed.status + " in " + observed.source + " by " + this)
  }

  def reset = { //TODO: more elegant solution: sources.foreach(src => src._2 = false)  //.forall(_ => false)
    for ( source <- sources ) sources( source._1 ) = false
    solveCount = 0
    notifyObservers( NodeStatus.reset )
  }
}




class NodeRunner( val name: String , var model : Work = new NoopWork() ) {

  implicit val timeout = Timeout(5 seconds)
  val actor = NodeControl.system.actorOf( Props( new Node(this, model) ), name = name )
  override def toString() = name

  def isSolvable : Boolean = {
    val fu = actor ? Solveable()
    Await.result( fu, 5 seconds ).asInstanceOf[Boolean]
  }

  def ->( that: NodeRunner ): NodeRunner = {
    that.actor ! RegisterSource( this.actor )
    actor ! RegisterDependent( that.actor )
    return that
  }

  def ->( thats: Set[NodeRunner] ): NodeRunner = {
    thats foreach { that =>
      that.actor ! RegisterSource( this.actor )
      actor ! RegisterDependent( that.actor )
    }
    return this
  }

  def observe( observee: NodeRunner ) = {
    observee.actor ! RegisterObserver( this.actor )
  }

  def ~>>( observee: NodeRunner ): NodeRunner = {
    observee.actor ! RegisterObserver( this.actor )
    return this
  }

  def addSource( source: NodeRunner ) = {
    actor ! RegisterSource( source.actor )
  }

  def addSources( ss: Set[NodeRunner] ) = {
    ss foreach { source => actor ! RegisterSource( source.actor ) }
  }

  def addDependent( dep: NodeRunner ) = {
    actor ! RegisterDependent( dep.actor )
  }

  def addDependents( ss: Set[NodeRunner] ) = {
    ss foreach { entry => actor ! RegisterDependent( entry.actor ) }
  }

  def statusChanged( observed: ObserveNodeStatus ) = {
    println( "observed status change : " + observed.status + " in " + name )
  }

  def blockUntilSolved( times: Int = 1, maxhang : Long = 2000L, polltime : Long = 50 ) : Boolean = {
    var solvec = 0
    var hangtime = 0L
    while( solvec < times && hangtime < maxhang ) {
      val fu = actor ? GetSolveCount()
      Thread.sleep(polltime); hangtime = hangtime + polltime
      solvec = Await.result( fu, 5 seconds ).asInstanceOf[Int]
    }
    hangtime < maxhang
  }

}

/**
 * Companion Object:
 * Cheap simple singleton builder/factory for NodeRunner
 */
object NodeRunner {

  private def makeNodeRunner(n: NodeRunner): NodeRunner = {
    return n
  }

  def apply( name: String ): NodeRunner = {
    makeNodeRunner( new NodeRunner( name ) )
  }

  def apply( name: String, model: Work ): NodeRunner = {
    makeNodeRunner( new NodeRunner( name, model ) )
  }

  def apply( aname: String, model: Work, benchmarker: Boolean ): NodeRunner = {
    val nr = makeNodeRunner( new NodeRunner( aname, model ) )

    val bnc = NodeControl.system.actorOf(Props[BenchMarker], name = aname+"benchmarker")
    nr.actor ! AddBenchMarker( bnc )
    nr
  }

  def apply( name: String, sources: Set[Actor], dependents: Set[Actor] ) {
    // TODO: complete constructor
  }
}