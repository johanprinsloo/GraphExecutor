import sbt._


class GraphExecutorProject(info: ProjectInfo) extends  DefaultWebProject(info) //DefaultProject(info)  with ProguardProject
{

  // repositories
  //val scalaToolsSnapshots = "Scala Tools Repository" at "http://nexus.scala-tools.org/content/repositories/snapshots/"
  val ondex = "ondex" at "http://ondex.rothamsted.bbsrc.ac.uk/nexus/content/groups/public/"
  val ScalaNLPMaven2 = "ScalaNLP Maven2" at "http://repo.scalanlp.org/repo/"
  val scalaToolsSnapshots = "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/"
  val sonatypeNexusSnapshots = "Sonatype Nexus Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  val sonatypeNexusReleases = "Sonatype Nexus Releases" at "https://oss.sonatype.org/content/repositories/releases"
  val fuseSourceSnapshots = "FuseSource Snapshot Repository" at "http://repo.fusesource.com/nexus/content/repositories/snapshots"


  //dependencies
  val scalala = "org.scalala" %% "scalala" % "1.0.0.RC2-SNAPSHOT"
  val scalatest = "org.scalatest" %% "scalatest" % "1.4.1"
  val jetty7 = "org.eclipse.jetty" % "jetty-webapp" % "7.0.2.v20100331" % "compile"
  val jetty7webSocket = "org.eclipse.jetty" % "jetty-websocket" % "7.0.2.v20100331" % "compile"
  val servlet = "javax.servlet" % "servlet-api" % "2.5" % "compile"

  //doc options
  override def docPath = "doc"
  //override def documentOptions = super.documentOptions ++ Seq(LinkSource)

  //custom task
  lazy val hi = task {println("Graph Executor - prototype actor based graph execution"); None}

  //override def mainClass = Some("nextgen.patterns.actors.Runner")
  override def mainClass: Option[String] = Some("org.graphexecutor.webui.WebUIMain")

  //proguard
  //override def proguardInJars = super.proguardInJars +++ scalaLibraryPath

  //override def proguardOptions = List(
  //proguardKeepMain("org.graphexecutor.webui.WebUIMain"))

  //tests
  override def testOptions = super.testOptions ++ Seq(TestArgument(TestFrameworks.ScalaTest, "-oD" ))
  lazy val ptest = timingTests
  def timingTests = task { runtimingtests(); None }  dependsOn(compile, test, doc) describedAs("Actor Timing Tests")

  def runtimingtests()  = {
    println("start timing tests @ " + consoleClasspath)
    runTask( Some("org.graphexecutor.Runner.main"), consoleClasspath )
    println("end timing tests")
  }

  //webrun
//  lazy val setupSysProps = task {
//    System.setProperty("replhtml.class.path", (runClasspath +++ Path.fromFile(buildScalaInstance.compilerJar) +++
//    Path.fromFile(buildScalaInstance.libraryJar)).absString)
//    None
//  } dep


  override def runAction = task { args => {
      val nargs = new Array[String](args.length+1)
      nargs.update(0, "-DDEBUG")
      Array.copy(args, 0, nargs, 1, args.length)
      super.runAction(args)//.dependsOn(setupSysProps)
    }
  }
  
}

