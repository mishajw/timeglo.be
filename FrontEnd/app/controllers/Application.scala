package controllers

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
      val startDate = dateFormat.parse(startString)
      val endDate = dateFormat.parse(endString)

      Ok(s"$startDate - $endDate")
    } catch {
      case e: java.text.ParseException =>
        Ok(s"Couldn't parse dates. Must be in DD-MM-YYYY format")
    }
  }
}
