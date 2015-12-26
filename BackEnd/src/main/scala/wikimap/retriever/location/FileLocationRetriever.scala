package wikimap.retriever.location

import scala.io.Source
import scala.util.Try

/**
  * Created by misha on 26/12/15.
  */
object FileLocationRetriever {

  val LocationString = {
    val (id, name, coord, sep) = ("(\\d+)", "([^\t]*)", "([-\\d\\.]+)", "\t")
    (Seq(id, name, name, name, coord, coord).mkString(sep) + ".*").r
  }

  case class Coords(lat: Double, long: Double)
  case class Location(names: Seq[String], coords: Coords)

  var tsvFile: Seq[Location] = Seq()
  var locations: Map[String, Coords] = Map()

  def setup = {
    val file = Source.fromFile("res/allCountries.txt")

    tsvFile = file.getLines()
      .take(100000)
      .map({
        case LocationString(id, name, asciiName, otherNames, lat, long) =>
          Some(Location(
            (otherNames.split(",").toSeq :+ name)
              .map(_.toLowerCase)
              .map(_.replaceAll("\\W", "")),
            Coords(lat.toDouble, long.toDouble)))
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

  def getLocation(name: String): Coords =
    Try(locations(name.toLowerCase().replaceAll("\\W", ""))).getOrElse(Coords(0, 0))
}
