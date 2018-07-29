package controllers

import models.Car
import models.Car._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json.collection.JSONCollection
import utils.SecureActions


/**
 * REST API Controller providing CRUD operations for Beers with Reactive Mongo in a full async API
 */
object Application extends Controller with MongoController with SecureActions {

  /**
   * A reference of a JSON style collection in Mongo
   */
  private def carsCollection = db.collection[JSONCollection]("cars")


  /**
   * Convinience helper thar marshalls json or sends a 404 if none found
   */
  private def asJson(v: Option[JsObject]) = v.map(Ok(_)).getOrElse(NotFound)

  /**
   * Default index entry point
   */
  def index = Action {
    Ok(views.html.index("carsREST!"))
  }

  /**
   * Actions that reactively list all cars in the collection
   */
  def listCars() = SimpleAuthenticatedAction {
    _ =>
      carsCollection
        .find(Json.obj())
        .cursor[JsObject]
        .collect[List]()map {
        cars =>
          Ok(JsArray(cars))

      }
  }

  /**
   * Finds a car by Id
   */
  def findCar(id: String) = SimpleAuthenticatedAction {
    _ =>
      carsCollection
        .find(Json.obj("id" -> id))
        .one[JsObject] map asJson
  }

  /**
   * Adds a cars
   */
  def addCar() = JsonAuthenticatedAction[Car] {
    (car, _) =>
      carsCollection.insert(car) map {
        _ => Ok(Json.toJson(car))
      }
  }

  /**
   * Partially updates the properties of a cars
   */
  def updateCar(id: String) = JsonAuthenticatedAction[JsObject] {
    (json, _) =>
      for {
        _ <- carsCollection.update(Json.obj("id" -> id), Json.obj("$set" -> json))
        newCar <- carsCollection.find(Json.obj("id" -> id)).one[JsObject]
      } yield asJson(newCar)
  }

  /**
   * Deletes a beer by id
   */
  def deleteCar(id: String) = SimpleAuthenticatedAction {
    _ =>
      for {
        newCar <- carsCollection.find(Json.obj("id" -> id)).one[JsObject]
        _ <- carsCollection.remove(Json.obj("id" -> id))
      } yield asJson(newCar)
  }

}