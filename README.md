#Play2 Scala REST + ReactiveMongo

Reactive Mongo (http://reactivemongo.org) will give us non-blocking and asynchronous access to a Mongo document store through a Scala driver and a Play! module for easy integration into a Play! app
but returns results as Scala Futures, and provides translation utilities for translating the Mongo document format (BSON) to JSON, and many functional helper methods for dealing with result sets.



###Register the plugin with Play

*conf/play.plugins*

```
400:play.modules.reactivemongo.ReactiveMongoPlugin
```

###Add the DB config

*conf/application.conf*

```
with user name and password:
mongodb.uri ="mongodb://username:password@localhost:27017/your_db_name"
without login credintials
mongodb.uri ="mongodb://localhost:27017/your_db_name"
```
---

##Code

###Model

First we are gonna create our basic model and we will be using Play automatic JSON Combinators to serialize case classes from and to JSON. 

*app/models/Car.scala*

```scala

package models

import reactivemongo.bson.BSONObjectID
import play.api.libs.json.Json

case class Car(
                 _id : String = BSONObjectID.generate.toString(), 
                 title : String, 
                 description : Option[String] = None
                 )

object Car {
  implicit val beersFormat = Json.format[Car]
}

```

###Utils

These are just handy wrappers used in Play action composition to validate JSON and provide a not so secure layer that can give you some hints on how to implement your own API security and validate that the incoming data conforms to a model.

*app/utils/JsonUtils.scala*

This trait function is used to validate incoming JSON in a generic reusable way so we can focus our controller actions in the actual CRUD and not so much in validating on each controller method.

```scala
package utils

import play.api.libs.json.{JsValue, Reads}
import scala.concurrent.Future
import play.api.mvc.{Results, SimpleResult}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._

trait JsonUtils extends Results {

  def validateJson[T](json: JsValue, success: (T, JsValue) =>
    Future[SimpleResult])(implicit reads: Reads[T]) = {
    json.validate[T].asEither match {
      case Left(errors) => {
        Logger.warn(s"Bad request : $errors")
        Future(BadRequest(errors.mkString(",")))
      }
      case Right(valid) => success(valid, json)
    }
  }

}
```

*app/utils/SecureActions.scala*

This trait helps us with enforcing security and json validation on each action that requires it. **It is incomplete and does not actually validate the header tokens or keys agains't anything, in the request header you can substitute the REST_API_KEY_HEADER header with anyValue**

```scala
package utils

import play.api.mvc._
import play.api.libs.json.Reads
import play.api.Logger
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

trait SecureActions extends JsonUtils {

  val REST_API_KEY_HEADER = "REST-API-KEY-HEADER"

  def SimpleAuthenticatedAction(f: (Request[AnyContent]) => Future[SimpleResult]) =
  Action.async { implicit request =>
      Logger.debug(s"received request : $request")
      request.headers.get(REST_API_KEY_HEADER).map {
        key => f(request)
      } getOrElse {
        Future(Unauthorized(s"Missing authentication headers: $REST_API_KEY_HEADER"))
      }
  }

  def JsonAuthenticatedAction[T](f: (T, Request[AnyContent]) =>
    Future[SimpleResult])(implicit reads: Reads[T]) = SimpleAuthenticatedAction {
    (request) =>
      request.body.asJson match {
        case Some(json) => validateJson[T](json, (t, validJson) => f(t, request))
        case None => Future(BadRequest("no json found"))
      }
  }

}


``` The web API-KEY-HEADER

**TO PUT or add car record
POST http://localhost:9000/cars
HEADERS:
accept: application/json
content-type: Application/json
rest-api-key-header: anyToken2

BODY
{"_id":"11","title":"title1","description":"desc1"}
{"_id":"22","title":"title2","description":"desc2"}
{"_id":"33","title":"title3","description":"desc3"}
=====================================
**TO GET ALL CARS
GET http://localhost:9000/cars
HEADERS:
accept: application/json
content-type: Application/json
rest-api-key-header: anyToken
===================================
**TO GET Specific CAR by id
GET http://localhost:9000/cars/11
HEADERS
accept: application/json
content-type: Application/json
rest-api-key-header: anyToken
====================================
**TO PUT or modify Specific CAR by id
PUT http://localhost:9000/cars/11
HEADERS
accept: application/json
content-type: Application/json
rest-api-key-header: anyToken

BOODY:
{"title":"updated1","description":"updated1"}

====================================
**TO DELETE specific car by id
DELETE http://localhost:9000/cars/33
HEADERS
accept: application/json
content-type: Application/json
rest-api-key-header: anyToken

