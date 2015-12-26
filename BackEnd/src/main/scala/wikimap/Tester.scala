package wikimap

import java.io.PrintWriter

import scala.util.Try

/**
  * Created by misha on 24/12/15.
  */
object Tester {

  case class Date(date: Int, month: String, year: Int)

  val SimpleDate = "[\\s*][\\[\\[]?([0-9]*)[\\]\\]]?[\\s*]".r
  val BCDate = "\\[\\[([0-9]*) BC\\]\\]".r

  def main(args: Array[String]): Unit = {
//    val ic = new IndexContainer
//    println(ic.getCompressedPosition("England"))

//    val ar = new ArticleRetriever
//    ar.getRange(0, 100)

    val aar = new APIArticleRetreiver

    val events: String = (for (
      month <- Seq(
        "January", "February", "March",
        "April", "May", "June",
        "July", "August", "September",
        "October", "November", "December");
      date <- 1 to 31
    ) yield {
      println(s"$month%20$date")

      Try(aar.getTitle(s"$month%20$date")
        .split("==(Events|Births)==").toList(1)
        .split("\n\\*").toList
        .map(event => {
          event.split(" &ndash; ") match {
            case Array(SimpleDate(d), desc) => Some(Date(date, month, d.toInt),  desc)
            case Array(BCDate(d), desc)     => Some(Date(date, month, -d.toInt), desc)
            case e => println(s"Couldn't match: ${e.toList}"); None
          }
        })
        .filter(_.isDefined)).getOrElse(List())
    })
    .flatMap(es => es)
    .mkString("\n")

//    new PrintWriter("out.txt") {
//      write(events)
//      close()
//    }
  }
}
