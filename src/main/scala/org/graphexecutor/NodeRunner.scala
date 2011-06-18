package org.graphexecutor

import scala.actors._
import scala.collection.mutable._
import org.graphexecutor.bench._
import org.graphexecutor.NodeStatus._

case class GoVote( source: Actor )

class NodeRunner( val name: String, var model: Model = new Model() ) extends Actor {
  val barrier = new SimpleBarrier( name )
  var dependents: Set[Actor] = Set()
  var sources: scala.collection.mutable.HashMap[Actor, Boolean] = scala.collection.mutable.HashMap()
  val observers: Set[Actor] = Set()

  override def toString() = name

  def act = {
    println( "NodeRunner " + name + " activated" )
    reset
    loop {
      react {
        case goVote: GoVote              => canGo( goVote )
        case observer: Actor             => registerObserver( observer );
        case observed: ObserveNodeStatus => statusChanged( observed );
        case "solve"                     => solve; reply { name + " solved" }
        case "reset"                     => reset; reply { name + " reset" }
        case "exit"                      => println( name + " stopping" ); barrier.unlock; exit(); reply { reply { name + " exit" } }
        case m                           => println( "unknown message " + m + " to " + name );
      }
    }
  }

  def reset = { //TODO: more elegant solution: sources.foreach(src => src._2 = false)  //.forall(_ => false)
    for ( source <- sources ) sources( source._1 ) = false
    notifyObservers( NodeStatus.reset )
    barrier.lockup
  }

  def ->( that: NodeRunner ): NodeRunner = {
    that addSource this
    this.dependents += that
    return that
  }

  def ->( thats: Set[NodeRunner] ): NodeRunner = {
    thats foreach { that =>
      that addSource this
      this.dependents += that
    }
    return this
  }

  def observe( observee: NodeRunner ) = {
    observee.observers += this
  }

  def ~>>( observee: NodeRunner ): NodeRunner = {
    observee.observers += this
    return this
  }

  def registerObserver( observer: Actor ): Boolean = {
    observers += observer
    observers.contains( observer )
  }

  def notifyObservers( status: NodeStatus ) = {
    observers foreach { observer => observer ! new ObserveNodeStatus( this.asInstanceOf[NodeRunner], status ) }
  }

  def statusChanged( observed: ObserveNodeStatus ) = {
    //println("observed status change to " + observed.status + " in " + observed.source + " by " + this)
  }

  def canGo( goVote: GoVote ) = {

    sources( goVote.source ) = true

    if ( sources.values.foldLeft( true )( _ && _ ) ) {
      println( name + " OK to execute.." )
      solve
    } else {
      println( name + " reference count upvote" )
    }
  }

  def solve = {
    startsolve
    model.solve
    completesolve
    dependents foreach { dependent =>
      println( name + " kicking " + dependent )
      dependent ! new GoVote( this )
    }
    barrier.unlock
    println( name + " complete" )
  }

  def startsolve = {
    markstartsolve
    notifyObservers( NodeStatus.start )
  }

  def completesolve = {
    markendsolve
    notifyObservers( NodeStatus.complete )
  }

  def addSource( source: Actor ) = {
    sources += ( source -> false )
  }

  def addSources( ss: Set[Actor] ) = {
    ss foreach { entry => sources put ( entry, false ) }
  }

  def addDependent( dep: Actor ) = {
    dependents += dep
  }

  def addDependents( ss: Set[Actor] ) = {
    ss foreach { entry => dependents += entry }
  }

  def markstartsolve = { println( "solve start " + name ) }
  def markendsolve = { println( "solve complete " + name ) }
}

/**
 * Companion Object:
 * Cheap simple singleton builder/factory for NodeRunner
 */
object NodeRunner {

  private def makeNodeRunner(n: NodeRunner): NodeRunner = {
    n.start
    return n
  }

  def apply( name: String ): NodeRunner = {
    makeNodeRunner( new NodeRunner( name ) )
  }

  def apply( name: String, model: Model ): NodeRunner = {
    makeNodeRunner( new NodeRunner( name, model ) )
  }

  def apply( name: String, model: Model, benchmarker: Boolean ): NodeRunner = {
    makeNodeRunner( new NodeRunner( name, model ) with BenchMarker )
  }

  def apply( name: String, sources: Set[Actor], dependents: Set[Actor] ) {
    // TODO: complete constructor
  }
}