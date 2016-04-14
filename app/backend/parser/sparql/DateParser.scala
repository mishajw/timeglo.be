package backend.parser.sparql

import backend.{NotPrecise, PreciseToYear, Date}
import play.api.Logger

object DateParser {

  private val log = Logger(getClass)

  private val rNumericDate = "(-?\\d+)-(\\d+)-(\\d+)".r
  private val rYearOnly =    "(-?\\d{1,4})".r

  def parse(dateStr: String, contexts: String*): Date = dateStr match {
    case rNumericDate(y, m, d)  => Date(d.toInt, m.toInt, y.toInt)
    case rYearOnly(y)           => Date(year = y.toInt, precision = PreciseToYear)
    case unparsed =>
      log.warn(s"Couldn't parse as date: $unparsed")
      Date(precision = NotPrecise)
  }

}
