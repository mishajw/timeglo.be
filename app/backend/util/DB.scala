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

  Class.forName("org.postgresql.Driver")
  val connection = DriverManager.getConnection("jdbc:postgresql://localhost/wikimap", "misha", "")
  connection.setAutoCommit(false)
  val statement = connection.createStatement()

  private val locatedEventStatement =
    connection.prepareStatement(getLineFromFileName("res/sql/event_locations.sql"))
  private val locationFromNamesStatement =
    connection.prepareStatement(getLineFromFileName("res/sql/location_from_names.sql"))

  private val insertCommands: Map[String, PreparedStatement] = Seq(
    ("events", Seq("occurs", "description")),
    ("locations", Seq("id", "name", "latitude", "longitude", "population")),
    ("locationNames", Seq("locationID", "name")),
    ("eventLocations", Seq("eventID", "locationID", "nameID")))
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
      val file = Source.fromFile(s"res/sql/$f.sql")

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

  def insertLocationWithID(l: Location, id: Int) = {
    val s = insertCommands("locations")

    s.setInt(1, id)
    s.setString(2, l.formattedName)
    s.setDouble(3, l.coords.lat)
    s.setDouble(4, l.coords.long)
    s.setBigDecimal(5, l.population)

    s.executeUpdate()

    l.matchedNames.foreach(n => {
      val s = insertCommands("locationNames")
      s.setLong(1, id)
      s.setString(2, n)
      s.executeUpdate()
    })
  }

  def getLocationFromNames(names: Seq[String]): Option[(Location, Int)] = {
    locationFromNamesStatement.setArray(1, connection.createArrayOf("varchar", names.toArray))

    val results = locationFromNamesStatement.executeQuery()

//    val possibleLocations = new ListBuffer[Location]()

    if (results.next()) {
      Some(
        Location(
          results.getString("name"),
          Seq(results.getString("foundName")),
          Coords(results.getDouble("latitude"), results.getDouble("longitude")),
          results.getBigDecimal("population"),
          Some(results.getInt("id"))),
        results.getInt("nameID"))
    } else {
      None
    }
  }

  def batchEventLocation(locationID: Int, eventID: Int, nameID: Int) = {
    val ps = insertCommands("eventLocations")

    ps.setInt(1, eventID)
    ps.setInt(2, locationID)
    ps.setInt(3, nameID)

    ps.addBatch()
  }

  def insertAllEventLocation() = {
     insertCommands("eventLocations").executeBatch()
  }

  def getLocatedEvents(start: java.sql.Date, end: java.sql.Date): Seq[LocatedEvent] = {
    locatedEventStatement.setDate(1, start)
    locatedEventStatement.setDate(2, end)
    val results = locatedEventStatement.executeQuery()

    val locatedEvents = new ListBuffer[LocatedEvent]()

    while (results.next()) {
      locatedEvents += LocatedEvent(
        Event(
          fromSqlDate(results.getDate("occurs")),
          results.getString("description")),
        Location(
          results.getString("name"),
          Seq(results.getString("matchedName")),
          Coords(results.getFloat("latitude"), results.getFloat("longitude")),
          results.getBigDecimal("population")),
        results.getString("matchedName"))
    }

    locatedEvents.toSeq
  }

  def performIndexing() = {
    val file = Source.fromFile("res/sql/index.sql")

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
    val results = statement.executeQuery(getLineFromFileName("res/sql/date_range.sql"))

    if (results.next()) {
      Some(results.getDate("earliest_date"), results.getDate("latest_date"))
    } else {
      None
    }
  }

  def insertName(id: Int, name: String): Unit = {
    val ps = insertCommands("locationNames")
    ps.setInt(1, id)
    ps.setString(2, name)
    ps.executeUpdate()
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
