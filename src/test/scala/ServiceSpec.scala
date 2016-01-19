import akka.event.NoLogging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.ContentTypes._
import akka.http.scaladsl.model.{HttpResponse, HttpRequest}
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.stream.scaladsl.Flow
import org.scalatest._

class ServiceSpec extends FlatSpec with Matchers with ScalatestRouteTest with Service {
  override def testConfigSource = "akka.loglevel = WARNING"
  override def config = testConfig
  override val logger = NoLogging


  val expResponse = Counters(List())

  "Service" should "respond a list of counters" in {
    Get("/counters?from=0&to=5") ~> countersRoute ~> check {
      status shouldBe OK
      contentType shouldBe `application/json`
      responseAs[Counters] shouldBe expResponse
    }
  }


  val putData = UserPagePair("5", "2")

  it should "increment a counter" in {
    Put("/inc", putData) ~> incCounter ~> check {
      status shouldBe OK
      contentType shouldBe NoContentType
      responseAs[String] shouldBe ""
    }
  }


}
