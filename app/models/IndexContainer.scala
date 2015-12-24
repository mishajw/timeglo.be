package models

import scala.io.Source

/**
  * Created by misha on 24/12/15.
  */
class IndexContainer {
  type IndexLine = (String, CompressedPosition)
  type CompressedPosition = (Long, Long)

  private var indexes: Map[String, CompressedPosition] = Map()
  private lazy val numberOfLines = {
    val file = getFile
    val n = file.getLines().size
    file.close()
    n
  }

  def getCompressedPosition(title: String): CompressedPosition = {
    getLineFromTitle(title.toLowerCase())._2
  }

  def getLineFromTitle(title: String): IndexLine= {
    def helper(min: Int, max: Int): IndexLine = {
      println(s"$min, $max")

      val middleIndex: Int = (min + max) / 2
      val middle@(middleTitle, _) = getLineFromNumber(middleIndex)

      println(middleTitle)

      if (middleTitle == title) {
        middle
      } else if (min >= max) {
        ("", (0, 0))
      } else if (middleTitle < title) {
        helper(middleIndex + 1, max)
      } else {
        helper(min, middleIndex - 1)
      }
    }

    helper(0, numberOfLines)
  }

  private def getLineFromNumber(lineNumber: Int): IndexLine = {
    println(lineNumber)
    val file = getFile
    val lines = file.getLines()

    val line = parseLine(if (lineNumber == 0) {
      lines.next()
    } else {
      lines.drop(lineNumber - 1).next()
    })

    file.close
    line
  }

  def parseLine(line: String): IndexLine = {
    val split = line.split(":")
    (split(2).toLowerCase(), (split(0).toLong, split(1).toLong))
  }

  private def getFile = Source.fromFile("res/enwiki-20151201-pages-articles-multistream-index.txt")
}
