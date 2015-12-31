package wikimap.retriever.location

import wikimap.util.DB
import wikimap.{Coords, Location}

import scala.io.Source

/**
  * Created by misha on 26/12/15.
  */
object FileLocationRetriever {

  val LocationString =
    List.fill(19)("([^\t]*)").mkString("\t").r

  def setup(source: String) = {
    DB.resetTables(Seq("locations", "locationNames"))

    val file = Source.fromFile(s"res/$source")

    file.getLines()
      .foreach(stringToLocation(_) match {
        case Some(l) => DB.insertLocation(l)
        case None =>
      })

    file.close()
  }

  def stringToLocation(str: String) = str match {
    case LocationString(id, name, asciiName, otherNames, lat, long,
    _, _, _, _, _, _, _, _, pop, _*) =>
      Some(Location(
        (name +: otherNames.split(",").toSeq).filter(_ != "").map(wikimap.strip),
        Coords(lat.toDouble, long.toDouble),
        pop.toInt))
    case s =>
      println(s"Couldn't parse: $s")
      None
  }
}
