package backend.util

import java.sql.{DriverManager, PreparedStatement, ResultSet}
import java.time.LocalDate
import java.util.Calendar

import backend._
import org.postgresql.util.PSQLException

import scala.collection.mutable.ListBuffer
import scala.io.{BufferedSource, Source}


/**
  * Created by misha on 28/12/15.
  */
object DB {
  Class.forName("org.postgresql.Driver")
  val connection = DriverManager.getConnection("jdbc:postgresql://localhost/wikimap", "misha", "")
  connection.setAutoCommit(false)
  val statement = connection.createStatement()

  private val locatedEventStatement =
    connection.prepareStatement(getLineFromFileName("src/"))
  private val insertCommands: Map[String, PreparedStatement] = Seq(
    ("events", Seq("occurs", "description")),
    ("locations", Seq("id", "latitude", "longitude", "population")),
    ("locationNames", Seq("locationID", "name")),
    ("eventLocations", Seq("eventID", "locationID")))
    .map(tup =>
      tup._1 -> connection.prepareStatement(
        s"INSERT INTO ${tup._1}" +
        s"(${tup._2.mkString(", ")}) " +
        s"VALUES (${Seq.fill(tup._2.size)("?").mkString(", ")});"))
    .toMap

  private lazy val tableNames: Seq[String] = insertCommands.keySet.toSeq

  def resetTables(tables: Seq[String] = tableNames) = {
    Seq("drop", "tables") .foreach(f => {
      val file = Source.fromFile(s"src/main/resources/sql/$f.sql")

      getLinesFromFile(file)
        .foreach(l => {
          if (tables.exists(t => l.contains(s" $t ")))
            statement.execute(l)
        })

      file.close()
    })
  }

  def commit() = connection.commit()

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
    s.setDouble(2, l.coords.lat)
    s.setDouble(3, l.coords.long)
    s.setBigDecimal(4, l.population)

    s.executeUpdate()

//    val id = {
//      val sStr: String = s"SELECT id FROM locations WHERE latitude='${l.coords.lat}' AND longitude='${l.coords.long}' AND population='${l.population}'"
//      val results = statement.executeQuery(sStr)
//      results.next()
//      results.getLong("id")
//    }

    l.names.foreach(n => {
      val s = insertCommands("locationNames")
      s.setLong(1, id)
      s.setString(2, n)
      s.executeUpdate()
    })
  }

  def getLocation(name: String): Option[Location] = {
    val stripped = backend.strip(name)

    val statementString =
      "SELECT L.latitude, L.longitude, L.population, L.id " +
      "FROM locations L, locationNames N " +
      "WHERE L.id=N.locationID " +
      s"AND N.name='$stripped';"

    val results = statement.executeQuery(statementString)

    val possibleLocations = new ListBuffer[Location]()

    while (results.next()) {
      possibleLocations += Location(
        Seq(name),
        Coords(results.getDouble("latitude"), results.getDouble("longitude")),
        results.getBigDecimal("population"),
        Some(results.getInt("id")))
    }

    possibleLocations.sortBy(_.population.negate()).toList match {
      case Nil => None
      case x :: xs => Some(x)
    }
  }

  def getLocationFromNames(names: Seq[String]): Option[Location] = {
    val statementString =
      "SELECT L.latitude, L.longitude, L.population, L.id, N.name " +
        "FROM locations L, locationNames N " +
        "WHERE L.id=N.locationID " +
        s"AND N.name=ANY(ARRAY[" +
        names.map(n => s"'${backend.strip(n)}'").mkString(", ") + "]) " +
        "ORDER BY L.population DESC " +
        "LIMIT 1;"

    val results = statement.executeQuery(statementString)

    val possibleLocations = new ListBuffer[Location]()

    if (results.next()) {
      Some(Location(
        Seq(results.getString("name")),
        Coords(results.getDouble("latitude"), results.getDouble("longitude")),
        results.getBigDecimal("population"),
        Some(results.getInt("id"))))
    } else {
      None
    }
  }

  def batchEventLocation(locationID: Int, eventID: Int) = {
    val ps = insertCommands("eventLocations")

    ps.setInt(1, eventID)
    ps.setInt(2, locationID)

    ps.addBatch()
  }

  def insertAllEventLocation() = {
    insertCommands("eventLocations").executeBatch()
  }

  def getLocatedEvents: Seq[LocatedEvent] = {
    val statementString = getLineFromFileName("src/main/resources/sql/event_locations.sql")
    val results = statement.executeQuery(statementString)

    val locatedEvents = new ListBuffer[LocatedEvent]()

    while (results.next()) {
      locatedEvents += LocatedEvent(
        Event(
          fromSqlDate(results.getDate("occurs")),
          results.getString("description")),
        Location(
          results.getString("allNames").split(",").toSeq,
          Coords(results.getFloat("latitude"), results.getFloat("longitude")),
          results.getBigDecimal("population")))
    }

    locatedEvents.toSeq
  }

  def performIndexing() = {
    val file = Source.fromFile("src/main/resources/sql/index.sql")

    getLinesFromFile(file)
      .foreach(l => {
        val save = connection.setSavepoint()
        try {
          statement.executeUpdate(l)
        } catch {
          case e: PSQLException =>
            connection.rollback(save)
            println(s"Couldn't create index, because it already exists:\n$l")
            println(e)
        }
      })
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
    //TODO ensure BC works
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
