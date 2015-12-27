package wikimap.parser

import wikimap.retriever.location.FileLocationRetriever
import wikimap.{Location, SimpleEvent}

/**
  * Created by misha on 26/12/15.
  */
object LocationExtractor {

  private val NounGroup = "([A-Z][a-z]+ ?){2,}".r
  private val NounSingle = "([A-Z][a-z]+)".r
  private val Link = "\\[\\[([^\\[[\\]]]+)\\]\\]".r

  def extractLocationMultiple(events: Seq[SimpleEvent]) =
    events.map(extractLocation)

  def extractLocation(event: SimpleEvent): Option[Location] = {
    val desc = event.description
    val links = extractLinksFromText(desc)
    val linkNouns = links.flatMap(extractSingleNounsFromText)
    val nouns = extractSingleNounsFromText(desc)
    val linkNounGroups = links.flatMap(extractNounGroupsFromText)
    val nounGroups = extractNounGroupsFromText(desc)

    val possible: Seq[String] =
      links ++ linkNouns ++ nouns ++ linkNounGroups ++ nounGroups

    println(possible)

    possible
      .flatMap(FileLocationRetriever.getLocation)
      .sortBy(-_.population) match {
      case Nil => None
      case biggest :: _ => Some(biggest)
    }
  }

  def extractLinksFromText(text: String): Seq[String] = {
    Link.findAllIn(text)
      .matchData
      .flatMap(_.subgroups)
      .toList.toSeq
      .map(_.split("\\|").last)
  }

  def extractNounGroupsFromText(text: String): Seq[String] = {
    NounGroup.findAllIn(text).toList.toSeq
  }

  def extractSingleNounsFromText(text: String): Seq[String] = {
    NounSingle.findAllIn(text).toList.toSeq
  }

  private def stripLinkDetails(eventDesc: String) =
    eventDesc.filterNot("[]".toSet)
}
