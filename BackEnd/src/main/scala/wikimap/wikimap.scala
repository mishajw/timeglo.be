/**
  * Created by misha on 27/12/15.
  */
package object wikimap {
  case class Coords(lat: Double, long: Double)
  case class Location(names: Seq[String], coords: Coords, population: Int)

  case class SimpleEvent(date: Date, description: String)
  case class Date(date: Int, month: Int, year: Int)

  def strip(text: String) = text.toLowerCase().replaceAll("\\W", "")
}
