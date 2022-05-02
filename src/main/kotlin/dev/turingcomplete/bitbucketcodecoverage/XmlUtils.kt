package dev.turingcomplete.bitbucketcodecoverage

import org.w3c.dom.Element
import org.w3c.dom.NodeList

// -- Properties ---------------------------------------------------------------------------------------------------- //
// -- Exposed Methods ----------------------------------------------------------------------------------------------- //

/**
 * Transforms the [NodeList] to a [Sequence] of [Element]s.
 */
internal fun NodeList.toElementSequence(): Sequence<Element> {
  return IntRange(0, this.length - 1).map { this.item(it) }.filterIsInstance<Element>().asSequence()
}

// -- Private Methods ----------------------------------------------------------------------------------------------- //
// -- Type ---------------------------------------------------------------------------------------------------------- //
