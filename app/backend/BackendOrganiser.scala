package backend

import backend.retriever.dbpedia.SPARQLListRetriever
import play.api.Logger

object BackendOrganiser {
  private val log = Logger(getClass)

  def runAll(): Unit = {

  }

  def main(args: Array[String]) {
//    BackendOrganiser.runAll()

    SPARQLListRetriever.run
  }
}
