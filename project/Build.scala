import sbt._
import Keys._

object Build extends sbt.Build {
  import Dependencies._
  import MavenSpecifics._

  lazy val myProject = Project("GraphExecutor", file("."))
    .settings(
      organization      := "com.gridx",
      version           := "1.0-SNAPSHOT",
      scalaVersion      := "2.9.1",
      scalacOptions     := Seq("-deprecation", "-encoding", "utf8"),
      javaOptions       := Seq("-Xms512M", "-Xmx4096M"),
      initialCommands   := Console.akkaImports,
      pomExtra          := addToPom,
      parallelExecution := false,
      resolvers           ++= Dependencies.resolutionRepos,
      libraryDependencies ++= Seq(
        Compile.actor,
        Compile.io_core,
        Compile.io_file,
        Compile.scalala,
        Test.scalatest,
        Test.junit,
        Container.slf4j_api,
        Container.slf4j_imp,
        Container.slf4s
      )
    )
}

object Dependencies {
  val resolutionRepos = Seq(
    ScalaToolsSnapshots,
    "Typesafe repo" at "http://repo.typesafe.com/typesafe/releases/",
    "Typesafe snap" at "http://repo.typesafe.com/typesafe/snapshots/",
    "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
    "ScalaNLP Maven2" at "http://repo.scalanlp.org/repo"
  )

  object V {
    val io         = "0.4.0"
    val scalala    = "1.0.0.RC2-SNAPSHOT"
    val akka       = "2.0.1"
    val scalatest  = "1.7.1"
    val junit      = "4.8.1"
    val slf4j      = "1.6.4"
    val slf4s      = "1.0.7"
  }

  object Compile {
    val io_core    = "com.github.scala-incubator.io" %% "scala-io-core"  % V.io        % "compile"
    val io_file    = "com.github.scala-incubator.io" %% "scala-io-file"  % V.io        % "compile"
    val scalala    = "org.scalala"                   %% "scalala"        % V.scalala   % "compile"
    val actor      = "com.typesafe.akka"             % "akka-actor"      % V.akka      % "compile"
  }

  object Test {
    val scalatest      = "org.scalatest"  %% "scalatest"     % V.scalatest  % "test"
    val junit          = "junit"           % "junit"         % V.junit      % "test"
  }

  object Container {
    val slf4j_api   = "org.slf4j"               %  "slf4j-api"       % V.slf4j
    val slf4j_imp   = "org.slf4j"               %  "slf4j-log4j12"   % V.slf4j
    val slf4s       = "com.weiglewilczek.slf4s" %% "slf4s"           % V.slf4s
  }
}

object MavenSpecifics {
  val addToPom = (
    <build>
      <sourceDirectory>src/main/scala</sourceDirectory>
      <testSourceDirectory>src/test/scala</testSourceDirectory>
      <plugins>
            <plugin>
                <groupId>org.scala-tools</groupId>
                <artifactId>maven-scala-plugin</artifactId>
                <executions>
                    <execution>
                        <id>scala-compile-first</id>
                        <phase>process-resources</phase>
                        <goals>
                            <goal>add-source</goal>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                    <execution>
                        <goals>
                            <goal>testCompile</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    <jvmArgs>
                        <jvmArg>-Xms64m</jvmArg>
                        <jvmArg>-Xmx1024m</jvmArg>
                    </jvmArgs>
                </configuration>
            </plugin>
        </plugins>
      </build>
    )
}

object Console {
  val akkaImports = {
     """
      import akka.actor.{Props, ActorSystem, ActorRef, Actor}
      import akka.dispatch.{ ExecutionContext, Promise }
      import akka.dispatch.Await
      import akka.pattern.ask
      import akka.util.Timeout
      import akka.util.duration._
      implicit val timeout = Timeout(5 seconds)
      val system = ActorSystem("GraphExecutorConsole")
    """
  }
}