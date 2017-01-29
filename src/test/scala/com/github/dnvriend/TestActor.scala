/*
 * Copyright 2017 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import akka.actor.ActorRef
import akka.actor.Status.Success
import akka.event.LoggingReceive
import akka.persistence.{ DeleteMessagesSuccess, PersistentActor }
import akka.persistence.journal.Tagged

object TestActor {
  final case class DeleteCmd(toSequenceNr: Long = Long.MaxValue) extends Serializable
}

class TestActor(id: Int) extends PersistentActor {
  import TestActor._
  override val persistenceId: String = "my-" + id
  println("==> Created test actor: " + persistenceId)
  var state: Int = 1

  def deleteCmd(ref: ActorRef): Receive = {
    case DeleteMessagesSuccess(toSequenceNr) =>
      println(s"[$persistenceId]: Deleted: $toSequenceNr")
      ref ! Success(s"deleted-$toSequenceNr")
  }

  override def receiveCommand: Receive = LoggingReceive {
    case DeleteCmd(toSequenceNr) =>
      deleteMessages(toSequenceNr)
      println(s"[$persistenceId]: Deleting: $toSequenceNr")
      context.become(deleteCmd(sender()))

    case event @ Tagged(payload: Any, tags) =>
      persist(event.copy(payload = s"$payload-$state")) { _ =>
        increment()
        sender() ! event
      }

    case event =>
      persist(s"$event-$state") { _ =>
        increment()
        sender() ! event
      }

  }

  def increment(): Unit = state += 1

  override def receiveRecover: Receive = LoggingReceive {
    case event: String => increment()
  }
}
