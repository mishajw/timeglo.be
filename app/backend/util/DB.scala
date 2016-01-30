package backend.util

import java.sql.{DriverManager, PreparedStatement, ResultSet}
import java.time.LocalDate
import java.util.Calendar

import backend._
import org.postgresql.util.PSQLException
import play.api.Logger

import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}


/**
  * Created by misha on 28/12/15.
  */
object DB {
  private val log = Logger(getClass)

  private val sqlPath: String = "res/postgres"

  Class.forName("org.postgresql.Driver")
  val connection = DriverManager.getConnection("jdbc:postgresql://localhost/wikimap", "misha", "")
  connection.setAutoCommit(false)
  val statement = connection.createStatement()

  private val wikiLocatedEventStatement =
    connection.prepareStatement(getLineFromFileName(sqlPath + "/wiki_event_locations.sql"))
  private val locationFromWikiStatement =
    connection.prepareStatement(getLineFromFileName(sqlPath + "/wiki_get_loc_id.sql"))


  private val insertCommands: Map[String, PreparedStatement] = Seq(
    ("events", Seq("occurs", "description")),
    ("locations", Seq("id", "name", "latitude", "longitude", "population")),
    ("locationNames", Seq("locationID", "name")),
    ("eventLocations", Seq("eventID", "locationID", "nameID")),
    ("wikiEventLocations", Seq("eventID", "locationID")))
    .map(tup =>
      tup._1 -> connection.prepareStatement(
        s"INSERT INTO ${tup._1}" +
        s"(${tup._2.mkString(", ")}) " +
        s"VALUES (${Seq.fill(tup._2.size)("?").mkString(", ")});"))
    .toMap

  private lazy val tableNames: Seq[String] = insertCommands.keySet.toSeq

  def resetTables(tables: Seq[String] = tableNames) = {
    log.warn(s"Dropping tables: ${tables.mkString(", ")}")

    Seq("drop", "tables") .foreach(f => {
      val file = Source.fromFile(s"$sqlPath/$f.sql")

      getLinesFromFile(file)
        .foreach(l => {
          if (tables.exists(t => l.contains(s" $t ")))
            statement.execute(l)
        })

      file.close()
    })
  }

  def commit() = {
    log.debug("Committing changes to database")
    connection.commit()
  }

  def disconnect() = connection.close()

  def insertEvent(event: Event) = {
    val ps = insertCommands("events")
    ps.setDate(1, toSqlDate(event.date))
    ps.setString(2, event.description)

    ps.executeUpdate()
  }

  def getEvents: Seq[Event] = {
    val results: ResultSet = statement.executeQuery("SELECT * FROM events;")

    var events = new ListBuffer[Event]()

    while (results.next()) {
      events += Event(
        fromSqlDate(results.getDate("occurs")),
        results.getString("description"),
        Some(results.getInt("id")))
    }

    events.toSeq
  }

  def performIndexing() = {
    val file = Source.fromFile(sqlPath + "/index.sql")

    getLinesFromFile(file)
      .foreach(l => {
        val save = connection.setSavepoint()
        try {
          log.debug(s"Trying to create index:\n$l")
          statement.executeUpdate(l)
          log.debug("Done.")
        } catch {
          case e: PSQLException =>
            connection.rollback(save)
            log.warn(s"Couldn't create index, because it already exists.")
        }
      })
  }

  def getDateRange: Option[(java.sql.Date, java.sql.Date)] = {
    val results = statement.executeQuery(getLineFromFileName(sqlPath + "/date_range.sql"))

    if (results.next()) {
      Some(results.getDate("earliest_date"), results.getDate("latest_date"))
    } else {
      None
    }
  }

  def getLocationFromWiki(name: String): Option[Int] = {
    locationFromWikiStatement.setString(1, name)
    val results = locationFromWikiStatement.executeQuery()

    if (results.next()) {
      Some(results.getInt("gt_id"))
    } else {
      None
    }
  }

  def batchWikiEventLocation(eventID: Int, locationIDs: Seq[Int]) = {
    val ps = insertCommands("wikiEventLocations")
    ps.setInt(1, eventID)

    locationIDs.foreach(locationID => {
      ps.setInt(2, locationID)
      ps.addBatch()
    })
  }

  def insertAllWikiEventLocation() = {
    insertCommands("wikiEventLocations").executeBatch()
  }

  def getWikiLocatedEvents(start: java.sql.Date, end: java.sql.Date): Seq[LocatedEvent] = {
    wikiLocatedEventStatement.setDate(1, start)
    wikiLocatedEventStatement.setDate(2, end)

    val results = wikiLocatedEventStatement.executeQuery()

    val les = new ListBuffer[LocatedEvent]

    while (results.next()) {
      les += LocatedEvent(
        Event(
          fromSqlDate(results.getDate("occurs")),
          results.getString("description")),
        SimpleLocation(
          results.getString("name"),
          Coords(results.getFloat("latitude"), results.getFloat("longitude")))
      )
    }

    les.toSeq
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
    Date(localDate.getDayOfMonth, localDate.getMonthValue, localDate.getYear)
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
