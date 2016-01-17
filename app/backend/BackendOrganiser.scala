package backend

import backend.parser.{NationalityExtractor, APIEventExtractor, LocationExtractor}
import backend.retriever.location.FileLocationRetriever
import backend.util.DB
import play.api.Logger

object BackendOrganiser {
  private val log = Logger(getClass)

  def runAll(): Unit = {
    log.info("Getting events...")
    APIEventExtractor.run
    DB.performIndexing()
    DB.commit()

    log.info("Getting locations...")
    FileLocationRetriever.setup("allCountries.txt")
    DB.performIndexing()
    DB.commit()

    log.info("Getting nationalities...")
    NationalityExtractor.run()
    DB.performIndexing()
    DB.commit()

    log.info("Extracting locations...")
    LocationExtractor.run()
    DB.performIndexing()
    DB.commit()

    log.info("Disconnecting...")
    DB.disconnect()

    log.info("Done!")
  }

  def main(args: Array[String]) {
    BackendOrganiser.runAll()
  }
}
