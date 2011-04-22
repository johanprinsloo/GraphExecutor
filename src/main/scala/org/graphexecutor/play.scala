package org.graphexecutor

import scala.collection.mutable._

object play {
  def main(args: Array[String]) {
    val n1 = NodeRunner("n1", new LinearModel(300))
    val n2 = NodeRunner("n2", new LinearModel(300))
    n1 -> n2
    val g1 = GraphRunner("g1", Set(n1, n2))
    g1.setStartNodes(Set(n1))
    g1.setEndNodes(Set(n2))
    g1 ! "bb"
    g1 ! "cc"
    g1 !? "run"
  }
}