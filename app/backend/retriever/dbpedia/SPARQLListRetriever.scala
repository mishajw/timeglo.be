package backend.retriever.dbpedia

import java.net.URLEncoder

import backend._
import play.api.Logger

import scala.io.Source

import org.json4s._
import org.json4s.native.JsonMethods._

object SPARQLListRetriever {

  private val log = Logger(getClass)

  def run: Seq[LocatedEvent] = {
    val raw = getRaw

    val json = parse(raw)

    parseJson(json)
  }

  private def parseJson(json: JValue): List[LocatedEvent] = {

    def getValue(obj: List[(String, JValue)]): String = {
      (for { JField("value", JString(value)) <- obj } yield value).head
    }

    val rNumericDate = "(-?\\d+)-(\\d+)-(\\d+)".r
    val rYearOnly =    "(-?\\d+{1,4})".r

    def parseDate(s: String): Date = s match {
      case rNumericDate(y, m, d)  => Date(d.toInt, m.toInt, y.toInt)
      case rYearOnly(y)           => Date(year = y.toInt, precision = PreciseToYear)
      case unparsed =>
        log.warn(s"Couldn't parse as date: $unparsed")
        Date(precision = NotPrecise)
    }

    (for {
      JObject(obj) <- json
      JField("results", JObject(results)) <- obj
      JField("bindings", JArray(bindings)) <- results
      JObject(eventContainer) <- bindings
      JField("date", JObject(date)) <- eventContainer
      JField("place_name", JObject(placeName)) <- eventContainer
      JField("long", JObject(long)) <- eventContainer
      JField("lat", JObject(lat)) <- eventContainer
      JField("desc", JObject(desc)) <- eventContainer
    } yield {
      LocatedEvent(
        Event(
          parseDate(getValue(date)),
          getValue(desc)),
        Location(
          getValue(placeName),
          Coords(getValue(lat).toDouble, getValue(long).toDouble),
          "")
      )
    }).asInstanceOf[List[LocatedEvent]]
  }

  private def getRaw: String = {
    Source.fromURL(url(query)).mkString
  }

  private def url(query: String): String = {
    s"http://dbpedia.org/sparql?default-graph-uri=http%3A%2F%2Fdbpedia.org&query=${
      URLEncoder.encode(query)
    }&format=format=application%2Fsparql-results%2Bjson&CXML_redir_for_subjs=121&CXML_redir_for_hrefs=&timeout=30000&debug=on"
  }

  private def query = {
    val f = Source.fromFile("conf/resources/sparql/events-query.rq")
    val q = f.mkString
    f.close() ; q
  }
}
