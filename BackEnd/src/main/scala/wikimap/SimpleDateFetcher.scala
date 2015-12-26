package wikimap

import scala.util.Try

/**
  * Created by misha on 26/12/15.
  */
class SimpleDateFetcher {

  val aar = new APIArticleRetreiver

  case class SimpleEvent(date: Date, description: String)
  case class Date(date: Int, month: String, year: Int)

  val SimpleDate = "[\\s*][\\[\\[]?([0-9]*)[\\]\\]]?[\\s*]".r
  val BCDate = "\\[\\[([0-9]*) BC\\]\\]".r

  val months = Seq(
    "January", "February", "March",
    "April", "May", "June",
    "July", "August", "September",
    "October", "November", "December")

  def run: Seq[SimpleEvent] = {
    (for (
      month <- months;
      date <- 1 to 31
    ) yield  getEventsForDate(Date(date, month, 0)))
      .flatMap(es => es)
  }

  def getEventsForDate(date: Date): Seq[SimpleEvent] = {
    Try(aar.getTitle(s"${date.month}%20${date.date}")
      .split("==(Events|Births)==").toList(1)
      .split("\n\\*").toList
      .map(event => {
        event.split(" &ndash; ") match {
          case Array(SimpleDate(d), desc) => Some(SimpleEvent(Date(date.date, date.month, d.toInt),  desc))
          case Array(BCDate(d), desc)     => Some(SimpleEvent(Date(date.date, date.month, -d.toInt), desc))
          case e => println(s"Couldn't match: ${e.toList}"); None
        }
      })
      .filter(_.isDefined)
      .map(_.get))
    .getOrElse(List())
  }

}
