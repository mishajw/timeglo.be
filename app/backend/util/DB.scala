package backend.util

import java.time.LocalDate
import java.util.Calendar

import backend._
import org.json4s._
import org.json4s.native.JsonMethods._
import play.api.Logger
import scalikejdbc.{AutoSession, ConnectionPool, GlobalSettings, LoggingSQLAndTimeSettings, _}

import scala.io.Source

/**
  * Created by misha on 28/12/15.
  */
object DB {

  private val log = Logger(getClass)

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
         precision       SERIAL REFERENCES date_precision,
         description     TEXT
       );

       CREATE TABLE locations (
         id              SERIAL PRIMARY KEY,
         name            TEXT,
         latitude        REAL,
         longitude       REAL
       );

       CREATE TABLE located_events_db (
         event_id        SERIAL REFERENCES events ON DELETE CASCADE,
         location_id     SERIAL REFERENCES locations ON DELETE CASCADE,
         PRIMARY KEY (event_id, location_id)
       );

       CREATE TABLE located_events_wiki (
         event_id        SERIAL REFERENCES events ON DELETE CASCADE,
         location_id     SERIAL REFERENCES geo_tags ON DELETE CASCADE,
         PRIMARY KEY (event_id, location_id)
       );
       
      """.update.apply()
  }

  def dateRange: Option[(java.sql.Date, java.sql.Date)] = {
    sql"""
         SELECT
           MAX(occurs) AS max_date,
           MIN(occurs) AS min_date
         FROM events;
       """.map(r => (r.date("min_date"), r.date("max_date")))
          .single.apply()
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
    log.debug(s"Inserting location IDs $locationIds for event $event")

    val eventId = insertEvent(event)

    for (id <- locationIds) {
      try {
        sql"""
           INSERT INTO located_events_wiki (event_id, location_id)
           VALUES ($eventId, $id)
         """.update.apply()
      } catch {
        case e: Throwable =>
          log.error(s"Got an error with event id $eventId and location id $id", e)
      }
    }

    eventId
  }

  def getLocationForLink(link: String): Option[Long] = {
    sql"""
         SELECT G.gt_id AS id
         FROM page P, geo_tags G
         WHERE
           P.page_title = $link AND
           P.page_id = G.gt_page_id AND
           G.gt_name IS NOT NULL
       """.map(_.long("id")).list.apply().headOption
  }

  def searchForEvent(start: java.sql.Date, end: java.sql.Date, searchWords: String): Seq[LocatedEvent] = {
    sql"""
         SELECT E.description, E.occurs, E.wiki_page, PR.type AS precision, LE.*
         FROM
           events E, date_precision PR,
           (
             (
               SELECT
                 P.page_title AS name,
                 G.gt_lat AS latitude,
                 G.gt_lon AS longitude,
                 LE.event_id
               FROM
                 located_events_wiki LE,
                 page P, geo_tags G
               WHERE
                 LE.location_id = G.gt_id AND
                 G.gt_page_id = P.page_id
             ) UNION (
               SELECT
                 L.name AS name,
                 L.latitude AS latitude,
                 L.longitude AS longitude,
                 LE.event_id
               FROM
                 located_events_db LE,
                 locations L
               WHERE
                 LE.location_id = L.id
             )
           ) AS LE
         WHERE
           E.precision = PR.id AND
           LE.event_id = E.id AND
           PR.type != 'NotPrecise' AND
           $start < E.occurs AND E.occurs < $end AND
           (
              ${searchWords.isEmpty} OR
              regexp_replace(lower(E.description), '[^A-Za-z0-9 ]', '', 'g') LIKE
                '%' || regexp_replace(lower($searchWords), '[^A-Za-z0-9 ]', '', 'g') || '%'
           ) AND
           NOT (LE.latitude = 0 AND LE.longitude = 0)

         LIMIT 7000
       """.map(resultsToLocatedEvent).list.apply()
  }

  def performIndexing() = {
    sql"""
       CREATE INDEX IF NOT EXISTS occurs_idx     ON events                (occurs);
       CREATE INDEX IF NOT EXISTS el_locid_idx   ON located_events_db     (location_id);
       CREATE INDEX IF NOT EXISTS el_locid_idx   ON located_events_db     (event_id);
       CREATE INDEX IF NOT EXISTS el_locid_idx   ON located_events_wiki   (location_id);
       CREATE INDEX IF NOT EXISTS el_locid_idx   ON located_events_wiki   (event_id);
       CREATE INDEX IF NOT EXISTS page_title_idx ON page                  (page_title);
       """.update.apply()
  }

  def deleteBlacklisted(): Unit = {
    case class BL(date: String, location: String, search: String)
    case class BLCollection(blacklisted: Seq[BL])

    implicit val formats = DefaultFormats
    val json = parse(Source.fromFile("conf/resources/blacklist.json").mkString)
    val blacklisted: BLCollection = json.extract[BLCollection]

    for (b <- blacklisted.blacklisted) {
      log.info(s"Removing $b")
      sql"""
         DELETE FROM events
         WHERE
           occurs = ${backend.stringToSqlDate(b.date)} AND
           description LIKE '%' || ${b.search} || '%' AND
           (
             id IN (
               SELECT LED.event_id FROM
                 located_events_db LED,
                 locations L
               WHERE
                 LED.location_id = L.id AND
                 L.name = ${b.location}
             ) OR
             id IN (
               SELECT LEW.event_id FROM
                 located_events_wiki LEW,
                 geo_tags G,
                 page P
               WHERE
                 LEW.location_id = G.gt_id AND
                 P.page_id = G.gt_page_id AND
                 P.page_title = ${b.location}
             )
           )
       """.update.apply()
    }
  }

  private def resultsToLocatedEvent(r: WrappedResultSet): LocatedEvent =
    LocatedEvent(
      Event(
        fromSqlDate(r.date("occurs"), r.string("precision")),
        r.stringOpt("wiki_page"),
        r.string("description")),
      Location(
        r.string("name"),
        Coords(r.double("latitude"), r.double("longitude")), ""))

  private def toSqlDate(d: Date) = {
    val cal = Calendar.getInstance()
    cal.set(Calendar.YEAR, d.year)
    cal.set(Calendar.MONTH, d.month - 1)
    cal.set(Calendar.DAY_OF_MONTH, d.date)

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
