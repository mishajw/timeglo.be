package backend.parser

import backend.Location
import backend.util.DB
import org.json4s.JsonAST.JValue
import play.api.Logger

import scala.io.Source
import org.json4s._
import org.json4s.native.JsonMethods._

/**
  * Created by misha on 17/01/16.
  */
object NationalityExtractor {
  private val log = Logger(getClass)

  private case class Nationality(country: String, name: String)

  def run() = {
    val file = Source.fromFile("res/nationalities.json")

    val json = parse(file.mkString)

    val nationalities: List[Nationality] = for (
      JArray(countries) <- json;
      JObject(params) <- countries;
      JField("country", JString(country)) <- params;
      JField("nationality", JString(nationality)) <- params
    ) yield Nationality(country, nationality)


    nationalities
      .foreach(nationality => {
        DB.getLocationFromNames(Seq(backend.strip(nationality.country))) match {
          case Some(tup) =>
            tup._1.id match {
              case Some(id) =>
                DB.insertName(id, backend.strip(nationality.name))
              case None =>
                log.error("Couldn't insert nationality because of no ID.")
            }
          case None =>
            log.warn(s"Couldn't find country for $nationality")
        }
      })

    file.close()
  }
}
