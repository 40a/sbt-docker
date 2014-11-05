import sbtdocker.immutable

name := "scripted-expand-archive"

organization := "sbtdocker"

version := "0.1.0"

dockerfile in docker := {
  val archive = file("archive.tgz")
  immutable.Dockerfile.empty
    .from("busybox")
    .add(archive, "/")
    .cmd("ls", "dir")
}

val check = taskKey[Unit]("Check")

check := {
  val process = Process("docker", Seq("run", (imageName in docker).value.toString))
  val out = process.!!
  if (out.trim != "file") sys.error("Unexpected output: " + out)
}
