import com.typesafe.sbt.SbtStartScript
import sbt._
import Keys._

object BuildSettings {
  val buildOrganization = "ucalgary_students"
  val buildVersion = "0.1"
  val buildScalaVersion = "2.10.1"

  val buildSettings = Defaults.defaultSettings ++ Seq(
    organization := buildOrganization,
    version := buildVersion,
    scalaVersion := buildScalaVersion)
}

object Build extends Build {

  import BuildSettings._;
  
  lazy val project = Project(
    "cpsc441_Assignment",
	file("."),
    settings = buildSettings ++ Seq(
      libraryDependencies ++= Seq(
        "com.novocode" % "junit-interface" % "0.8" % "test->default"
      ),
      parallelExecution in Test := false
    ) ++ SbtStartScript.startScriptForClassesSettings
  )
}