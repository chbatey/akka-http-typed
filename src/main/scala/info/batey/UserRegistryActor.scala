package info.batey

//#user-registry-actor
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.{ Actor, ActorLogging, Props }

//#user-case-classes
final case class User(name: String, age: Int, countryOfResidence: String)
final case class Users(users: Seq[User])
//#user-case-classes

object UserRegistry {

  final case class ActionPerformed(description: String)

  sealed trait UserCommand
  final case class GetUsers(replyTo: ActorRef[Users]) extends UserCommand
  final case class CreateUser(user: User, replyTo: ActorRef[ActionPerformed]) extends UserCommand
  final case class GetUser(name: String, replyTo: ActorRef[Option[User]]) extends UserCommand
  final case class DeleteUser(name: String, replyTo: ActorRef[ActionPerformed]) extends UserCommand

  def apply(users: Set[User] = Set.empty): Behavior[UserCommand] = Behaviors.receiveMessage {
    case GetUsers(replyTo) =>
      replyTo.tell(Users(users.toSeq))
      Behaviors.same
    case CreateUser(user, replyTo) =>
      replyTo.tell(ActionPerformed(s"User ${user.name} created."))
      UserRegistry(users + user)
    case GetUser(name, replyTo) =>
      replyTo.tell(users.find(_.name == name))
      Behaviors.same
    case DeleteUser(name, replyTo) =>
      UserRegistry(users.filterNot(_.name == name))
      Behaviors.same
  }

}