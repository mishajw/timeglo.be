package wikimap.parser.simple

import scala.util.matching.Regex.MatchIterator

/**
  * Created by misha on 26/12/15.
  */
object SimpleLocationExtractor {

  private val nounGroup = "([A-Z][a-z]+ ?)+".r
  private val link = "\\[\\[([^\\[[\\]]]+)\\]\\]".r

  def extractLocationMultiple(events: Seq[SimpleEvent]) =
    events.map(extractLocation)

  def extractLocation(event: SimpleEvent): Location = {
    LocationName("")
  }

  def extractLinksFromText(text: String): Seq[String] = {
    link.findAllIn(text)
      .matchData
      .flatMap(_.subgroups)
      .toList.toSeq
  }

  def extractNounsFromText(text: String): Seq[String] = {
    nounGroup.findAllIn(text).toList.toSeq
  }

  private def stripLinkDetails(eventDesc: String) =
    eventDesc.filterNot("[]".toSet)
}
