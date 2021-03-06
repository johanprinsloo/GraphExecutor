import scala.collection.mutable._

/*
* Test of the Actor Based execution framework -
* This should really all be auto generated from a parsed graphml file
 */
object Runner {
  def main(args: Array[String]) {

    println("ExampleTest1 NodeRunner")

    //nodes and their payloads
    val linsize = 200
    val ssize = 100
    val fl01 = NodeRunner( "FL01", SSsystem(ssize, 0.01) )
    val sp01 = NodeRunner( "SP01", LinearModel(linsize) )
    val hx01 = NodeRunner( "HX01", SSsystem(ssize, 0.01) )
    val exp1 = NodeRunner( "EXP1", LinearModel(linsize) )
    val col1 = NodeRunner( "COL1", LinearModel(linsize) )
    val hx03 = NodeRunner( "HX03", SSsystem(ssize, 0.01) )
    val pu01 = NodeRunner( "PU01", SSsystem(ssize, 0.01) )
    val mx02 = NodeRunner( "MX02", LinearModel(linsize) )
    val hx06 = NodeRunner( "HX06", LinearModel(linsize) )
    val hx02 = NodeRunner( "HX02", LinearModel(linsize) )
    val mx01 = NodeRunner( "MX01", LinearModel(linsize) )
    val cmp1 = NodeRunner( "CMP1", LinearModel(linsize) )

    //graph config - tedious and manual - we need a parser
    sp01 -> hx02
    sp01 -> hx01 -> mx01
    fl01 -> hx01 -> col1
    col1 -> pu01 -> hx06
    fl01 -> exp1 -> hx03 -> mx02 -> hx02 -> cmp1
    exp1 -> col1
    col1 -> mx02
    hx02 -> mx01

    //start execution by sending messages to the first two nodes
    sp01 ! "solve"
    fl01 ! "solve"

    //each node includes a barrier which is released after it has solved
    // we wait for the last 3 nodes to solve
    mx01.barrier.await
    hx06.barrier.await
    cmp1.barrier.await
  }
}