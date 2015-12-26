package wikimap.parser

/**
  * Created by misha on 26/12/15.
  */
package object simple {
  sealed trait Location
  case class LocationName(name: String) extends Location
  case class LocationCoord(long: Double, lat: Double) extends Location

  case class SimpleEvent(date: Date, description: String)
  case class Date(date: Int, month: String, year: Int)
}
