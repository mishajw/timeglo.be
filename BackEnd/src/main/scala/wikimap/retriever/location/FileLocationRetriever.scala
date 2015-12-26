package wikimap.retriever.location

import scala.io.Source
import scala.util.Try

/**
  * Created by misha on 26/12/15.
  */
object FileLocationRetriever {

  case class Coords(long: Double, lat: Double)
  case class Location(names: Seq[String], coords: Coords)

  lazy val tsvFile = {
    val file = Source.fromFile("res/GB.txt")

    val values = file
      .getLines()
      .map(_.split("\t+").toList)
      .filter(_.size > 4)
      .map(spl => Location(
        Seq(spl(1), spl(2)),
        Coords(
          Try(spl(3).toDouble).getOrElse(0),
          Try(spl(4).toDouble).getOrElse(0))
      ))

    values
  }

  lazy val locations: Map[String, Coords] = {
    tsvFile
      .flatMap(l => l.names.map(_ -> l.coords))
      .toMap
  }
}
