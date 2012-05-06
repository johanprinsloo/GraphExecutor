package org.graphexecutor

import org.graphexecutor.bench._
import org.graphexecutor.NodeStatus._
import akka.util.Timeout
import akka.util.duration._
import akka.pattern.{ask, gracefulStop}
import signals._
import akka.dispatch.{ExecutionContext, Future, Await}
import akka.actor._

object NodeControl {
  val system = ActorSystem("GraphExecutor")
  implicit val ec = ExecutionContext.defaultExecutionContext(system)
  implicit val timeout = Timeout(5 seconds)


  def stop(n : NodeRunner) = {
    Await.result( gracefulStop(n.actor, 5 seconds)(NodeControl.system), 5 seconds )
  }

  /**
   * Synchronous graceful shutdown of a list of nodes
   * @param ns : List[NodeRunner]
   * @return
   */
  def stopNodes( ns : List[NodeRunner]) = {
    val flist = ns.map( n => gracefulStop(n.actor, 5 seconds)(NodeControl.system) )
    Await.result(Future.sequence(flist), (1*ns.size) seconds)
  }

  def resetNodes( ns : List[NodeRunner]) = {
    val flist = ns.map( n => n.actor ? "reset" )
    Await.result(Future.sequence(flist), (1*ns.size) seconds)
  }
}

class Node(val config : NodeRunner, var model: Work ) extends Actor {
  var solveCount = 0
  var dependents = Set[ActorRef]()
  var observers = Set[ActorRef]()
  var benchmarkers = Set[ActorRef]()
  var sources = collection.mutable.HashMap[ActorRef, Boolean]()

  def receive = {
        case goVote: GoVote              => canGo( goVote )
        case dep : RegisterDependent     => dependents += dep.actor; sender ! true
        case src : RegisterSource        => sources += ( src.actor -> false ); sender ! true
        case bnc : AddBenchMarker        => benchmarkers += bnc.actor; sender ! true
        case obs : RegisterObserver      => observers += obs.actor; sender ! true
        case observed: ObserveNodeStatus => statusChanged( observed ); config.statusChanged( observed )
        case "solve"                     => solve; sender ! { config.name + " solved" }
        case "reset"                     => reset; sender ! { config.name + " reset" }
        case m : Solveable               => sender ! isNodeSolveable
        case m : GetSolveCount           => sender ! solveCount
        case m : GetDependents           => sender ! dependents
        case m : GetSources              => sender ! getSourceKeys
        case m : GetObservers            => sender ! observers
        case m : GetBenchmarkers         => sender ! benchmarkers
        case m : IsDependent             => sender ! dependents.contains( m.n.actor )
        case m : IsSource                => sender ! sources.keySet.contains( m.n.actor )
        case m : isObserver              => sender ! observers.contains( m.n.actor )
        case m                           => println( "unknown message " + m + " to " + config.name )
  }

  def canGo( goVote: GoVote ) = {

    sources( goVote.source ) = true

    if ( sources.values.foldLeft( true )( _ && _ ) ) {
      println( config.name + " OK to execute.. ")
      solve
    } else {
      println( config.name + " reference count upvote status: "
        + (for( v <- sources.values if v == true ) yield v).size   + " / " + sources.size + " upvotes" )
    }
  }

  def getSourceKeys : scala.collection.immutable.Set[ActorRef] = {
    val keys: scala.collection.immutable.Set[ActorRef] = sources.keySet.toSet
    println( self.path.name + " sources keyset:  " + keys )
    keys
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
    observers foreach { observer => observer ! new ObserveNodeStatus( self, status ) }
  }

  def statusChanged( observed: ObserveNodeStatus ) = {
    println("observed status change to " + observed.status + " in " + observed.source + " by " + this)
  }

  def reset = {
    for ( source <- sources ) sources( source._1 ) = false
    solveCount = 0
    notifyObservers( NodeStatus.reset )
  }
}




class NodeRunner( val name: String , var model : Work = new NoopWork() ) {

  import NodeControl._
  implicit val ec = ExecutionContext.defaultExecutionContext(system)
  val actor = system.actorOf( Props( new Node(this, model) ), name = name )

  override def toString() = name

  def isSolvable : Boolean = {
    val fu = actor ? Solveable()
    Await.result( fu, 5 seconds ).asInstanceOf[Boolean]
  }

  def ->( that: NodeRunner ): NodeRunner = {
    that.registerSource(this)
    this.registerDependent(that)
    println("linked " + this.name + " to " + that.name)
    return that
  }

  /**
   * TODO remove bleed
   * @param that
   * @return
   */
  def ->( that: GraphRunner[GxNode,GxLink,ShadowGraph] ): NodeRunner = {
    that.registerSource(this)
    this.registerDependent(that.snode)
    println("linked(g) " + this.name + " to " + that.snode.name)
    return that
  }

  def registerSource( node : NodeRunner ) = {
    val f = actor ? RegisterSource( node.actor )
    Await.result(f, 1 second)
  }

  def registerDependent( node : NodeRunner ) = {
    val f = actor ? RegisterDependent( node.actor )
    Await.result(f, 1 second)
  }

  /**
   * TODO test and debug
   * @param thats
   * @return
   */
  def ->( thats: Set[NodeRunner] ): NodeRunner = {
    thats foreach { that =>
      val f = for {
        a <-  that.actor ? RegisterSource( this.actor )
        b <-  actor ? RegisterDependent( that.actor )
      } yield (a,b)
      Await.result(f, 1 second)
    }
    println("linked " + this.name + " to " + thats)
    return this
  }

  def observe( observee: NodeRunner ) = {
    val f = observee.actor ? RegisterObserver( this.actor )
    Await.result(f, 1 second)
  }

  def ~>>( observee: NodeRunner ): NodeRunner = {
    val f = observee.actor ? RegisterObserver( this.actor )
    Await.result(f, 1 second)
    return this
  }

  def addSource( source: NodeRunner ) = {
    val f = actor ? RegisterSource( source.actor )
    Await.result(f, 1 second)
  }

  def addSources( ss: Set[NodeRunner] ) = {
    val flist = ss.map( nd => actor ? RegisterSource( nd.actor ) )  //List[Future[Boolean]]
    val futureList = Future.sequence(flist)                         //Future[List[Boolean]]
    Await.result(futureList, 1 second)
  }

  def addDependent( dep: NodeRunner ) = {
    Await.result((actor ? RegisterDependent( dep.actor ) ), 1 second)
  }

  def addDependents( ss: Set[NodeRunner] ) = {
    val flist = ss.map( n => actor ? RegisterDependent( n.actor ) )
    Await.result(Future.sequence(flist), 1 second)
  }

  def dependents : Set[ActorRef] = {
    val f = ask( actor, GetDependents() ).mapTo[Set[ActorRef]]
    return Await.result(f, 5 seconds)
  }

  def sources : Set[ActorRef] = {
    val f = ask( actor, GetSources() ).mapTo[Set[ActorRef]]
    Await.result(f, 5 seconds)
  }

  def hasDependent( n : NodeRunner ) : Boolean = {
    Await.result( (actor ? IsDependent(n) ), 1 second ).asInstanceOf[Boolean]
  }

  def hasDependents( ln : List[NodeRunner] ) : Boolean = {
    val lf = ln.map( n => ask( actor, IsDependent(n) ).mapTo[Boolean] )   // list of future booleans
    val fl = Future.sequence(lf)                                          // future list of booleans
    val rl = Await.result( fl , 5 seconds )                               // list of present booleans
    rl.foldLeft(true)( _ && _ )
  }

  def observers : Set[ActorRef] = {
    val f = actor ? GetObservers()
    Await.result(f, 1 second).asInstanceOf[Set[ActorRef]]
  }

  def hasObserver( n : NodeObserver ) : Boolean = {
    Await.result((actor ? isObserver(n) ), 1 second ).asInstanceOf[Boolean]
  }

  def hasSource( n : NodeRunner ) : Boolean = Await.result( (actor ? IsSource(n) ), 1 second).asInstanceOf[Boolean]

  def hasSources( ln : List[NodeRunner] ) : Boolean = {
    val fl = ln.map( n => ask( actor, IsSource(n) ).mapTo[Boolean] )
    val rl = Await.result( Future.sequence(fl) , 1 second )
    rl.foldLeft(true)( _ && _ )
  }

  def getSolveCount : Int = Await.result(( actor ? GetSolveCount() ), 1 second).asInstanceOf[Int]

  def statusChanged( observed: ObserveNodeStatus ) = {
    println( "observed status change : " + observed.status + " in " + name )
  }

  def solveSync() = {
    Await.result(( actor ? "solve" ), 1 second)
  }

  def solveAsync() = {
    actor ! "solve"
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

  def reset() = {
    val flist = actor ? "reset"
    Await.result(flist, 1 seconds)
  }


  def unary_~ = {
    stopActor
  }

  def ~ (node : NodeRunner) : NodeRunner = {
    ~node
    this
  }

  def stopActor() : Unit = {
    val stopped: Future[Boolean] = gracefulStop(actor, 5 seconds)(system)
    Await.result(stopped, 6 seconds)
  }

}

/**
 * Companion Object:
 * Cheap simple singleton builder/factory for NodeRunner
 */
object NodeRunner {

  import NodeControl._

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

    val bnc = NodeControl.system.actorOf(Props[BenchMarker], name = aname+"_benchmarker")
    Await.result( (nr.actor ? AddBenchMarker( bnc )), 5 seconds)
    nr
  }

  def apply( name: String, sources: Set[Actor], dependents: Set[Actor] ) {
    // TODO: complete constructor
  }
}