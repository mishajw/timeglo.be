package wikimap.parser

import wikimap.retriever.wikipedia.APIArticleRetreiver
import wikimap.util.DB
import wikimap.{Date, Event}

import scala.util.Try

/**
  * Created by misha on 26/12/15.
  */
object APIEventExtractor {
  val SimpleDate = " *\\[?\\[?([0-9]*)\\]?\\]? *".r
  val BCDate = " *\\[\\[([0-9]*) ?BC\\]\\] *".r

  val months = Seq(
    "January", "February", "March",
    "April", "May", "June",
    "July", "August", "September",
    "October", "November", "December")

  def run: Seq[Event] = {
    DB.resetTables(Seq("events", "eventLocations"))

    (for (
      month <- months.indices;
      date <- 0 to 30
    ) yield  {
      println(s"$date, $month")
      val e = getEventsForDate(Date(date, month, 0))
      e.foreach(DB.insertEvent)
      e
    }).flatMap(es => es)
  }

  private def getEventsForDate(date: Date): Seq[Event] = {
    Try(
      APIArticleRetreiver
        .getTitle(s"${months(date.month)}%20${date.date + 1}")
        .split("==(Events|Births)==").toList(1)
        .split("\n\\*").toList
        .map(_.split(" ?&ndash; ?") match {
          case Array(SimpleDate(d), desc) =>
            Some(Event(Date(date.date, date.month, d.toInt),  desc))
          case Array(BCDate(d), desc) =>
            Some(Event(Date(date.date, date.month, -d.toInt), desc))
          case e => None
        })
        .filter(_.isDefined)
        .map(_.get))
      .getOrElse(List())
  }

}
