package backend.parser.location

import java.sql.DriverManager

import backend.{Coords, SimpleLocation}
import backend.util.DB
import play.api.Logger

import scala.io.Source

/**
  * Created by misha on 26/01/16.
  */
object LinkLocationExtractor {
  private val log = Logger(getClass)

  Class.forName("com.mysql.jdbc.Driver")
  val connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/wikipedia", "root", "")
  connection.setAutoCommit(false)
  val statement = connection.createStatement()

  val statementGetCoords = connection.prepareStatement({
    val f = Source.fromFile("res/mysql/get_coords.sql")
    val s = f.mkString
    f.close() ; s
  })

  private val linkRegex = "\\[\\[[^\\[\\]]*\\]\\]".r

  def run() = {
    // For every event...
    DB.getEvents
      .foreach(e => {
        // Get every link...
        val coords = (linkRegex findAllIn e.description)
          // Format as a link...
          .map(s => s
            .substring(2, s.length - 2)
            .split("\\|").head)
          .flatMap(e => Seq(
            e.replace(" ", "_"),
            e.replace(" ", "")))
          // Try to convert to coords
          .flatMap(getLocationFromDB)

        log.info(s"${e.description.replace("\n", "")} =>\n${coords.mkString(", ")}")
      })
  }

  private def getLocationFromDB(name: String): Option[SimpleLocation] = {
    statementGetCoords.setString(1, name)
    val results = statementGetCoords.executeQuery()

    if (results.next()) {
      val geoName: String = results.getString("geo_name")
      Some(SimpleLocation(
        geoName match {
          case "" => results.getString("page_name")
          case n => n
        },
        Coords(
          results.getDouble("gt_lat"),
          results.getDouble("gt_lon"))
      ))
    } else {
      None
    }
  }
}
