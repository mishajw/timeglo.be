/**
  * Created by misha on 27/12/15.
  */
package object backend {
  case class Event(date: Date, description: String, id: Option[Int] = None)
  case class Date(date: Int = 1, month: Int = 1, year: Int = 1)
  case class LocatedEvent(event: Event, location: Location)

  case class Coords(lat: Double, long: Double)
  case class Location(name: String, coords: Coords, locationType: String)

  case class NewEvent(date: NewDate, desc: String)
  case class NewLocatedEvent(event: NewEvent, location: Location)
  case class NewDate(date: Int = 1, month: Int = 1, year: Int = 1, precision: DatePrecision = PreciseToDate) {
    override def toString: String = precision match {
      case PreciseToDate => s"NewDate($date/$month/$year)"
      case PreciseToMonth => s"NewDate($month/$year)"
      case PreciseToYear => s"NewDate($year)"
      case NotPrecise => "NewDate(N/A)"
    }
  }

  sealed trait DatePrecision
  case object PreciseToYear extends DatePrecision
  case object PreciseToMonth extends DatePrecision
  case object PreciseToDate extends DatePrecision
  case object NotPrecise extends DatePrecision

  def strip(text: String) = text.toLowerCase().replaceAll("\\W", "")
}
