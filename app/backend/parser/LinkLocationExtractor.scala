package backend.parser

import backend.util.DB
import play.api.Logger

/**
  * Created by misha on 26/01/16.
  */
object LinkLocationExtractor {
  private val log = Logger(getClass)

  private val linkRegex = "\\[\\[[^\\[\\]]*\\]\\]".r

  def run() = {
    DB.resetTables(Seq("wikiEventLocations"))

    // For every event...
    DB.getEvents
      .filter(_.id.nonEmpty)
      .foreach(e => {
        // Get every link...
        val coords = (linkRegex findAllIn e.description)
          .flatMap(s => {
            // Format as a link...
            val link = s.substring(2, s.length - 2)
              .split("\\|").head
              .replace(" ", "_")

            // Try to convert to coords
            DB.getLocationFromWiki(link)
          }).toSeq

        DB.batchWikiEventLocation(e.id.get, coords)

        log.debug(s"${e.description.replace("\n", "")} =>\n${coords.mkString(", ")}")
      })

    DB.insertAllWikiEventLocation()
  }
}
