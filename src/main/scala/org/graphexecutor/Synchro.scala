package org.graphexecutor

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}

trait Barrier {
  def await()
}

class SimpleBarrier(name: String) extends Barrier {
  val lock = new AtomicBoolean(true)
  val waiters = new AtomicInteger( 0 )

  def lockup = lock.set(true)

  def unlock = lock.set(false)

  def await() = {

    println("barrier " + name + " has " + waiters.incrementAndGet() + " waiter")
    while ( lock.get() ) {
      Thread.sleep(10)
    }
    println("barrier " + name + " released")
    waiters.set(0)
  }
}