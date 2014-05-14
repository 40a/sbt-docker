package sbtdocker

import sbt._
import scala.sys.process.{Process, ProcessLogger}
import scala.sys.error
import sbtdocker.Dockerfile.{StageDir, CopyPath}

object DockerBuilder {
  /**
   * Build a Dockerfile using a provided docker binary.
   *
   * @param dockerPath path to the docker binary
   * @param buildOptions options for the build command
   * @param imageName name of the resulting image
   * @param dockerFile Dockerfile to build
   * @param stageDir stage dir
   * @param log logger
   */
  def apply(dockerPath: String, buildOptions: BuildOptions, imageName: ImageName, dockerFile: Dockerfile, stageDir: StageDir, log: Logger): ImageId = {
    log.info(s"Creating docker image with name: '$imageName'")

    prepareFiles(dockerFile, stageDir, log)

    buildImage(dockerPath, buildOptions, imageName, stageDir, log)
  }

  def prepareFiles(dockerFile: Dockerfile, stageDir: StageDir, log: Logger) = {
    log.debug(s"Preparing stage directory '${stageDir.file.getPath}'")

    IO.delete(stageDir.file)

    IO.write(stageDir.file / "Dockerfile", dockerFile.toInstructionsString)
    copyFiles(dockerFile.pathsToCopy, stageDir, log)
  }

  def copyFiles(pathsToCopy: Seq[CopyPath], stageDir: StageDir, log: Logger) = {
    for (CopyPath(source, targetRelative) <- pathsToCopy) {
      val target = stageDir.file / targetRelative.getPath
      log.debug(s"Copying '${source.getPath}' to '${target.getPath}'")

      if (target.exists()) {
        error( s"""Path "${target.getPath}" already exists in stage directory""")
      }

      if (source.isFile) {
        IO.copyFile(source, target, preserveLastModified = true)
      } else if (source.isDirectory) {
        IO.copyDirectory(source, target, overwrite = false, preserveLastModified = true)
      }
    }
  }

  private val SuccessfullyBuilt = "Successfully built (.*)".r

  def buildImage(dockerPath: String, buildOptions: BuildOptions, imageName: ImageName, stageDir: StageDir, log: Logger): ImageId = {
    val processLog = ProcessLogger({ line =>
      log.info(line)
    }, { line =>
      log.info(line)
    })

    val flags = List(
      buildOptions.noCache.map(value => s"--no-cache=$value"),
      buildOptions.rm.map(value => s"--rm=$value"))

    val command = (dockerPath :: "build" :: "-t" :: imageName.name :: flags.flatten) :+ "."
    log.debug(s"Running command: '${command.mkString(" ")}' in '${stageDir.file.absString}'")

    val process = Process(command, stageDir.file).lines_!(processLog)
    process.foreach { line =>
      log.info(line)
    }
    process.last match {
      case SuccessfullyBuilt(id) =>
        log.info(s"Successfully built docker image: ${imageName.name}")
        ImageId(id)
      case _ =>
        error("Error when building Dockerfile")
    }
  }
}
