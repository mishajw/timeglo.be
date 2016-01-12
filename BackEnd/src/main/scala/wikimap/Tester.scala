package wikimap

import java.io.FileWriter
import java.math.BigDecimal

import wikimap.parser.{FileEventExtractor, APIEventExtractor, LocationExtractor}
import wikimap.retriever.location.FileLocationRetriever
import wikimap.util.DB

object Tester {
  def main(args: Array[String]): Unit = {
//    APIEventExtractor.run
//    FileLocationRetriever.setup("allCountries.txt")
//    LocationExtractor.run()

//    println(DB.getLocatedEvents().mkString("\n"))

    DB.commit()
    DB.disconnect()
  }

  def full(): Unit = {
    println("Setting up locations...")
    FileLocationRetriever.setup("cities15000.txt")

    println("Getting events...")
    val events = FileEventExtractor.run()
    println(s"Found ${events.size}")

    println("Matching...")
    val matched = events.flatMap(e => LocationExtractor.extractLocation(e) match {
      case Some(l) => Some(e, l)
      case None => None
    })
    println(s"Found ${matched.size}")
 
    new FileWriter("out/matched.txt") {
      write(matched.map(tup =>
        s"${tup._1.date}:\n" +
          s"\tDesc: ${tup._1.description}\n" +
          s"\t${tup._2.names.head} at ${tup._2.coords}\n" +
          s"\tPopulation: ${tup._2.population}").mkString("\n"))
      close()
    }

  }
}
