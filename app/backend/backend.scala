/**
  * Created by misha on 27/12/15.
  */
package object backend {
  case class Coords(lat: Double, long: Double)
  case class SimpleLocation(name: String, coords: Coords, locationType: String)

  case class Event(date: Date, description: String, id: Option[Int] = None)
  case class Date(date: Int = 1, month: Int = 1, year: Int = 1)
  case class LocatedEvent(event: Event, location: SimpleLocation)

  sealed trait DatePrecision
  case object PreciseToYear extends DatePrecision
  case object PreciseToMonth extends DatePrecision
  case object PreciseToDate extends DatePrecision
  case object NotPrecise extends DatePrecision

  def strip(text: String) = text.toLowerCase().replaceAll("\\W", "")
}
