package backend

import backend.parser.{APIEventExtractor, LocationExtractor}
import backend.retriever.location.FileLocationRetriever
import backend.util.DB

object BackendOrganiser {
  def runAll(): Unit = {
    println("Getting events...")
    APIEventExtractor.run
    println("Getting locations...")
    FileLocationRetriever.setup("allCountries.txt")
    println("Extracting locations...")
    LocationExtractor.run()
    println("Indexing...")
    DB.performIndexing()

    println("Committing...")
    DB.commit()
    println("Disconnecting...")
    DB.disconnect()

    println("Done!")
  }

  def main(args: Array[String]) {
    BackendOrganiser.runAll()
  }
}
