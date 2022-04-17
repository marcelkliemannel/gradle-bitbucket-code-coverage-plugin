package dev.turingcomplete.bitbucketcodecoverage

import java.io.File
import java.io.File.separator
import java.nio.file.Path

/**
 * Represents the code coverage of a class file.
 *
 * @property packageName the class package in the 'dot format' (e.g, `foo.bar`).
 * @property sourceFileName the name of the source file (e.g. `MyClass.java`).
 */
class ClassCodeCoverage(val packageName: String, val sourceFileName: String) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val fullyCoveredLines = mutableSetOf<Int>()
  private val partiallyCoveredLines = mutableSetOf<Int>()
  private val uncoveredLines = mutableSetOf<Int>()

  var sourceFile: Path? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun addFullyCoveredLine(line: Int) {
    fullyCoveredLines.add(line)
  }

  fun addPartiallyCoveredLine(line: Int) {
    partiallyCoveredLines.add(line)
  }

  fun addUncoveredLine(line: Int) {
    uncoveredLines.add(line)
  }

  /**
   * Gets the relative path source file which is derived from the package and
   * class name.
   */
  fun getRelativeSourceFile() : File {
    return if (packageName.isNotBlank()) {
       File(packageName.replace(".", separator), sourceFileName)
    }
    else {
      File(sourceFileName)
    }
  }

  /**
   * Creates a representation of this [ClassCodeCoverage] for the Bitbucket API.
   *
   * Example: `C:1,2,3;P:4;U:5,6`.
   */
  fun toBitbucketCodeCoverage() : String {
    return "C:${fullyCoveredLines.joinToString(",")};P:${partiallyCoveredLines.joinToString(",")};U:${uncoveredLines.joinToString(",")}"
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}