package wikimap

import java.io.PrintWriter

import wikimap.parser.SimpleDateFetcher

/**
  * Created by misha on 24/12/15.
  */
object Tester {
  def main(args: Array[String]): Unit = {
    val dates = (new SimpleDateFetcher).run.mkString("\n")

    new PrintWriter("out.txt") {
      write(dates)
      close()
    }
  }
}
