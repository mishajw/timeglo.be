package controllers

import java.sql.Date
import java.text.DateFormat

import backend.util.DB
import play.api.mvc._

import scala.reflect.macros.ParseException

class Application extends Controller {

  private val dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy")

  def index = Action {
    Ok(views.html.index())
  }

  def getEvents(startString: String, endString: String) = Action {
    try {
      val startDate = new java.sql.Date(dateFormat.parse(startString).getTime)
      val endDate = new java.sql.Date(dateFormat.parse(endString).getTime)

      val events = DB.getLocatedEvents(startDate, endDate)

      Ok(events.mkString("\n\n"))
    } catch {
      case e: java.text.ParseException =>
        Ok(s"Couldn't parse dates. Must be in DD-MM-YYYY format")
    }
  }
}
