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

  def resetTables() = {
    sql"""
       DROP TABLE IF EXISTS date_precision CASCADE;
       DROP TABLE IF EXISTS events CASCADE;
       DROP TABLE IF EXISTS locations CASCADE;
       DROP TABLE IF EXISTS located_events_db CASCADE;
       DROP TABLE IF EXISTS located_events_wiki CASCADE;

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
         wiki_page       TEXT,
         precision       INT REFERENCES date_precision,
         description     TEXT
       );

       CREATE TABLE locations (
         id              SERIAL PRIMARY KEY,
         name            TEXT,
         latitude        REAL,
         longitude       REAL
       );

       CREATE TABLE located_events_db (
         event_id        INT REFERENCES events,
         location_id     INT REFERENCES locations,
         PRIMARY KEY (event_id, location_id)
       );

       CREATE TABLE located_events_wiki (
         event_id        INT REFERENCES events,
         location_id     INT REFERENCES page,
         PRIMARY KEY (event_id, location_id)
       );
       
      """.update.apply()
  }

  def dateRange: Option[(java.sql.Date, java.sql.Date)] = {
    ???
  }

  def insertEvent(event: Event): Long = {
      sql"""
         INSERT INTO events (occurs, wiki_page, precision, description) VALUES (
            ${toSqlDate(event.date)},
            ${event.wikiPage},
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

  def insertLocatedEvent(le: LocatedEvent): Long = {
    val eventId = insertEvent(le.event)
    val locationId = insertLocation(le.location)

    sql"""
         INSERT INTO located_events_db (event_id, location_id)
         VALUES ($eventId, $locationId)
       """.update.apply()
  }

  def insertEventWithLocationIds(event: Event, locationIds: Seq[Long]): Long = {
    val eventId = insertEvent(event)
    
    sql"""
         INSERT INTO located_events_wiki (event_id, location_id)
         VALUES (?, ?)
       """.batch(locationIds.map(Seq(eventId, _))).apply()

    eventId
  }

  def getLocatedEvents: Seq[LocatedEvent] = {
    sql"""
         SELECT E.description, E.occurs, E.wiki_page, L.name, L.latitude, L.longitude, P.type AS precision
         FROM located_events_db LE, events E, locations L, date_precision P
         WHERE
          LE.event_id = E.id AND
          LE.location_id = L.id AND
          E.precision = P.id
       """.map(resultsToLocatedEvent).list.apply()
  }

  def getLocationForLink(link: String): Option[Long] = {
    sql"""
         SELECT G.gt_id AS id
         FROM page P, geo_tags G
         WHERE
           P.page_title = $link AND
           P.page_id = G.gt_page_id
       """.map(_.long("id")).single.apply()
  }

  def performIndexing() = {
    sql"""
       CREATE INDEX occurs_idx     ON events           (occurs);
       CREATE INDEX el_locid_idx   ON event_locations   (location_id);
       CREATE INDEX el_evid_idx    ON event_locations   (event_id);
       """.update.apply()
  }

  def searchForEvent(start: java.sql.Date, end: java.sql.Date, searchWords: String): Seq[LocatedEvent] = {
    sql"""
        SELECT E.description, E.occurs, E.wiki_page, L.name, L.latitude, L.longitude, P.type AS precision
        FROM located_events_db LE, events E, locations L, date_precision P
        WHERE
         LE.event_id = E.id AND
         LE.location_id = L.id AND
         E.precision = P.id AND
         P.type != 'NotPrecise' AND
         $start < E.occurs AND E.occurs < $end AND
         (
            ${searchWords.isEmpty} OR
            regexp_replace(lower(E.description), '[^A-Za-z0-9 ]', '', 'g') LIKE
              '%' || regexp_replace(lower($searchWords), '[^A-Za-z0-9 ]', '', 'g') || '%'
         )
       """.map(resultsToLocatedEvent).list.apply() ++
      sql"""
        SELECT E.description, E.occurs, E.wiki_page, L.name, L.gt_lat AS latitude, L.gt_lon AS longitude, P.type AS precision
        FROM located_events_wiki LE, events E, geo_tags L, date_precision P
        WHERE
         LE.event_id = E.id AND
         LE.location_id = L.gt_id AND
         E.precision = P.id AND
         P.type != 'NotPrecise' AND
         $start < E.occurs AND E.occurs < $end AND
         (
            ${searchWords.isEmpty} OR
            regexp_replace(lower(E.description), '[^A-Za-z0-9 ]', '', 'g') LIKE
              '%' || regexp_replace(lower($searchWords), '[^A-Za-z0-9 ]', '', 'g') || '%'
         )
       """.map(resultsToLocatedEvent).list.apply()
  }

  private def resultsToLocatedEvent(r: WrappedResultSet): LocatedEvent =
    LocatedEvent(
      Event(
        fromSqlDate(r.date("occurs"), r.string("precision")),
        Some(r.string("wiki_page")),
        r.string("description")),
      Location(
        r.string("name"),
        Coords(r.double("latitude"), r.double("longitude")), ""))

  private def toSqlDate(d: Date) = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, d.year)
    cal.set(Calendar.MONTH, d.month)
    cal.set(Calendar.DAY_OF_MONTH, d.date + 1)

    new java.sql.Date(cal.getTime.getTime)
  }

  private def fromSqlDate(d: java.sql.Date, precisionRaw: String): Date = {
    val precision = precisionRaw match {
      case "PreciseToYear" => PreciseToYear
      case "PreciseToMonth" => PreciseToMonth
      case "PreciseToDate" => PreciseToDate
      case "NotPrecise" => NotPrecise
    }

    val localDate: LocalDate = d.toLocalDate

    Date(localDate.getDayOfMonth, localDate.getMonthValue, {
      if (d.before(jesusWasBorn))
        -localDate.getYear
      else
        localDate.getYear
    }, precision)
  }
}
