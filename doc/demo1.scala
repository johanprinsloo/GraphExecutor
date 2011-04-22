package nextgen.test

import scala.actors._

case class GoVote()

class Model {
  def solve = {}
}

class NodeRunner(val name: String) extends Actor
{
  var dependents: Set[Actor] = Set()
  var sources: HashMap[Actor, Boolean] = HashMap()
  var model: Model = new Model()
  val barrier = new SimpleBarrier(name)
  
  def -> (that: NodeRunner): NodeRunner = {
    that.sources += (this -> false)
    this.dependents += that
    return this
  }

  def act = {
    println("GE acting activated")
    loop {
      react {
        case goVote: GoVote => canGo(goVote)
        case "solve" => solve; reply { true }
        case "exit" => {println(name+" stopping"); exit()}
        case _ => {println("unknown message")}
      }
    }
  }

  def canGo(goVote: GoVote) = {

    sources(goVote.source) = true

    if (sources.values.foldLeft(true)(_ && _)) {
      println(name + " OK to execute..")
      solve
    }
    else
    {
      println(name + " reference count upvote")
    }
  }

  def solve = {
    model.solve
    dependents foreach {
      dependent =>
        println(name + " kicking " + dependent)
        dependent ! new GoVote(this)
    }
    barrier.unlock
  }
}