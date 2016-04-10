package backend

import backend.retriever.dbpedia.SPARQLListRetriever
import backend.util.DB
import play.api.Logger

object BackendOrganiser {
  private val log = Logger(getClass)

  def main(args: Array[String]) {
    DB.resetTables()
    val events = SPARQLListRetriever.run
    events.foreach(DB.insertLocatedEvent)

    println(DB.getLocatedEvents.mkString("\n"))
  }
}
