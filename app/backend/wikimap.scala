/**
  * Created by misha on 27/12/15.
  */
package object backend {
  case class Coords(lat: Double, long: Double)
  case class Location(names: Seq[String], coords: Coords, population: java.math.BigDecimal, id: Option[Int] = None)

  case class Event(date: Date, description: String, id: Option[Int] = None)
  case class Date(date: Int, month: Int, year: Int)
  case class LocatedEvent(event: Event, location: Location)

  def strip(text: String) = text.toLowerCase().replaceAll("\\W", "")
}
