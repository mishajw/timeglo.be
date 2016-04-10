package backend

import backend.retriever.dbpedia.SPARQLListRetriever
import backend.util.DB
import play.api.Logger

object BackendOrganiser {
  private val log = Logger(getClass)

  def main(args: Array[String]) {
    log.info("Resetting database tables")
    DB.resetTables()

    log.info("Getting events from DBpedia using SPARQL")
    val events = SPARQLListRetriever.run

    log.info("Inserting events into the database")
    events.foreach(DB.insertLocatedEvent)
  }
}
