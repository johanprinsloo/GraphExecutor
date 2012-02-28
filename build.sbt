organization := "org.graphexecutor"

name := "GraphExecutor"

version := "0.1.0-SNAPSHOT"

libraryDependencies ++= Seq(
   "org.scalala" %% "scalala" % "1.0.0.RC3-SNAPSHOT",
   "org.scalatest" %% "scalatest" % "1.7.1" % "test"
)

resolvers ++= Seq(
  "java m2" at "http://download.java.net/maven/2",
  "Scala Tools Snapshots" at "http://scala-tools.org/repo-snapshots/",
  "ScalaNLP Maven2" at "http://repo.scalanlp.org/repo"
)

scalaVersion := "2.9.1"