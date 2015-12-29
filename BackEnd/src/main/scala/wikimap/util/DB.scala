package wikimap.util

import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.util.URLParser
import com.github.mauricio.async.db.{ResultSet, Connection, QueryResult, RowData}
import wikimap.{Date, SimpleEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.io.{BufferedSource, Source}


/**
  * Created by misha on 28/12/15.
  */
object DB {
  private val configuration = URLParser.parse("jdbc:postgresql://localhost:5432/wikimap?username=postgres&password=")
  private val connection: Connection = new PostgreSQLConnection(configuration)
  Await.result(connection.connect, 5 seconds)

  private val insertCommands: Map[String, String] = Seq(
    ("events", Seq("occurs", "description")),
    ("locations", Seq("latitude", "longitude", "population")),
    ("locationNames", Seq("locationID", "name")),
    ("eventLocations", Seq("eventID", "locationID")))
    .map(tup =>
      tup._1 -> (
        s"INSERT INTO ${tup._1}" +
        s"(${tup._2.mkString(", ")}) " +
        s"VALUES (${Seq.fill(tup._2.size)("?").mkString(", ")});"))
    .toMap

  def resetTables() = {
    Seq("drop", "tables") .foreach(f => {
      val file = Source.fromFile(s"src/main/resources/sql/$f.sql")

      getLinesFromFile(file)
        .foreach(l => Await.result(connection.sendQuery(l), 5 seconds))

      file.close()
    })
  }

  def disconnect = connection.disconnect

  def insertEvent(event: SimpleEvent) = {
    await(connection.sendPreparedStatement(
      insertCommands("events"),
      Seq(dateToString(event.date), event.description)))
  }

  def getEvents(): Seq[SimpleEvent] = {
    val results: QueryResult = await(connection.sendQuery("SELECT description FROM events;"))

    results.rows match {
      case Some(r) =>
        r.map(row => SimpleEvent(stringToDate(""), row(0).toString))
      case None =>
        Seq()
    }
  }

  private def stringToDate(s: String) = {
    Date(0,0,0)
  }

  private def dateToString(d: Date) = {
    if (d.year > 0)
      f"'${d.date}%02d-${d.month}%02d-${d.year}%04d'"
    else
      f"'${d.date}%02d-${d.month}%02d-${-d.year}%04d BC'"
  }

  private def getLinesFromFile(file: BufferedSource) = file
    .getLines().toList
    .mkString("\n")
    .split("\n\n").toList

//  def main(args: Array[String]) {
//    val future: Future[QueryResult] = connection.sendQuery("SELECT * FROM test")
//
//    val mapResult: Future[Any] = future.map(queryResult => queryResult.rows match {
//      case Some(resultSet) =>
//        val row: RowData = resultSet.map((r: RowData) => r.)
//        row(0)
//      case None => -1
//    })
//
//    val result = Await.result( mapResult, 5 seconds )
//
//    println(result)
//
//    connection.disconnect
//  }

  private def await[T](future: Future[T]): T = Await.result(future, 5 seconds)
}
