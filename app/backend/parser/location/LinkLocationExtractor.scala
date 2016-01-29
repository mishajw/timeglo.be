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
    println(DB.getEvents
      .reverse
      .take(10)
      .map(e =>
        // Get every link...
        (linkRegex findAllIn e.description)
          // Format as a link...
          .map(s => s
            .substring(2, s.length - 2)
            .split("\\|").head
            .replace(" ", "_"))
          // Try to convert to coords
          .flatMap(getLocationFromDB)
      )
      .map(_.mkString(", "))
      .mkString("\n"))
  }

  private def getLocationFromDB(name: String): Option[SimpleLocation] = {
    statementGetCoords.setString(1, name)
    val results = statementGetCoords.executeQuery()

    if (results.next()) {
      Some(SimpleLocation(
        results.getString("gt_name"),
        Coords(
          results.getDouble("gt_lat"),
          results.getDouble("gt_lon"))
      ))
    } else {
      None
    }
  }
}
