package controllers

import backend.util.DB
import org.json4s._
import org.json4s.jackson.JsonMethods
import play.api.mvc._
class Application extends Controller {

  private val dateFormat = new java.text.SimpleDateFormat("dd.MM.yyyy")

  def index = Action {
    Ok(views.html.index())
  }

  def getEvents(startString: String, endString: String) = Action {
    try {
      val startDate = new java.sql.Date(dateFormat.parse(startString).getTime)
      val endDate = new java.sql.Date(dateFormat.parse(endString).getTime)

      val events = JArray(DB.getLocatedEvents(startDate, endDate)
        .map(le => {
          JObject(List(
            "date" -> JString(le.event.date.toString),
            "desc" -> JString(le.event.description),
            "location" -> JObject(List(
              "names" -> JArray(le.location.names.map(JString).toList),
              "lat" -> JDouble(le.location.coords.lat),
              "long" -> JDouble(le.location.coords.long),
              "population" -> JString(le.location.population.toString)
            ))
          ))
        }).toList)

      Ok(JsonMethods.pretty(JsonMethods.render(events)))
    } catch {
      case e: java.text.ParseException =>
        Ok(s"Couldn't parse dates. Must be in DD-MM-YYYY format")
    }
  }
}
