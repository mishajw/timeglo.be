package controllers

import java.sql.Date
import java.text.DateFormat

import play.api.mvc._

import scala.reflect.macros.ParseException

class Application extends Controller {

  private val dateFormat = new java.text.SimpleDateFormat("dd-MM-yyyy")

  def index = Action {
    Ok(views.html.index())
  }

  def getEvents(startString: String, endString: String) = Action {
    try {
      val startDate = Date.valueOf(startString)
      val endDate = Date.valueOf(endString)

      Ok(s"$startDate - $endDate")
    } catch {
      case e: IllegalArgumentException =>
        Ok(s"Couldn't parse dates. Must be in DD-MM-YYYY format")
    }
  }
}
