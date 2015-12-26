package wikimap.parser

/**
  * Created by misha on 26/12/15.
  */
package object simple {
  case class SimpleEvent(date: Date, description: String)
  case class Date(date: Int, month: String, year: Int)
}
