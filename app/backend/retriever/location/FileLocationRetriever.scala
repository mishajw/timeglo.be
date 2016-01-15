package backend.retriever.location

import backend.util.DB
import backend.{Coords, Location}
import play.api.Logger

import scala.io.Source

/**
  * Created by misha on 26/12/15.
  */
object FileLocationRetriever {
  private val log = Logger(getClass)

  val LocationString =
    List.fill(19)("([^\t]*)").mkString("\t").r

  def setup(source: String) = {
    DB.resetTables(Seq("locations", "locationNames"))

    val file = Source.fromFile(s"res/$source")

    file.getLines()
      .zipWithIndex
      .foreach(tup => {
        if ((tup._2 & 4095) == 0) log.debug(s"On index: ${tup._2}")
        stringToLocation(tup._1) match {
          case Some(l) =>
            DB.insertLocationWithID(l, tup._2)
          case None =>
        }
      })

    file.close()
  }

  def stringToLocation(str: String) = str match {
    case LocationString(id, name, asciiName, otherNames, lat, long,
    _, _, _, _, _, _, _, _, pop, _*) =>
      Some(Location(
        name,
        otherNames
          .split(",")
          .toSeq
          .filter(_.nonEmpty)
          .map(backend.strip),
        Coords(lat.toDouble, long.toDouble),
        new java.math.BigDecimal(pop)))
    case s =>
      println(s"Couldn't parse: $s")
      None
  }
}
