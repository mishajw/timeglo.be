package wikimap.retriever.wikipedia

import org.json4s._
import org.json4s.native.JsonMethods._

import scala.io.Source

/**
  * Created by misha on 24/12/15.
  */
object APIArticleRetreiver {
  val baseURL = "https://en.wikipedia.org/w/api.php?%20format=json&action=query&prop=revisions&rvprop=content"

  def getTitle(title: String): String = {
    val rawJSON = Source.fromURL(s"$baseURL&titles=$title").mkString

    (for (
      JObject(parsed) <- parse(rawJSON);
      JField("query", JObject(query)) <- parsed;
      JField("pages", JObject(pages)) <- query;
      JField(id, JObject(page)) <- pages;
      JField("revisions", JArray(revisions)) <- page;
      JObject(revision) <- revisions;
      JField("*", JString(content)) <- revision
    ) yield content).head
  }
}
