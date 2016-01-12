package wikimap.parser

import wikimap.retriever.location.FileLocationRetriever
import wikimap.util.DB
import wikimap.{Location, Event}

/**
  * Created by misha on 26/12/15.
  */
object LocationExtractor {

  private val NounGroup = "([A-Z][a-z]+ ?){2,}".r
  private val NounSingle = "([A-Z][a-z]+)".r
  private val Link = "\\[\\[([^\\[[\\]]]+)\\]\\]".r

  def run() = {
    DB.resetTables(Seq("eventLocations"))

    val start = System.currentTimeMillis()

    DB.getEvents
      .take(10)
      .foreach(event => {
        println(event.date)
        extractLocation(event) match {
          case Some(location) =>
            (location.id, event.id) match {
              case (Some(lid), Some(eid)) =>
                DB.batchEventLocation(lid, eid)
              case _ =>
                println(s"IDs not set for location and/or event:\n$event\n$location")
            }
          case None =>
        }
      })

    DB.insertAllEventLocation()

    println(s"Took: ${System.currentTimeMillis() - start}")
  }

  def extractLocationMultiple(events: Seq[Event]) =
    events.map(extractLocation)

  def extractLocation(event: Event): Option[Location] = {
    val desc = event.description
    val links = extractLinksFromText(desc)
    val linkNouns = links.flatMap(extractSingleNounsFromText)
    val nouns = extractSingleNounsFromText(desc)
    val linkNounGroups = links.flatMap(extractNounGroupsFromText)
    val nounGroups = extractNounGroupsFromText(desc)

    val possible: Seq[String] =
      links ++ linkNouns ++ nouns ++ linkNounGroups ++ nounGroups

    DB.getLocationFromNames(possible)
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
