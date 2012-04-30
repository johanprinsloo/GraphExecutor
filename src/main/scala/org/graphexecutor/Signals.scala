package org.graphexecutor

import akka.actor.ActorRef

case object signals {
  case class GoVote( source : ActorRef )
  case class RegisterDependent( actor : ActorRef )
  case class RegisterSource( actor : ActorRef )
  case class RegisterObserver( actor : ActorRef )
  case class GetDependents()
  case class GetSources()
  case class GetObservers()
  case class GetBenchmarkers()
  case class Solveable()
  case class GetSolveCount()
  case class IsDependent( n : NodeRunner )
  case class IsSource( n : NodeRunner )
  case class isObserver( n : NodeObserver )
  case class SolveStartReport( threadId : Long )
  case class SolveCompleteReport( )
  case class AddBenchMarker( actor: ActorRef )
}


object NodeStatus extends Enumeration {
  type NodeStatus = Value
  val start, complete, reset = Value
}

import NodeStatus._
case class ObserveNodeStatus( source: ActorRef, status: NodeStatus )

trait NodeObserver extends NodeRunner {
  override def statusChanged( observed: ObserveNodeStatus ) = {
    println( "observed status change to " + observed.status + " in " + observed.source + " by " + this )
  }
}

trait Observer[T] {
  def receiveUpdate( subject: T );
}

trait Observable[T] {
  this: T =>

  private var observers: List[Observer[T]] = Nil

  def addObserver( observer: Observer[T] ) = observers = observer :: observers

  def notifyObservers() = observers.foreach( _.receiveUpdate( this ) )
}


