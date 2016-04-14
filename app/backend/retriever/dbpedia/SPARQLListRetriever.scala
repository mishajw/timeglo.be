package backend.retriever.dbpedia

import java.net.URLEncoder

import org.json4s._
import org.json4s.native.JsonMethods._
import play.api.Logger

import scala.io.Source

object SPARQLListRetriever {

  private val log = Logger(getClass)

  def run: Seq[JValue] = {
    queries
      .map(url)
      .map(Source.fromURL(_).mkString)
      .map(parse(_))
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
