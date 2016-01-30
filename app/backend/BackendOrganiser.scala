package backend

import backend.parser.{APIEventExtractor, LinkLocationExtractor}
import backend.util.DB
import play.api.Logger

object BackendOrganiser {
  private val log = Logger(getClass)

  def runAll(): Unit = {
    log.info("Getting events...")
    APIEventExtractor.run
    DB.performIndexing()
    DB.commit()

    log.info("Extracting locations...")
    LinkLocationExtractor.run()
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
