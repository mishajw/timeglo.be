package backend

import backend.parser.sparql.SPARQLParser
import backend.parser.{APIEventExtractor, LinkLocationExtractor}
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

    log.info("Removing blacklisted")
    DB.deleteBlacklisted()

    log.info("Done")
  }

  private def runSparql() = {
    log.info("Getting events from DBpedia using SPARQL")
    val jsons = SPARQLListRetriever.run

    log.info("Parsing JSON")
    val events =
      (jsons flatMap SPARQLParser.parse)
        .filter(_.event.date.precision != NotPrecise)

    log.info("Inserting events into the database")
    events.foreach(DB.insertLocatedEvent)
  }

  private def runDateRetriever() = {
    log.info("Getting events from Wikipedia API")
    val events = APIEventExtractor.run

    log.info("Pairing with locations")
    val eventLocations =
      LinkLocationExtractor.run(events)
        .filter { case (e, ls) => ls.nonEmpty }

    log.info("Inserting events into the database")
    eventLocations map
      { case (e, ids) => DB.insertEventWithLocationIds(e, ids) }
  }
}
