package backend.parser.location

import backend.util.DB
import backend.{Event, Location}
import play.api.Logger

/**
  * Created by misha on 26/12/15.
  */
object LocationExtractor {
  private val log = Logger(getClass)

  private val NounGroup = "([A-Z][a-z]+ ?){2,}".r
  private val NounSingle = "([A-Z][a-z]+)".r
  private val Link = "\\[\\[([^\\[[\\]]]+)\\]\\]".r

  private val shortWords = "is and at the to the with be of a in that have it for not on with he as".split(" ")

  def run() = {
    DB.resetTables(Seq("eventLocations"))

    DB.getEvents
      .foreach(event => {
        log.debug(s"Getting location for: ${event.date}")
        extractLocation(event) match {
          case Some(tup) =>
            log.debug("Found!")
            (tup._1.id, event.id) match {
              case (Some(lid), Some(eid)) =>
                DB.batchEventLocation(lid, eid, tup._2)
              case _ =>
                println(s"IDs not set for location and/or event:\n$event\n${tup._1}")
            }
          case None =>
            log.debug("Couldn't find.")
        }
      })

    DB.insertAllEventLocation()
  }

  def extractLocation(event: Event): Option[(Location, Int)] = {
    val desc = event.description
    val links = extractLinksFromText(desc)
    val linkNouns = links.flatMap(extractSingleNounsFromText)
    val nouns = extractSingleNounsFromText(desc)
    val linkNounGroups = links.flatMap(extractNounGroupsFromText)
    val nounGroups = extractNounGroupsFromText(desc)

    val possible: Seq[String] =
      (links ++ linkNouns ++ nouns ++ linkNounGroups ++ nounGroups)
      .map(backend.strip)
      .filter(w => !shortWords.contains(w))

    log.debug(s"${event.description} =>\n\t$possible")

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
