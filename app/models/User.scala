package models

case class User(id: String)

object UserList {
  var users: List[User] = Nil

  def add(user: User) = if(this notContains user) users ::= user
  def leave(user: User) = { users = users filterNot (_ == user) }
  def notContains(user: User) = !(users contains user)
}