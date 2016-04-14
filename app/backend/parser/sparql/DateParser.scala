package backend.parser.sparql

import backend.{NotPrecise, PreciseToYear, Date}
import play.api.Logger

object DateParser {

  private val log = Logger(getClass)

  private val rNumericDate = "(-?\\d+)-(\\d+)-(\\d+)".r
  private val rYearOnly =    "(-?\\d{1,4})".r

  private case class Context(s: String)

  def parse(dateStr: String, contexts: String*): Date = {
    implicit val context = Context(contexts.mkString(" "))

    dateStr match {
      case rNumericDate(y, m, d) =>
        Date(d.toInt, m.toInt, y.toInt)
      case rYearOnly(y) =>
        parseYearOnly(y.toInt)
      case unparsed =>
        log.warn(s"Couldn't parse as date: $unparsed")
        Date(precision = NotPrecise)
    }
  }

  private def parseYearOnly(year: Int)(implicit context: Context) = {
    // If we can find BC in the context...
    if (s"$year (BC|bc)".r findFirstMatchIn context.s isDefined) {
      Date(year = -year, precision = PreciseToYear)
    } else {
      Date(year = year, precision = PreciseToYear)
    }
  }
}
