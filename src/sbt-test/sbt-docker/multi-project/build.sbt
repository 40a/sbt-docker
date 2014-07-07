organization in ThisBuild := "sbtdocker"

lazy val alfa = project.in(file("alfa"))
  .settings(name := "scripted-multi-project-alfa")
  .settings(dockerfile in docker := new Dockerfile {
    from("ubuntu:14.04")
    entryPoint("echo", "alfa")
})

lazy val bravo = project.in(file("bravo"))
  .settings(name := "scripted-multi-project-bravo")
  .settings(dockerfile in docker := new Dockerfile {
    from("ubuntu:14.04")
    entryPoint("echo", "bravo")
})

def checkImage(imageName: ImageName, expectedOut: String) {
  val process = Process("docker", Seq("run", imageName.name))
  val out = process.!!
  if (out.trim != expectedOut) sys.error(s"Unexpected output (${imageName.name}): $out")
}

val check = taskKey[Unit]("Check")

check := {
  checkImage((imageName in docker in alfa).value, "alfa")
  checkImage((imageName in docker in bravo).value, "bravo")
}
