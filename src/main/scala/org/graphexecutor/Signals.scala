package org.graphexecutor

object NodeStatus extends Enumeration {
  type NodeStatus = Value
  val start, complete, reset = Value
}

import NodeStatus._

case class ObserveNodeStatus( source: NodeRunner, status: NodeStatus )

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

//trait Valuee[T] extends (() => T)
//
//trait Var[T] extends Valuee[T] {
//   def apply(newValue : T) : Unit
//}
//
//trait ObservableVar[T] extends Var[T] with Observable[T] {
//  def observableVar[T](v : T) : ObservableVar[T] = { v }
//  override def apply(newValue : T) : Unit = {
//     apply( newValue )
//     notifyObservers()
//  }
//}
//
//class ExModel {
//   val someField = observableVar(1.4)
//   val someOtherField = observableVar("Hello")
//}
