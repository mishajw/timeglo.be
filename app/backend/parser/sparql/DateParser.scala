package backend.parser.sparql

import java.time.Year

import backend.{Date, NotPrecise, PreciseToMonth, PreciseToYear}
import play.api.Logger

object DateParser {

  private val log = Logger(getClass)

  private val rNumericDate = "(-?\\d+)-(\\d+)-(\\d+)".r
  private val rYearOnly =    "(-?\\d{1,4})".r
  private val rMonthDate =   "--(\\d+)-(\\d+)".r
  private val rNumber =      "(\\d+)[ ,\\.\\-]+".r
  private val rYearAndBC =   "(\\d+)( BC)?".r

  private val months = backend.months.map(_.toLowerCase)
  private val monthsAbbr = months.map(_.take(3))

  case class Context(s: String)

  def parse(dateStr: String, contexts: String*): Date = {
    implicit val context = Context(contexts.mkString(" "))

    dateStr match {
      case rNumericDate(y, m, d) =>
        Date(d.toInt, m.toInt, y.toInt)
      case rYearOnly(y) =>
        parseYearOnly(y.toInt)
      case rMonthDate(m, d) =>
        parseMonthDate(m.toInt, d.toInt)
      case unparsed =>
        parseOtherDate(unparsed)
    }
  }

  private def parseYearOnly(year: Int)(implicit context: Context = Context("")) = {
    // If we can find BC in the context...
    if (s"$year (BC|bc)".r findFirstMatchIn context.s isDefined) {
      Date(year = -year, precision = PreciseToYear)
    } else {
      Date(year = year, precision = PreciseToYear)
    }
  }

  private def parseMonthDate(month: Int, date: Int)(implicit context: Context = Context("")): Date = {
    (rNumber findAllIn context.s.replace(",", ""))
      .map(_ filter (_.isDigit))
      .map(_.toInt)
      .toSeq
      .filter(_ <= Year.now().getValue)
      .sorted
      .lastOption match {
        case Some(year) => Date(date, month, year)
        case None => Date(precision = NotPrecise)
      }
  }

  def parseOtherDate(dateStr: String)(implicit context: Context = Context("")): Date = {
    val year = (rYearAndBC findAllIn dateStr)
      .toSeq
      .filter(_.length < 8)
      .map { y =>
        if (y.endsWith(" BC"))
          -y.split(" ").head.toInt
        else y.toInt
      }
      .headOption

    val month = dateStr
      .split("\\W+")
      .map(_.toLowerCase)
      .flatMap { w =>
        if (months contains w)
          Some(months indexOf w)
        else if (monthsAbbr contains w)
          Some(monthsAbbr indexOf w)
        else None
      }
     .map(_ + 1)
     .headOption

    (month, year) match {
      case (Some(m), Some(y)) =>
        Date(month = m, year = y, precision = PreciseToMonth)
      case (None, Some(y)) =>
        parseYearOnly(y)
      case (Some(m), None) =>
        parseMonthDate(m, 1).copy(precision = PreciseToMonth)
      case _ =>
        Date(precision = NotPrecise)
    }
  }
}
