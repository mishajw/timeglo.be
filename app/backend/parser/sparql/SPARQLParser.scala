package backend.parser.sparql

import backend._
import org.json4s.JsonAST.{JArray, JField, JObject, JString}
import org.json4s._
import play.api.Logger

object SPARQLParser {

  private val log = Logger(getClass)

  private val rCoord = "(-?\\d+\\.?\\d*)".r

  def parse(json: JValue): Seq[LocatedEvent] = {
    (for {
      JObject(obj) <- json
      JField("results", JObject(results)) <- obj
      JField("bindings", JArray(bindings)) <- results
      JObject(eventContainer) <- bindings
      JField("wiki_page", JObject(wikiPage)) <- eventContainer
      JField("date", JObject(date)) <- eventContainer
      JField("place_name", JObject(placeName)) <- eventContainer
      JField("long", JObject(long)) <- eventContainer
      JField("lat", JObject(lat)) <- eventContainer
      JField("desc", JObject(desc)) <- eventContainer
    } yield {
      LocatedEvent(
        Event(
          DateParser.parse(getValue(date)),
          Some(getValue(wikiPage).replaceAll("\\?oldid=.*", "")),
          getValue(desc)),
        Location(
          getValue(placeName),
          parseCoords(getValue(lat), getValue(long)),
          "")
      )
    }).asInstanceOf[List[LocatedEvent]]
  }

  private def getValue(obj: List[(String, JValue)]): String = {
    (for { JField("value", JString(value)) <- obj } yield value).head
  }

  private def parseCoords(sLat: String, sLong: String): Coords = {
    (sLat, sLong) match {
      case (rCoord(lat), rCoord(long)) => Coords(lat.toDouble, long.toDouble)
      case unparsed =>
        log.warn(s"Couldn't parse as coordinate: $unparsed")
        Coords(0, 0)
    }
  }
}
