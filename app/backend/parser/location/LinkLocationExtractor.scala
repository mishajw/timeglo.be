package backend.parser.location

import java.sql.DriverManager

import backend.{Coords, SimpleLocation}
import backend.util.DB
import play.api.Logger

import scala.io.Source

/**
  * Created by misha on 26/01/16.
  */
object LinkLocationExtractor {
  private val log = Logger(getClass)

  private val linkRegex = "\\[\\[[^\\[\\]]*\\]\\]".r

  def run() = {
    // For every event...
    DB.getEvents
      .foreach(e => {
        // Get every link...
        val coords = (linkRegex findAllIn e.description)
          // Format as a link...
          .map(s => s
            .substring(2, s.length - 2)
            .split("\\|").head)
          .flatMap(e => Seq(
            e.replace(" ", "_"),
            e.replace(" ", "")))
          // Try to convert to coords
          .flatMap(DB.getLocationFromWiki)

        log.info(s"${e.description.replace("\n", "")} =>\n${coords.mkString(", ")}")
      })
  }
}
