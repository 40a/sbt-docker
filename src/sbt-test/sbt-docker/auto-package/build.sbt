import DockerKeys._

name := "scripted-auto-package"

organization := "sbtdocker"

version := "0.1.0"

scalaVersion := "2.11.5"

libraryDependencies += "joda-time" % "joda-time" % "2.7"

dockerSettingsAutoPackage()

val check = taskKey[Unit]("Check")

check := {
  val process = Process("docker", Seq("run", (imageName in docker).value.toString))
  val out = process.!!
  if (out.trim != "Hello AutoPackage\n20") sys.error("Unexpected output: " + out)
}
