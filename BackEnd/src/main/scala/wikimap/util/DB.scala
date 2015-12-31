package wikimap.util

import java.sql.{DriverManager, PreparedStatement, ResultSet}
import java.time.LocalDate
import java.util.Calendar

import wikimap.{Coords, Date, Location, SimpleEvent}

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.{BufferedSource, Source}


/**
  * Created by misha on 28/12/15.
  */
object DB {
  Class.forName("org.postgresql.Driver")
  val connection = DriverManager.getConnection("jdbc:postgresql://localhost/wikimap", "misha", "")
  connection.setAutoCommit(false)
  val statement = connection.createStatement()

  private val insertCommands: Map[String, PreparedStatement] = Seq(
    ("events", Seq("occurs", "description")),
    ("locations", Seq("latitude", "longitude", "population")),
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

  def insertEvent(event: SimpleEvent) = {
    val ps = insertCommands("events")
    ps.setDate(1, toSqlDate(event.date))
    ps.setString(2, event.description)

    ps.executeUpdate()
  }

  def getEvents: Seq[SimpleEvent] = {
    val results: ResultSet = statement.executeQuery("SELECT * FROM events;")

    var events: Seq[SimpleEvent] = Seq()

    while (results.next()) {
      events = events :+ SimpleEvent(fromSqlDate(results.getDate("occurs")), results.getString("description"))
    }

    events
  }

  def insertLocation(l: Location) = {
    val s = insertCommands("locations")
    s.setDouble(1, l.coords.lat)
    s.setDouble(2, l.coords.long)
    s.setInt(3, l.population)

    s.executeUpdate()

    val id = {
      val sStr: String = s"SELECT id FROM locations WHERE latitude='${l.coords.lat}' AND longitude='${l.coords.long}' AND population='${l.population}'"
      val results = statement.executeQuery(sStr)
      results.next()
      results.getLong("id")
    }

    l.names.foreach(n => {
      val s = insertCommands("locationNames")
      s.setLong(1, id)
      s.setString(2, n)
      s.executeUpdate()
    })
  }

  def getLocation(name: String): Option[Location] = {
    val stripped = wikimap.strip(name)

    println(stripped)

    val statementString =
      "SELECT L.latitude, L.longitude, L.population " +
      "FROM locations L, locationNames N " +
      "WHERE L.id=N.locationID " +
      s"AND N.name='$name';"

    val results = statement.executeQuery(statementString)

    println(statementString)

    val possibleLocations = new ListBuffer[Location]()

    while (results.next()) {
      possibleLocations += Location(
        Seq(name),
        Coords(results.getDouble("latitude"), results.getDouble("longitude")),
        results.getInt("population"))
    }

    println(possibleLocations)

    possibleLocations.sortBy(-_.population).toList match {
      case Nil => None
      case x :: xs => Some(x)
    }
  }

  private def toSqlDate(d: Date) = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, d.year)
    cal.set(Calendar.MONTH, d.month)
    cal.set(Calendar.DAY_OF_MONTH, d.date)

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
}
