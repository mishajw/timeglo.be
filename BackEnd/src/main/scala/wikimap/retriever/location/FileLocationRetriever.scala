package wikimap.retriever.location

import wikimap.{Coords, Location}

import scala.io.Source

/**
  * Created by misha on 26/12/15.
  */
object FileLocationRetriever {

  val LocationString = {
//    val (id, name, coord, tab8, tab4, sep) =
//      ("(\\d*)", "([^\t]*)", "([-\\d\\.]+)", "(\t.*){8}", "(\t.*){4}", "\t")
//    (Seq(id, name, name, name, coord, coord).mkString(sep) + ".*").r
    List.fill(19)("([^\t]*)").mkString("\t").r
  }

  var tsvFile: Seq[Location] = Seq()
  var locations: Map[String, Location] = Map()

  def setup(source: String) = {
    val file = Source.fromFile(s"res/$source")

    tsvFile = file.getLines()
      .flatMap(stringToLocation)
      .toSeq

    locations = tsvFile
      .flatMap(l => l.names.map(_ -> l))
      .toMap

    file.close()
  }

  def stringToLocation(str: String) = str match {
    case LocationString(id, name, asciiName, otherNames, lat, long,
    _, _, _, _, _, _, _, _, pop, _*) =>
      Some(Location(
        (name +: otherNames.split(",").toSeq).map(strip),
//        Seq(strip(name)),
        Coords(lat.toDouble, long.toDouble),
        BigInt(pop)))
    case s =>
      println(s"Couldn't parse: $s")
      None
  }

  def getLocation(name: String): Option[Location] = {
    val stripped = strip(name)
    if (locations contains stripped) {
      Some(locations(stripped))
    } else {
      None
    }
  }

  private def strip(text: String) = text.toLowerCase().replaceAll("\\W", "")
}
