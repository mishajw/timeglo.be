package backend.retriever.dbpedia

import java.net.URLEncoder

import backend._
import play.api.Logger

import scala.io.Source

import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try

object SPARQLListRetriever {

  private val log = Logger(getClass)

  def run: Seq[LocatedEvent] = {
    queries
      .map(url)
      .map(Source.fromURL(_).mkString)
      .map(parse(_))
      .flatMap(parseJson)
  }

  private def parseJson(json: JValue): List[LocatedEvent] = {

    def getValue(obj: List[(String, JValue)]): String = {
      (for { JField("value", JString(value)) <- obj } yield value).head
    }

    val rNumericDate = "(-?\\d+)-(\\d+)-(\\d+)".r
    val rYearOnly =    "(-?\\d{1,4})".r
    val rCoord =       "(-?\\d+\\.?\\d*)".r

    def parseDate(s: String): Date = s match {
      case rNumericDate(y, m, d)  => Date(d.toInt, m.toInt, y.toInt)
      case rYearOnly(y)           => Date(year = y.toInt, precision = PreciseToYear)
      case unparsed =>
        log.warn(s"Couldn't parse as date: $unparsed")
        Date(precision = NotPrecise)
    }

    def parseCoords(sLat: String, sLong: String): Coords = {
      (sLat, sLong) match {
        case (rCoord(lat), rCoord(long)) => Coords(lat.toDouble, long.toDouble)
        case unparsed =>
          log.warn(s"Couldn't parse as coordinate: $unparsed")
          Coords(0, 0)
      }
    }

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
      println(s"${getValue(date)} => ${parseDate(getValue(date))}")

      LocatedEvent(
        Event(
          parseDate(getValue(date)),
          Some(getValue(wikiPage)),
          getValue(desc)),
        Location(
          getValue(placeName),
          parseCoords(getValue(lat), getValue(long)),
          "")
      )
    }).asInstanceOf[List[LocatedEvent]]
  }

  private def url(query: String): String = {
    s"http://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=${
      URLEncoder.encode(query, "UTF-8")
    }&format=format=application%2Fsparql-results%2Bjson&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on"
  }

  private def queries: Seq[String] = {
    val max = 12000
    val increment = Math.min(max / 2, 8000)
    val plainQuery = query

    (0 until max by increment)
      .map(i => s"$plainQuery LIMIT $increment OFFSET $i")
  }

  private def query = {
    val f = Source.fromFile("conf/resources/sparql/events-query.rq")
    val q = f.mkString
    f.close() ; q
  }
}
