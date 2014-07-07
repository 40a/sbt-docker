name := "scripted-simple"

organization := "sbtdocker"

version := "0.1.0"

// Make docker depend on the package task, which generates a jar file of the application code
docker <<= docker.dependsOn(Keys.`package` in(Compile, packageBin))

// Define a Dockerfile
dockerfile in docker <<= (artifactPath.in(Compile, packageBin), managedClasspath in Compile, mainClass.in(Compile, packageBin)) map {
  case (jarFile, classpath, Some(mainClass)) =>
    new Dockerfile {
      // Base image
      from("dockerfile/java")
      // Add all files on the classpath
      val files = classpath.files.map { file =>
        val target = "/app/" + file.getName
        add(file, target)
        target
      }
      // Add the generated JAR file
      val jarTarget = s"/app/${jarFile.getName}"
      add(jarFile, jarTarget)
	    // Make a colon separated classpath with the JAR file
      val classpathString = files.mkString(":") + ":" + jarTarget
      // On launch run Java with the classpath and the found main class
      entryPoint("java", "-cp", classpathString, mainClass)
    }
  case (_, _, None) =>
    sys.error("Expected exactly one main class")
}

// Set a custom image name
imageName in docker := {
  ImageName(namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value))
}

val check = taskKey[Unit]("Check")

check := {
  val process = Process("docker", Seq("run", (imageName in docker).value.name))
  val out = process.!!
  if (out.trim != "Hello World") sys.error("Unexpected output: " + out)
}
