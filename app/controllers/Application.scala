package controllers

import java.util.Calendar

import backend.util.DB
import org.json4s._
import org.json4s.jackson.JsonMethods
import play.api.Logger
import play.api.mvc._
class Application extends Controller {

  private val log = Logger(getClass)

  private val dateFormatString: String = "dd.MM.yyyy"
  private val dateFormat = new java.text.SimpleDateFormat(dateFormatString)

  def index = Action {
    Ok(views.html.index())
  }

  def getEvents(startString: String, endString: String) = Action {
    log.debug(s"Asked for events from $startString to $endString")

    try {
      val startDate = new java.sql.Date(dateFormat.parse(startString).getTime)
      val endDate = new java.sql.Date(dateFormat.parse(endString).getTime)

      log.debug(s"Parsed dates as $startDate and $endDate")

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

  def getDateRange = Action {
    log.debug("Asked for date range")

    DB.getDateRange match {
      case Some(tup) =>
        val (start, end) = tup

        def getParts(d: java.sql.Date) = {
          val cal = Calendar.getInstance()
          cal.setTime(d)
          ( cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.ERA) match {
              case 0 => -cal.get(Calendar.YEAR)
              case 1 =>  cal.get(Calendar.YEAR)
            })
        }

        val (startParts, endParts) = (getParts(start), getParts(end))

        val json = JObject(List(
          "startDate" -> JObject(List(
            "date" -> JInt(startParts._1),
            "month" -> JInt(startParts._2),
            "year" -> JInt(startParts._3)
          )),
          "endDate" -> JObject(List(
            "date" -> JInt(endParts._1),
            "month" -> JInt(endParts._2),
            "year" -> JInt(endParts._3)
          ))
        ))

        Ok(stringifyJson(json))
      case None =>
        errorJson("Couldn't get first and last dates.")
    }
  }

  private def getEventsInJson(startDate: java.sql.Date, endDate: java.sql.Date) = {
    JArray(DB.getWikiLocatedEvents(startDate, endDate)
      .map(le => {
        JObject(List(
          "date" -> JString(s"${le.event.date.date}-${le.event.date.month}-${le.event.date.year}"),
          "desc" -> JString(le.event.description),
          "location" -> JObject(List(
            "name" -> JString(le.location.name),
            "lat" -> JDouble(le.location.coords.lat),
            "long" -> JDouble(le.location.coords.long)
          ))
        ))
      }).toList)
  }

  private def stringifyJson(json: JValue) =
    JsonMethods.pretty(JsonMethods.render(json))

  private def errorJson(errorMsg: String) = {
    log.warn(s"Giving user error: $errorMsg")
    BadRequest(stringifyJson(JObject(List(
      "error" -> JString(errorMsg))
    )))
  }
}
