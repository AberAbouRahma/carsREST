

package models

import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Json
import java.util.UUID

import org.joda.time.{DateTime, Days}

/**
 * @param id
 * @param title a mandatory title
 * @param description an optional description
 */
case class Car(
                 title : String,
                 fuel : String,
                 price : Int,
                 newCar: Boolean,
                 mileage: Option[Int] =  None,
                 firstRegistration :  Option[String] =None
                 )


/**
 * Companion object provides JSON serialization
 */
object Car {
  implicit val carsFormat = Json.format[Car]
}