package backend.retriever.dbpedia

import java.net.URLEncoder

import backend.Event

import scala.io.Source

object SPARQLListRetriever {

  def run: Seq[Event] = {
    val res = Source.fromURL(url(query)).mkString

    print(res)

    Seq()
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
