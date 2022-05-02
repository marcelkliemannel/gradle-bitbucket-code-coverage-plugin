package dev.turingcomplete.bitbucketcodecoverage

import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

object SourceFilesResolver {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  /**
   * Tries to resolve the relative project root dir of the given
   * [FileCodeCoverage.sourceFile] by traversing the given [sourceFilesSearchDirs].
   *
   * @return a mapping from the original source file to
   */
  fun resolveSourceFilesRelativeToProjectRootDir(project: Project,
                                                 sourceFilesSearchDirs: FileCollection,
                                                 sourceFiles: Collection<File>): Map<File, File> {

    // Create a `PathMatcher` for each source file
    val missingSourceFiles = createPathMatchers(sourceFiles).toMutableMap()

    // Search for all `PathMatcher`'s by traversing all `sourceFilesSearchDirs`.
    val originalToResolvedSourceFile = mutableMapOf<File, File>()
    val searchDirFileVisitor = createSearchDirFileVisitor(project, missingSourceFiles, originalToResolvedSourceFile)
    sourceFilesSearchDirs.files
            .takeWhile { missingSourceFiles.isNotEmpty() }
            .filter { it.exists() }
            .filter {
              val isInsideProjectRootDir = it.startsWith(project.rootDir)
              if (!isInsideProjectRootDir) {
                project.logger.warn("Source files search directory '$it' is outside of the project root directory and will be ignored.")
              }
              isInsideProjectRootDir
            }
            .forEach { sourceFilesSearchDir ->
              project.logger.debug("Searching for source files in directory: $sourceFilesSearchDir...")
              Files.walkFileTree(sourceFilesSearchDir.toPath(), searchDirFileVisitor)
            }

    return originalToResolvedSourceFile
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createPathMatchers(sourceFiles: Collection<File>): Map<PathMatcher, File> {
    val fileSystem = FileSystems.getDefault()

    return sourceFiles.filter { !it.isAbsolute }.associateBy { sourceFile ->
      val pathMatcher = fileSystem.getPathMatcher("glob:**/$sourceFile")
      pathMatcher
    }
  }

  /**
   * Creates a visitor which removes a matching source file from
   * `missingSourceFiles` and adds the match to `originalToResolvedSourceFile`.
   */
  private fun createSearchDirFileVisitor(project: Project,
                                         missingSourceFiles: MutableMap<PathMatcher, File>,
                                         originalToResolvedSourceFile: MutableMap<File, File>): FileVisitor<Path> {

    return object : SimpleFileVisitor<Path>() {

      override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
        missingSourceFiles.asSequence().find { it.key.matches(file) }?.let {
          val sourceFile = it.value
          project.logger.debug("Resolved source file '${sourceFile}' to file: $file")
          originalToResolvedSourceFile[sourceFile] = file.toFile().relativeTo(project.rootDir)
          missingSourceFiles.remove(it.key)
        }

        return if (missingSourceFiles.isEmpty()) FileVisitResult.TERMINATE else FileVisitResult.CONTINUE
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}