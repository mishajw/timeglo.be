package controllers

import java.util.Calendar

import backend.LocatedEvent
import backend.util.DB
import org.json4s.ParserUtil.ParseException
import org.json4s._
import org.json4s.jackson.JsonMethods
import play.api.Logger
import play.api.mvc._

class Application extends Controller {

  private val log = Logger(getClass)

  private val dateFormatString: String = "dd.MM.yyyy"
  private val dateFormat = new java.text.SimpleDateFormat(dateFormatString)

  private val regexBadLocationType = "adm.+".r

  def index = Action {
    Ok(views.html.index())
  }

  def indexWithDates(start: String, end: String) = Action {
    Ok("")
  }

  def indexWithSearch(start: String, end: String, search: String) = Action {
    Ok("")
  }

  def search(startString: String, endString: String, searchTerm: String) = Action {
    log.debug(s"Asked for date between $startString and $endString, with search term $searchTerm")

    stringToSqlDates(startString, endString) match {
      case Some((startDate, endDate)) if startDate before endDate =>
          val events = DB.searchForEvent(startDate, endDate, searchTerm.replace("%20", " "))

          log.debug(s"Sending user ${events.length} events")

          Ok(
            stringifyJson(
              eventsToJson(events)))
      case None => errorJson("Incorrect date format")
      case _ => errorJson("Start date must be before end date")
    }
  }

  def getDateRange = Action {
    log.debug("Asked for date range")

    DB.dateRange match {
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

  private def eventsToJson(events: Seq[LocatedEvent]) = {
    JArray(events.map(le => {
      JObject(List(
        "date" -> JString(s"${le.event.date.date}.${le.event.date.month}.${le.event.date.year}"),
        "datePrecision" -> JString(le.event.date.precision.toString),
        "desc" -> JString(le.event.desc),
        "wikiPage" -> JString(le.event.wikiPage.getOrElse("")),
        "location" -> JObject(List(
          "name" -> JString(le.location.name.replace("_", " ")),
          "lat" -> JDouble(le.location.coords.lat),
          "long" -> JDouble(le.location.coords.long),
          "type" -> JString(le.location.locationType match {
            case regexBadLocationType() => ""
            case s => s
          })
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

  private def stringToSqlDates(start: String, end: String): Option[(java.sql.Date, java.sql.Date)] = {
    try {
      Some(
        new java.sql.Date(dateFormat.parse(start).getTime),
        new java.sql.Date(dateFormat.parse(end).getTime))
    } catch {
      case e: ParseException => None
    }
  }
}
