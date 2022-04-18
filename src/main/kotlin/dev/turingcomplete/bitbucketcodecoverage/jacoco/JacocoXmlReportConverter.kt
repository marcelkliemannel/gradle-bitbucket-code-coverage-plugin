package dev.turingcomplete.bitbucketcodecoverage.jacoco

import dev.turingcomplete.bitbucketcodecoverage.ClassCodeCoverage
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

  fun convertReport(file: File): List<ClassCodeCoverage> {
    assert(isReportConvertible(file))

    val packageElements = mutableListOf<Element>()

    // Read packages from children of 'group' elements
    readXmlReportFile(file)
            .getElementsByTagName("group")
            .toElementSequence()
            .flatMap { it.getElementsByTagName("package").toElementSequence() }
            .forEach { packageElements.add(it) }

    // Read packages from first level 'package' elements
    readXmlReportFile(file)
            .getElementsByTagName("package").toElementSequence()
            .forEach { packageElements.add(it) }

    return packageElements.flatMap(collectPackageCoverage()).toList()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun collectPackageCoverage(): (Element) -> Sequence<ClassCodeCoverage> {
    return { packageElement ->
      val packageName = packageElement.getAttribute("name").replace("/", ".")

      packageElement.getElementsByTagName("sourcefile")
              .toElementSequence()
              .map(collectSourceFileCoverage(packageName))
    }
  }

  private fun collectSourceFileCoverage(packageName: String): (Element) -> ClassCodeCoverage {
    return { sourceFileElement ->
      val fileName = sourceFileElement.getAttribute("name")
      val bitbucketCodeCoverage = ClassCodeCoverage(packageName, fileName)

      sourceFileElement.getElementsByTagName("line")
              .toElementSequence()
              .forEach(collectLineCoverage(bitbucketCodeCoverage))

      bitbucketCodeCoverage
    }
  }

  private fun collectLineCoverage(bitbucketCodeCoverage: ClassCodeCoverage): (Element) -> Unit {
    return { lineElement ->
      val line = lineElement.getAttribute("nr").toInt()
      val missedBranches = lineElement.getAttribute("mb").toInt()
      val coveredBranches = lineElement.getAttribute("cb").toInt()

      val isSimpleStatement = missedBranches == 0 && coveredBranches == 0
      if (isSimpleStatement) {
        // Line coverage state
        val missedInstructions = lineElement.getAttribute("mi").toInt()
        if (missedInstructions == 0) {
          bitbucketCodeCoverage.addFullyCoveredLine(line)
        }
        else {
          bitbucketCodeCoverage.addUncoveredLine(line)
        }
      }
      else {
        // Block coverage state
        if (missedBranches == 0) {
          bitbucketCodeCoverage.addFullyCoveredLine(line)
        }
        else if (coveredBranches == 0) {
          bitbucketCodeCoverage.addUncoveredLine(line)
        }
        else {
          bitbucketCodeCoverage.addPartiallyCoveredLine(line)
        }
      }
    }
  }

  private fun readXmlReportFile(jacocoXmlReport: File): Document {
    val factory = DocumentBuilderFactory.newInstance().apply {
      // Disable report.dtd validation
      setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false)
      setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
    }
    return factory.newDocumentBuilder().parse(org.xml.sax.InputSource(StringReader(jacocoXmlReport.readText())))
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}

