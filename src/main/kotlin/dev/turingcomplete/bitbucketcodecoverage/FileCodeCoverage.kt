package dev.turingcomplete.bitbucketcodecoverage

import java.io.File

/**
 * Represents the code coverage of a file.
 *
 * @property sourceFile the path to the source file of this code coverage which
 * must be relative to the project root dir. The [SourceFilesResolver] can be
 * used to search for the path of the file in the project root directory.
 */
class FileCodeCoverage(val sourceFile: File,
                       val fullyCoveredLines: Set<Int>,
                       val partiallyCoveredLines: Set<Int>,
                       val uncoveredLines: Set<Int>) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    assert(!sourceFile.isAbsolute)
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  /**
   * Creates a representation of this [FileCodeCoverage] for the Bitbucket API.
   *
   * Example: `C:1,2,3;P:4;U:5,6`.
   */
  fun toBitbucketCodeCoverage(): String {
    return "C:${fullyCoveredLines.joinToString(",")};P:${partiallyCoveredLines.joinToString(",")};U:${uncoveredLines.joinToString(",")}"
  }

  fun hasCoverageInfo(): Boolean {
    return fullyCoveredLines.isNotEmpty() || partiallyCoveredLines.isNotEmpty() || uncoveredLines.isNotEmpty()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Builder {

    val fullyCoveredLines = mutableSetOf<Int>()
    val partiallyCoveredLines = mutableSetOf<Int>()
    val uncoveredLines = mutableSetOf<Int>()

    fun build(sourceFile: File): FileCodeCoverage {
      return FileCodeCoverage(sourceFile, fullyCoveredLines, partiallyCoveredLines, uncoveredLines)
    }
  }
}