package wikimap.parser.simple

import wikimap.retriever.wikipedia.APIArticleRetreiver
import wikimap.{Date, SimpleEvent}

import scala.util.Try

/**
  * Created by misha on 26/12/15.
  */
object SimpleDateFetcher {

  val aar = new APIArticleRetreiver

  val SimpleDate = " *\\[?\\[?([0-9]*)\\]?\\]? *".r
  val BCDate = " *\\[\\[([0-9]*) ?BC\\]\\] *".r

  val months = Seq(
    "January", "February", "March",
    "April", "May", "June",
    "July", "August", "September",
    "October", "November", "December")

  def run: Seq[SimpleEvent] = {
    (for (
      month <- months.take(3);
      date <- 1 to 3
    ) yield  getEventsForDate(Date(date, month, 0)))
      .flatMap(es => es)
  }

  private def getEventsForDate(date: Date): Seq[SimpleEvent] = {
    Try(
      aar.getTitle(s"${date.month}%20${date.date}")
        .split("==(Events|Births)==").toList(1)
        .split("\n\\*").toList
        .map(_.split(" ?&ndash; ?") match {
          case Array(SimpleDate(d), desc) =>
            Some(SimpleEvent(Date(date.date, date.month, d.toInt),  desc))
          case Array(BCDate(d), desc) =>
            Some(SimpleEvent(Date(date.date, date.month, -d.toInt), desc))
          case e => None
        })
        .filter(_.isDefined)
        .map(_.get))
      .getOrElse(List())
  }

}
