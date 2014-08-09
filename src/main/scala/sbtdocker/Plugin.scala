package sbtdocker

import sbt.Keys._
import sbt._

object Plugin extends sbt.Plugin {

  import sbtdocker.Plugin.DockerKeys._

  object DockerKeys {
    val docker = taskKey[ImageId]("Creates a Docker image.")
    val dockerPush = taskKey[Unit]("Pushes a already built image to the registry.")
    val dockerBuildAndPush = taskKey[ImageId]("Creates a Docker image and pushes it to the registry.")

    val dockerfile = taskKey[DockerfileLike[_]]("Definition of the Dockerfile that should be built.")
    val stageDirectory = taskKey[File]("Staging directory used when building the image.")
    val imageName = taskKey[ImageName]("Name of the built image.")
    val dockerPath = settingKey[String]("Path to the Docker binary.")
    val buildOptions = settingKey[BuildOptions]("Options for the Docker build command.")
  }

  lazy val baseDockerSettings = Seq(
    docker <<= (streams, dockerPath in docker, buildOptions in docker, stageDirectory in docker, dockerfile in docker, imageName in docker) map {
      (streams, dockerPath, buildOptions, stageDir, dockerfile, imageName) =>
        val log = streams.log
        log.debug("Using Dockerfile:")
        log.debug(dockerfile.mkString)

        DockerBuilder(dockerPath, buildOptions, imageName, dockerfile, stageDir, log)
    },
    stageDirectory in docker <<= target map (target => target / "docker"),
    imageName in docker <<= (organization, name) map {
      case ("", name) =>
        ImageName(name)
      case (organization, name) =>
        ImageName(namespace = Some(organization), repository = name)
    },
    dockerPath in docker := sys.env.get("DOCKER").filter(_.nonEmpty).getOrElse("docker"),
    buildOptions in docker := BuildOptions(),
    dockerPush <<= (streams, dockerPath in docker, imageName in docker) map {
      (streams, dockerPath, imageName) =>
        val log = streams.log

        DockerPush(dockerPath, imageName, log)
    },
    dockerBuildAndPush <<= (docker, dockerPush) { (build, push) =>
      build.doFinally(push)
    }
  )

  def packageDockerSettings(fromImage: String, exposePorts: Seq[Int]) = Seq(
    docker <<= docker.dependsOn(Keys.`package`.in(Compile, packageBin)),
    mainClass in docker <<= mainClass in docker or mainClass.in(Compile, packageBin),
    dockerfile in docker := {
      val classpath = (managedClasspath in Compile).value
      val jar = artifactPath.in(Compile, packageBin).value
      val mainclass = (mainClass in docker).value.getOrElse {
        sys.error("No main class found or multiple main classes exists. " +
          "One can be set with 'mainClass in docker := Some(\"package.MainClass\")'.")
      }

      val appPath = "/app"
      val libsPath = s"$appPath/libs"
      val jarPath = s"$appPath/${jar.name}"

      val libFiles = classpath.files.map(libFile => StageFile(libFile, libsPath))
      val classpathString = s"${libFiles.map(_.target).mkString(":")}:$jarPath"

      new Dockerfile {
        from(fromImage)

        if (exposePorts.nonEmpty) {
          expose(exposePorts: _*)
        }

        stageFiles(libFiles)
        add(libsPath, libsPath)
        add(jar, jarPath)

        entryPoint("java", "-cp", classpathString, mainclass)
      }
    }
  )

  /**
   * Sets up all basic docker settings with default values, only [[DockerKeys.dockerfile]] is undefined.
   */
  def dockerSettings: Seq[sbt.Def.Setting[_]] = baseDockerSettings

  /**
   * Sets up all docker settings and defines a simple Dockerfile. The Dockerfile will only expose the specified ports,
   * add all the runtime dependencies and the a packaged artifact of the project and set the entry point to be
   * `java -cp {classpath} {main class}`.
   * The from image defaults to 'dockerfile/java'.
   *
   * These settings will not work for all projects, use [[sbtdocker.Plugin.dockerSettings]] instead and define a
   * Dockerfile that works with your project.
   */
  def dockerSettingsAutoPackage(fromImage: String = "dockerfile/java",
                                exposePorts: Seq[Int] = Seq.empty): Seq[sbt.Def.Setting[_]] = {
    baseDockerSettings ++ packageDockerSettings(fromImage, exposePorts)
  }

}
