package backend.parser

import backend.retriever.wikipedia.APIArticleRetriever
import backend.util.DB
import backend.{Date, Event}
import play.api.Logger

import scala.collection.immutable.IndexedSeq
import scala.util.Try

/**
  * Created by misha on 26/12/15.
  */
object APIEventExtractor {
  private val log = Logger(getClass)

  val SimpleDate = " *\\[?\\[?([0-9]*)\\]?\\]? *".r
  val BCDate = " *\\[\\[([0-9]*) ?BC\\]\\] *".r

  val months = Seq(
    "January", "February", "March",
    "April", "May", "June",
    "July", "August", "September",
    "October", "November", "December")

  def run: Seq[Event] = {
    (for (month <- 1 to 1; date <- 1 to 31) yield {
      getEventsForDate(date, month)
    }).flatten
  }

  private def getEventsForDate(date: Int, month: Int): Seq[Event] = {
    APIArticleRetriever
      .getTitle(s"${months(month - 1)}%20$date")
      .split("==(Events|Births)==").toList(1)
      .split("\n\\*").toList
        .map(_.split(" ?&ndash; ?"))
        .collect {
          case Array(SimpleDate(d), desc) =>
            Event(Date(date, month, d.toInt), None, desc)
          case Array(BCDate(d), desc) =>
            Event(Date(date, month, -d.toInt), None, desc)
        }
  }
}
