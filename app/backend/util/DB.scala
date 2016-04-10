package backend.util

import java.time.LocalDate
import java.util.Calendar

import backend._
import play.api.Logger
import scalikejdbc.{AutoSession, ConnectionPool, GlobalSettings, LoggingSQLAndTimeSettings, _}

import scala.io.{BufferedSource, Source}

/**
  * Created by misha on 28/12/15.
  */
object DB {
  private val log = Logger(getClass)

  private val sqlPath: String = "conf/resources/postgres"

  lazy implicit val session = {
    Class.forName("org.postgresql.Driver")
    ConnectionPool.singleton("jdbc:postgresql://localhost/wikimap", "postgres", "postgres")

    GlobalSettings.loggingSQLAndTime = LoggingSQLAndTimeSettings(
      enabled = false
    )

    AutoSession
  }

  private val jesusWasBorn: java.util.Date = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, 0)
    cal.set(Calendar.MONTH, 0)
    cal.set(Calendar.DAY_OF_MONTH, 0)
    cal.getTime
  }

  def resetTables(tables: Seq[String]) = {
    sql"""${Source.fromFile(s"$sqlPath/tables.sql").mkString}"""
    sql"""${Source.fromFile(s"$sqlPath/drop.sql").mkString}"""
  }

  def dateRange: Option[(java.sql.Date, java.sql.Date)] = {
    ???
  }

  def insertEvent(event: NewLocatedEvent) = {
    ???
  }

  def getEvents: Seq[NewLocatedEvent] = {
    ???
  }

  def performIndexing() = {
    ???
  }

  def searchForEvent(start: java.sql.Date, end: java.sql.Date, searchWords: String): Seq[NewLocatedEvent] = {
    ???
  }

  private def toSqlDate(d: Date) = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, d.year)
    cal.set(Calendar.MONTH, d.month)
    cal.set(Calendar.DAY_OF_MONTH, d.date + 1)

    new java.sql.Date(cal.getTime.getTime)
  }

  private def fromSqlDate(d: java.sql.Date): Date = {
    val localDate: LocalDate = d.toLocalDate

    Date(localDate.getDayOfMonth, localDate.getMonthValue, {
      if (d.before(jesusWasBorn))
        -localDate.getYear
      else
        localDate.getYear
    })
  }

  private def getLinesFromFile(file: BufferedSource) = file
    .getLines().toList
    .mkString("\n")
    .split("\n\n").toList

  private def getLineFromFileName(fileName: String): String = {
    try {
      val file = Source.fromFile(fileName)
      val line = getLinesFromFile(file).head
      file.close()
      line
    } catch {
      case e: Throwable =>
        println(s"Couldn't open file $fileName"); ""
    }
  }
}
