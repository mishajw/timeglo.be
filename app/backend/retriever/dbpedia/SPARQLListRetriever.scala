package backend.retriever.dbpedia

import java.net.URLEncoder

import backend._

import scala.io.Source

import org.json4s._
import org.json4s.native.JsonMethods._

object SPARQLListRetriever {

  def run: Seq[Event] = {
    val raw = getRaw

    val json = parse(raw)

    val parsed = parseJson(json)

    println(parsed.mkString("\n"))

    Seq()
  }

  private def parseJson(json: JValue): List[List[String]] = {

    def getValue(obj: List[(String, JValue)]): String = {
      (for { JField("value", JString(value)) <- obj } yield value).head
    }

    (for {
      JObject(obj: List[(String, JsonAST.JValue)]) <- json
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
          Date(1, 1, 1),
          getValue(desc)),
        SimpleLocation(
          getValue(placeName),
          Coords(getValue(lat).toDouble, getValue(long).toDouble),
          "")
      )
    }).asInstanceOf[List[List[String]]]
  }

  private def getRaw: String = {
//    Source.fromURL(url(query)).mkString
    Source.fromFile("largeresources/dbpedia.json").mkString
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
