package backend

import backend.parser.{LinkLocationExtractor, APIEventExtractor}
import backend.retriever.dbpedia.SPARQLListRetriever
import backend.util.DB
import play.api.Logger

object BackendOrganiser {
  private val log = Logger(getClass)

  def main(args: Array[String]) {
    runAll()
  }

  private def runAll() = {
    log.info("Resetting database")
    DB.resetTables()

    log.info("Running sparql")
    runSparql()

    log.info("Running date retriever")
    runDateRetriever()

    log.info("Done")
  }

  private def runSparql() = {
    log.info("Getting events from DBpedia using SPARQL")
    val events = SPARQLListRetriever.run

    log.info("Inserting events into the database")
    events.foreach(DB.insertLocatedEvent)

    log.info(s"Found ${DB.getLocatedEvents.size} located events in the database")
  }

  private def runDateRetriever() = {
    log.info("Getting events from Wikipedia API")
    val events = APIEventExtractor.run

    log.info("Pairing with locations")
    val eventLocations = LinkLocationExtractor.run(events)

    log.info("Inserting events into the database")
    eventLocations map
      { case (e, ids) => DB.insertEventWithLocationIds(e, ids) }
  }
}
