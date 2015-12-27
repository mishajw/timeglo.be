package wikimap.parser.simple

import wikimap.{Location, SimpleEvent}
import wikimap.retriever.location.FileLocationRetriever

/**
  * Created by misha on 26/12/15.
  */
object SimpleLocationExtractor {

  private val nounGroup = "([A-Z][a-z]+ ?)+".r
  private val link = "\\[\\[([^\\[[\\]]]+)\\]\\]".r

  def extractLocationMultiple(events: Seq[SimpleEvent]) =
    events.map(extractLocation)

  def extractLocation(event: SimpleEvent): Option[Location] = {
    val desc = event.description
    val links = extractLinksFromText(desc)
    val linkNouns = links.flatMap(extractNounsFromText)
    val nouns = extractNounsFromText(desc)

    (links ++ linkNouns ++ nouns)
      .flatMap(FileLocationRetriever.getLocation)
      .sortBy(-_.population) match {
      case Nil => None
      case biggest :: _ => Some(biggest)
    }

//    val linkNounsLocation = linkNouns.flatMap(FileLocationRetriever.getLocation)
//    if (linkNounsLocation != Nil) return Some(linkNounsLocation.sortBy(_.population).last)
//
//    val linksLocations = links.flatMap(FileLocationRetriever.getLocation)
//    if (linksLocations != Nil) return Some(linksLocations.sortBy(_.population).last)
//
//    val nounsLocation = nouns.flatMap(FileLocationRetriever.getLocation)
//    if (nounsLocation != Nil) return Some(nounsLocation.sortBy(_.population).last)

//    None
  }

  def extractLinksFromText(text: String): Seq[String] = {
    link.findAllIn(text)
      .matchData
      .flatMap(_.subgroups)
      .toList.toSeq
      .map(_.split("\\|").last)
  }

  def extractNounsFromText(text: String): Seq[String] = {
    nounGroup.findAllIn(text).toList.toSeq
  }

  private def stripLinkDetails(eventDesc: String) =
    eventDesc.filterNot("[]".toSet)
}
