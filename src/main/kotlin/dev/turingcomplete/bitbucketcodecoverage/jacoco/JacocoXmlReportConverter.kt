package dev.turingcomplete.bitbucketcodecoverage.jacoco

import dev.turingcomplete.bitbucketcodecoverage.FileCodeCoverage
import dev.turingcomplete.bitbucketcodecoverage.toElementSequence
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

object JacocoXmlReportConverter {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun isReportConvertible(file: File): Boolean {
    return file.isFile && file.extension == "xml"
  }

  /**
   * Converts the given JaCoCo XML report file to a list of [FileCodeCoverage]s.
   *
   * See [report.dtd](https://github.com/jacoco/jacoco/blob/master/org.jacoco.report/src/org/jacoco/report/xml/report.dtd)
   * for the XML structure.
   */
  fun convertReport(file: File): Map<FileCodeCoverage.Builder, File> {
    assert(isReportConvertible(file))

    val reportDocument = readXmlReportFile(file)
    val packageElements = mutableListOf<Element>()

    // Read packages from children of 'group' elements
    reportDocument.getElementsByTagName("group")
            .toElementSequence()
            .flatMap { it.getElementsByTagName("package").toElementSequence() }
            .forEach { packageElements.add(it) }

    // Read packages from first level of 'package' elements
    reportDocument.getElementsByTagName("package").toElementSequence()
            .forEach { packageElements.add(it) }

    return packageElements.flatMap(collectPackageCoverage()).toMap()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun readXmlReportFile(jacocoXmlReport: File): Document {
    return DocumentBuilderFactory.newInstance().apply {
      // Disable report.dtd validation
      setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
      setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }.newDocumentBuilder().parse(org.xml.sax.InputSource(StringReader(jacocoXmlReport.readText())))
  }

  /**
   * Parses a `package` element and collects code coverage form its `sourcefile`
   * children.
   *
   * ```
   * <package name="firstPackage/secondPackage">
   *   ...
   * </package>
   * ```
   */
  private fun collectPackageCoverage(): (Element) -> Sequence<Pair<FileCodeCoverage.Builder, File>> {
    return { packageElement ->
      val packageName = packageElement.getAttribute("name")

      packageElement.getElementsByTagName("sourcefile")
              .toElementSequence()
              .map(collectSourceFileCoverage(packageName))
    }
  }

  /**
   * Parses a `sourcefile` element and collects code coverage form its `line`
   * children.
   *
   * ```
   * <sourcefile name="MyClass.java">
   *   ...
   * </sourcefile>
   * ```
   */
  private fun collectSourceFileCoverage(packageName: String): (Element) -> Pair<FileCodeCoverage.Builder, File> {
    return { sourceFileElement ->
      val fileName = sourceFileElement.getAttribute("name")
      val relativeSourceFile = if (packageName.isNotBlank()) File(packageName.replace("/", File.separator), fileName) else File(fileName)

      val bitbucketCodeCoverage = FileCodeCoverage.Builder()

      sourceFileElement.getElementsByTagName("line")
              .toElementSequence()
              .forEach(collectLineCoverage(bitbucketCodeCoverage))

      bitbucketCodeCoverage to relativeSourceFile
    }
  }

  /**
   * Parses a `line` element to collect the code coverage of this line.
   *
   * ```
   *  <line nr="37" mi="1" ci="0" mb="0" cb="0"/>
   *  <line nr="39" mi="0" ci="1" mb="0" cb="0"/>
   *  ...
   * ```
   */
  private fun collectLineCoverage(bitbucketCodeCoverage: FileCodeCoverage.Builder): (Element) -> Unit {
    return { lineElement ->
      val line = lineElement.getAttribute("nr").toInt()
      val missedBranches = lineElement.getAttribute("mb").toInt()
      val coveredBranches = lineElement.getAttribute("cb").toInt()

      val isNonBranchStatement = missedBranches == 0 && coveredBranches == 0
      if (isNonBranchStatement) {
        // Line coverage state
        when (lineElement.getAttribute("mi").toInt()) {
          0 -> bitbucketCodeCoverage.fullyCoveredLines.add(line)
          else -> bitbucketCodeCoverage.uncoveredLines.add(line)
        }
      }
      else {
        // Block coverage state
        when {
          missedBranches == 0 -> bitbucketCodeCoverage.fullyCoveredLines.add(line)
          coveredBranches == 0 -> bitbucketCodeCoverage.uncoveredLines.add(line)
          else -> bitbucketCodeCoverage.partiallyCoveredLines.add(line)
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}

