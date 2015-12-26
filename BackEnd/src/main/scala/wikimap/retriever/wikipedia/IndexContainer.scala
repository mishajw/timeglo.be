package wikimap.retriever.wikipedia

import scala.io.Source
import scala.util.{Failure, Success, Try}

/**
  * Created by misha on 24/12/15.
  */
class IndexContainer {
  type IndexLine = (String, CompressedPosition)
  type CompressedPosition = (Long, Long)

  val RawIndexLine = "([0-9]*):([0-9]*):(.*)".r

  private lazy val positions: Map[String, CompressedPosition] = {
    val file = Source.fromFile("res/enwiki-20151201-pages-articles-multistream-index.txt")

    val newMap = file.getLines.map({
      case RawIndexLine(bytePos, pagePos, title) =>
        title.toLowerCase -> (bytePos.toLong, pagePos.toLong)
      case x =>
        println(x)
        "" -> (-1l, -1l)
    }).toMap

    file.close()
    newMap
  }

  def getCompressedPosition(title: String): CompressedPosition = {
    println(positions)

    Try(positions(title.toLowerCase())) match {
      case Success(x) => x
      case Failure(_) => (-1l, -1l)
    }
  }
}
