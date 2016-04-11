package backend.parser

import backend.Event
import backend.util.DB
import play.api.Logger

object LinkLocationExtractor {
  private val log = Logger(getClass)

  private val linkRegex = "\\[\\[[^\\[\\]]*\\]\\]".r

  def run(events: Seq[Event]): Seq[(Event, Seq[Long])] = {
    events
      .map(e => {
        // Get all the links, and try and get them from the DB
        val locationIds: Seq[Long] =
          (linkRegex findAllIn e.desc).toSeq
            .map(formatLink)
            .flatMap(DB.getLocationForLink)

        (e, locationIds)
      })
  }

  private def formatLink(link: String): String = {
    link.substring(2, link.length - 2)
      .split("\\|").head
      .replace(" ", "_")
  }
}
