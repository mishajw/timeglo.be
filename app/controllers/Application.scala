package controllers

import backend.util.DB
import org.json4s._
import org.json4s.jackson.JsonMethods
import play.api.mvc._
class Application extends Controller {

  private val dateFormatString: String = "dd.MM.yyyy"
  private val dateFormat = new java.text.SimpleDateFormat(dateFormatString)

  def index = Action {
    Ok(views.html.index())
  }

  def getEvents(startString: String, endString: String) = Action {
    try {
      val startDate = new java.sql.Date(dateFormat.parse(startString).getTime)
      val endDate = new java.sql.Date(dateFormat.parse(endString).getTime)

      if (startDate.before(endDate)) {
        Ok(stringifyJson(getEventsInJson(startDate, endDate)))
      } else {
        errorJson("Start date must be before end date")
      }
    } catch {
      case e: java.text.ParseException =>
        errorJson(s"Not a valid format for a date. Must be in format ${dateFormatString.toUpperCase()}")
    }
  }

  private def getEventsInJson(startDate: java.sql.Date, endDate: java.sql.Date) = {
    JArray(DB.getLocatedEvents(startDate, endDate)
      .map(le => {
        JObject(List(
          "date" -> JString(le.event.date.toString),
          "desc" -> JString(le.event.description),
          "location" -> JObject(List(
            "name" -> JString(le.location.formattedName),
            "matchedName" -> JString(le.matchedName),
            "lat" -> JDouble(le.location.coords.lat),
            "long" -> JDouble(le.location.coords.long),
            "population" -> JString(le.location.population.toString)
          ))
        ))
      }).toList)
  }

  private def stringifyJson(json: JValue) =
    JsonMethods.pretty(JsonMethods.render(json))

  private def errorJson(errorMsg: String) = BadRequest(stringifyJson(JObject(List(
    "error" -> JString(errorMsg))
  )))
}
