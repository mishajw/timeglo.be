package wikimap.retriever.location

import scala.io.Source

/**
  * Created by misha on 26/12/15.
  */
object FileLocationRetriever {

  val LocationString = {
    val (id, name, coord, sep) = ("(\\d+)", "([^\t]*)", "([-\\d\\.]+)", "\t")
    (Seq(id, name, name, name, coord, coord).mkString(sep) + ".*").r
  }

  case class Coords(long: Double, lat: Double)
  case class Location(names: Seq[String], coords: Coords)

  var tsvFile: Seq[Location] = Seq()
  var locations: Map[String, Coords] = Map()

  def setup = {
    val file = Source.fromFile("res/allCountries.txt")

    tsvFile = file.getLines()
//      .take(1000)
      .map({
        case LocationString(id, name, asciiName, otherNames, long, lat) =>
          Some(Location(otherNames.split(",").toSeq :+ name, Coords(long.toDouble, lat.toDouble)))
        case s =>
          println(s"Couldn't parse: $s")
          None
      })
      .flatten
      .toSeq

    locations = tsvFile
      .flatMap(l => l.names.map(_ -> l.coords))
      .toMap

    file.close()
  }
}
