package info.batey

//#quick-start-server
import akka.{Done, actor}
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object QuickstartServer extends App {

  val system = ActorSystem[Done](Behaviors.setup { ctx =>
    // http doesn't know about akka typed so create untyped system/materializer
    implicit val untypedSystem: actor.ActorSystem = ctx.system.toUntyped
    implicit val materializer: ActorMaterializer = ActorMaterializer()(ctx.system.toUntyped)
    implicit val ec: ExecutionContextExecutor = ctx.system.executionContext

    val userRoutesRef = ctx.spawn(UserRegistry(), "userRegistryActor")

    val routes = new UserRoutes(ctx.system, userRoutesRef)

    val serverBinding: Future[Http.ServerBinding] = Http()(untypedSystem).bindAndHandle(routes.userRoutes, "localhost", 8080)
    serverBinding.onComplete {
      case Success(bound) =>
        println(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
      case Failure(e) =>
        Console.err.println(s"Server could not start!")
        e.printStackTrace()
        ctx.self ! Done
    }
    Behaviors.receiveMessage {
      case Done =>
        Behaviors.stopped
    }

  }, "helloAkkaHttpServer")

}
