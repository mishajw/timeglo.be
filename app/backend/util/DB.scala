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
    sql"""
       DROP TABLE IF EXISTS date_precision CASCADE;
       DROP TABLE IF EXISTS events CASCADE;
       DROP TABLE IF EXISTS locations CASCADE;
       DROP TABLE IF EXISTS located_events CASCADE;

       CREATE TABLE date_precision (
         id              SERIAL PRIMARY KEY,
         type            TEXT
       );

       INSERT INTO date_precision(type) VALUES ('PreciseToYear');
       INSERT INTO date_precision(type) VALUES ('PreciseToMonth');
       INSERT INTO date_precision(type) VALUES ('PreciseToDate');
       INSERT INTO date_precision(type) VALUES ('NotPrecise');

       CREATE TABLE events (
         id              SERIAL PRIMARY KEY,
         occurs          DATE,
         precision       INT REFERENCES date_precision,
         description     TEXT
       );

       CREATE TABLE locations (
         id              SERIAL PRIMARY KEY,
         name            TEXT,
         latitude        REAL,
         longitude       REAL
       );

       CREATE TABLE located_events (
         event_id        INT REFERENCES events,
         location_id     INT REFERENCES locations,
         PRIMARY KEY (event_id, location_id)
       );
      """.update.apply()
  }

  def dateRange: Option[(java.sql.Date, java.sql.Date)] = {
    ???
  }

  def insertEvent(event: NewEvent): Long = {
      sql"""
         INSERT INTO events (occurs, precision, description) VALUES (
            ${toSqlDate(event.date)},
            (SELECT id FROM date_precision WHERE type = ${event.date.precision.toString}),
            ${event.desc}
         )
       """.updateAndReturnGeneratedKey.apply()
  }

  def insertLocation(location: Location): Long = {
    val id: Option[Long] = sql"""
          SELECT id FROM locations
          WHERE name = ${location.name}
      """.map(_.long("id")).single.apply()

    id getOrElse {
      sql"""
           INSERT INTO locations (name, latitude, longitude) VALUES (
              ${location.name},
              ${location.coords.lat},
              ${location.coords.long}
           )
         """.updateAndReturnGeneratedKey.apply()
    }
  }

  def insertLocatedEvent(le: NewLocatedEvent): Long = {
    val eventId = insertEvent(le.event)
    val locationId = insertLocation(le.location)

    sql"""
         INSERT INTO located_events (event_id, location_id)
         VALUES ($eventId, $locationId)
       """.update.apply()
  }

  def getLocatedEvents: Seq[NewLocatedEvent] = {
    sql"""
         SELECT E.description, L.name, L.latitude, L.longitude
         FROM located_events LE, events E, locations L, date_precision P
         WHERE
          LE.event_id = E.id AND
          LE.location_id = L.id AND
          E.precision = P.id
       """.map(resultsToLocatedEvent).list.apply()
  }

  def performIndexing() = {
    sql"""
       CREATE INDEX occurs_idx     ON events           (occurs);
       CREATE INDEX el_locid_idx   ON event_locations   (location_id);
       CREATE INDEX el_evid_idx    ON event_locations   (event_id);
       """.update.apply()
  }

  def searchForEvent(start: java.sql.Date, end: java.sql.Date, searchWords: String): Seq[NewLocatedEvent] = {
    sql"""
        SELECT E.description, L.name, L.latitude, L.longitude
        FROM located_events LE, events E, locations L, date_precision P
        WHERE
         LE.event_id = E.id AND
         LE.location_id = L.id AND
         E.precision = P.id AND
         $start < E.occurs AND E.occurs < $end AND
         (
            ${searchWords.isEmpty} OR
            regexp_replace(lower(E.description), '[^A-Za-z0-9 ]', '', 'g') LIKE
              '%' || regexp_replace(lower($searchWords), '[^A-Za-z0-9 ]', '', 'g') || '%'
         )
       """.map(resultsToLocatedEvent).list.apply()
  }

  private def resultsToLocatedEvent(r: WrappedResultSet): NewLocatedEvent =
    NewLocatedEvent(
      NewEvent(
        NewDate(precision = NotPrecise),
        r.string("description")),
      Location(
        r.string("name"),
        Coords(r.double("latitude"), r.double("longitude")), ""))

  private def toSqlDate(d: NewDate) = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, d.year)
    cal.set(Calendar.MONTH, d.month)
    cal.set(Calendar.DAY_OF_MONTH, d.date + 1)

    new java.sql.Date(cal.getTime.getTime)
  }

  private def fromSqlDate(d: java.sql.Date): NewDate = {
    val localDate: LocalDate = d.toLocalDate

    NewDate(localDate.getDayOfMonth, localDate.getMonthValue, {
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
