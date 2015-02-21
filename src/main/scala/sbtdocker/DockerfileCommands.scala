package sbtdocker

import sbt._
import sbtdocker.Instructions._

trait DockerfileLike extends DockerfileCommands {
  type T <: DockerfileLike

  def instructions: Seq[Instruction]
}

trait DockerfileCommands {
  type T <: DockerfileCommands

  def addInstruction(instruction: Instruction): T

  def addInstructions(instructions: TraversableOnce[Instruction]): T

  @deprecated("Use stageFile instead.", "0.4.0")
  def copyToStageDir(source: File, targetRelativeToStageDir: File): T = stageFile(source, targetRelativeToStageDir)

  /**
   * Stage a file. The file will be copied to the stage directory when the Dockerfile is built.
   *
   * The `target` file must be unique for this Dockerfile. Otherwise later staged files will overwrite previous
   * files on the same target.
   *
   *@param source File to copy into stage dir.
   * @param target Path to copy file to, should be relative to the stage dir.
   */
  def stageFile(source: File, target: File): T = {
    addInstruction(Instructions.StageFile(CopyFile(source), target.getPath))
  }

  /**
   * Stage a file. The file will be copied to the stage directory when the Dockerfile is built.
   *
   * If the `target` ends with / then the source filename will be added at the end.
   *
   * The `target` file must be unique for this Dockerfile. Otherwise later staged files will overwrite previous
   * files on the same target.
   *
   * @param source File to copy into stage dir.
   * @param target Path to copy file to, should be relative to the stage dir.
   */
  def stageFile(source: File, target: String): T = {
    addInstruction(Instructions.StageFile(CopyFile(source), target))
  }

  def stageFiles(sources: Seq[File], target: String): T = {
    addInstruction(Instructions.StageFile(sources.map(CopyFile), target))
  }

  /**
   * Stage multiple files.
   */
  def stageFiles(files: TraversableOnce[StageFile]): T = {
    addInstructions(files)
  }

  // Instructions

  def from(image: String): T = addInstruction(From(image))

  def from(image: ImageName): T = addInstruction(From(image.toString))

  def maintainer(name: String): T = addInstruction(Maintainer(name))

  def maintainer(name: String, email: String): T = addInstruction(Maintainer(s"$name <$email>"))

  def run(args: String*): T = addInstruction(Instructions.Run.exec(args))

  def runShell(args: String*): T = addInstruction(Instructions.Run.shell(args))

  def runRaw(command: String): T = addInstruction(Instructions.Run(command))

  def cmd(args: String*): T = addInstruction(Cmd.exec(args))

  def cmdShell(args: String*): T = addInstruction(Cmd.shell(args))

  def cmdRaw(command: String): T = addInstruction(Cmd(command))

  def expose(ports: Int*): T = addInstruction(Expose(ports))

  def env(key: String, value: String): T = addInstruction(Env(key, value))

  def add(source: File, destination: String): T = addInstruction(Add(CopyFile(source), destination))

  def add(sources: Seq[File], destination: String): T = addInstruction(Add(sources.map(CopyFile), destination))

  def add(source: File, destination: File): T = addInstruction(Add(CopyFile(source), destination.getPath))

  @deprecated("Use addRaw instead.", "todo")
  def add(source: URL, destination: String): T = addRaw(source, destination)

  @deprecated("Use addRaw instead.", "todo")
  def add(source: URL, destination: File): T = addRaw(source, destination)

  @deprecated("Use addRaw instead.", "todo")
  def add(source: String, destination: String): T = addRaw(source, destination)

  @deprecated("Use addRaw instead.", "todo")
  def add(source: String, destination: File): T = addRaw(source, destination)

  def addRaw(source: URL, destination: String): T = addInstruction(AddRaw(source.toString, destination))

  def addRaw(source: URL, destination: File): T = addInstruction(AddRaw(source.toString, destination.toString))

  def addRaw(source: String, destination: String): T = addInstruction(AddRaw(source, destination))

  def addRaw(source: String, destination: File): T = addInstruction(AddRaw(source, destination.toString))

  def copy(source: File, destination: String): T = addInstruction(Copy(CopyFile(source), destination))
  
  def copy(sources: Seq[File], destination: String): T = addInstruction(Copy(sources.map(CopyFile), destination))

  def copy(source: File, destination: File): T = addInstruction(Copy(CopyFile(source), destination.toString))

  @deprecated("Use copyRaw instead.", "todo")
  def copy(source: URL, destination: String): T = copyRaw(source, destination)

  @deprecated("Use copyRaw instead.", "todo")
  def copy(source: URL, destination: File): T = copyRaw(source, destination)

  @deprecated("Use copyRaw instead.", "todo")
  def copy(source: String, destination: String): T = copyRaw(source, destination)

  @deprecated("Use copyRaw instead.", "todo")
  def copy(source: String, destination: File): T = copyRaw(source, destination)

  def copyRaw(source: URL, destination: String): T = addInstruction(CopyRaw(source.toString, destination))

  def copyRaw(source: URL, destination: File): T = addInstruction(CopyRaw(source.toString, destination.toString))

  def copyRaw(source: String, destination: String): T = addInstruction(CopyRaw(source, destination))

  def copyRaw(source: String, destination: File): T = addInstruction(CopyRaw(source, destination.toString))

  def entryPoint(args: String*): T = addInstruction(EntryPoint.exec(args))

  def entryPointShell(args: String*): T = addInstruction(EntryPoint.shell(args))
  
  def entryPointRaw(command: String): T = addInstruction(EntryPoint(command))

  def volume(mountPoints: String*): T = addInstruction(Volume(mountPoints))

  def user(username: String): T = addInstruction(User(username))

  def workDir(path: String): T = addInstruction(WorkDir(path))

  def onBuild(instruction: DockerfileInstruction): T = addInstruction(Instructions.OnBuild(instruction))

}
