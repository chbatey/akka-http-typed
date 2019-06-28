package info.batey

//#quick-start-server
import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.Future
import scala.util.{ Failure, Success }

object QuickstartServer extends App {

  val system = ActorSystem[Nothing](Behaviors.setup[Nothing] { ctx =>
    // http doesn't know about akka typed so create untyped system/materializer
    implicit val untypedSystem: actor.ActorSystem = ctx.system.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()(ctx.system.toUntyped)
    implicit val ec = ctx.system.executionContext

    // didn't convert this yet
    val userRoutesRef = ctx.actorOf(UserRegistryActor.props, "userRegistryActor")

    val routes = new UserRoutes(untypedSystem, userRoutesRef)

    val serverBinding: Future[Http.ServerBinding] = Http()(ctx.system.toUntyped).bindAndHandle(routes.userRoutes, "localhost", 8080)
    serverBinding.onComplete {
      case Success(bound) =>
        println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        Console.err.println(s"Server could not start!")
        e.printStackTrace()
      // TODO send message to shut self down
    }
    Behaviors.empty

  }, "helloAkkaHttpServer")

}
