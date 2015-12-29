package wikimap.parser

import wikimap.{SimpleEvent, Date}

import scala.io.Source

/**
  * Created by misha on 27/12/15.
  */
object FileEventExtractor {

  val SimpleEventString = "SimpleEvent\\(Date\\((\\d+),([A-Za-z]+),([\\d-]+)\\),(.*)\\)".r

  def run(path: String = "out/out.txt"): Seq[SimpleEvent] = {
    val file = Source.fromFile(path)

    val events = file.getLines()
      .toList
      .flatMap(_ match {
        case SimpleEventString(date, month, year, desc) =>
          Some(SimpleEvent(Date(date.toInt, month.toInt, year.toInt), desc))
        case _ =>
          None
      })

    file.close()

    events
  }
}
