package info.batey

import akka.actor.typed.{ ActorRef, ActorSystem }

import scala.concurrent.duration._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.delete
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.http.scaladsl.server.directives.PathDirectives.path

import scala.concurrent.Future
import akka.util.Timeout
import info.batey.UserRegistry.{ ActionPerformed, CreateUser, DeleteUser, GetUser, GetUsers, UserCommand }

class UserRoutes(system: ActorSystem[_], userRegistryActor: ActorRef[UserCommand]) extends JsonSupport {
  lazy val log = system.log

  // Required by the `ask` method below
  implicit val timeout = Timeout(5.seconds) // usually we'd obtain the timeout from the system's configuration
  implicit val scheduler = system.scheduler
  import akka.actor.typed.scaladsl.AskPattern._

  lazy val userRoutes: Route =
    pathPrefix("users") {
      concat(
        pathEnd {
          concat(
            get {
              val users: Future[Users] = userRegistryActor.ask(replyTo => GetUsers(replyTo))
              complete(users)
            },
            post {
              entity(as[User]) { user =>
                val userCreated: Future[ActionPerformed] = userRegistryActor.ask(replyTo => CreateUser(user, replyTo))
                onSuccess(userCreated) { performed =>
                  log.info("Created user [{}]: {}", user.name, performed.description)
                  complete((StatusCodes.Created, performed))
                }
              }
            })
        },
        path(Segment) { name =>
          concat(
            get {
              val maybeUser: Future[Option[User]] = userRegistryActor.ask(replyTo => GetUser(name, replyTo))
              rejectEmptyResponse {
                complete(maybeUser)
              }
            },
            delete {
              val userDeleted: Future[ActionPerformed] = userRegistryActor.ask(replyTo => DeleteUser(name, replyTo))
              onSuccess(userDeleted) { performed =>
                log.info("Deleted user [{}]: {}", name, performed.description)
                complete((StatusCodes.OK, performed))
              }
            })
        })
    }
}
