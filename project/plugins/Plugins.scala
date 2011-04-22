import sbt._
class Plugins(info: ProjectInfo) extends PluginDefinition(info) {
  val proguard = "org.scala-tools.sbt" % "sbt-proguard-plugin" % "0.0.+"
//  val databinder_repo = "Databinder Repository" at "http://databinder.net/repo"
//  val spde_sbt = "us.technically.spde" % "spde-sbt-plugin" % "0.4.2"
}