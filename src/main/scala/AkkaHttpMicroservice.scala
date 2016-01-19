import akka.actor.ActorSystem
import akka.event.{LoggingAdapter, Logging}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.ToResponseMarshallable
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.StatusCodes._
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import scala.collection.mutable
import scala.concurrent.{ExecutionContextExecutor, Future}
import spray.json.DefaultJsonProtocol
import akka.http.scaladsl.server.Directives._

case class PageOffsets(from: Int, to: Int)

case class UserPagePair(user: String, page: String)

case class CounterInfo(key: String, value: Int)

case class Counters(items: List[CounterInfo])

object CounterState{
  import scala.collection.mutable.{HashMap, HashSet}

  val hashTable = new HashMap[String, HashSet[String]]()

  def inc(userPagePair: UserPagePair): Unit = {
    userPagePair match {
      case UserPagePair(userId, pageId) => {
        synchronized{
          hashTable.get(pageId) match {
            case Some(uniqueUsers) => {
              uniqueUsers += userId
            }
            case None => {
              val uniqueUsers = new HashSet[String]()
              uniqueUsers += userId
              hashTable += ((pageId, uniqueUsers))
            }
          }
        }
      }
    }
  }

  def get(pageId: String): Int = {
    hashTable.get(pageId) match {
      case Some(uniqueUsers) => uniqueUsers.size
      case None => 0
    }
  }

  def list(fromInt: Int, toInt: Int): Counters = {
    val lst = hashTable.take(toInt).map(kv => CounterInfo(kv._1, kv._2.size)).toList
    Counters(lst)
  }
}

trait Protocols extends DefaultJsonProtocol {
  implicit val counterInfoFormat = jsonFormat2(CounterInfo)
  implicit val countersInfoFormat = jsonFormat1(Counters)
  implicit val userPagePair = jsonFormat2(UserPagePair)
}

trait Service extends Protocols {
  implicit val system: ActorSystem
  implicit def executor: ExecutionContextExecutor
  implicit val materializer: Materializer

  def config: Config
  val logger: LoggingAdapter

  val countersRoute = {
    path("counters") {
      get {
        parameters('from.as[Int], 'to.as[Int])
          .as(PageOffsets) { offsets =>
            complete {
              val items = CounterState.list(offsets.from, offsets.to)
              items
            }
        }
      }
    }
  }

  val incCounter = {
    path("inc") {
      put {
        entity(as[UserPagePair]) { userPage => {
            mapResponse(f => f.withEntity(HttpEntity.Empty)) {
              complete {
                CounterState.inc(userPage)
                OK
              }
            }
          }
        }
      }
    }
  }

  val routes = {
    logRequestResult("akka-http-microservice") {
      countersRoute ~ incCounter
    }
  }
}

object AkkaHttpMicroservice extends App with Service {
  override implicit val system = ActorSystem()
  override implicit val executor = system.dispatcher
  override implicit val materializer = ActorMaterializer()

  override val config = ConfigFactory.load()
  override val logger = Logging(system, getClass)

  Http().bindAndHandle(routes, config.getString("http.interface"), config.getInt("http.port"))
}
