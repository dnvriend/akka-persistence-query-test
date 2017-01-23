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

package com.github.dnvriend.query

import akka.persistence.query.{ EventEnvelope2, Sequence }
import com.github.dnvriend.TestSpec

import scala.concurrent.duration._

abstract class EventsByTagTest2(config: String) extends TestSpec(config) {

  it should "find events by tag from an offset using Offset interface " in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "number3")
      actor2 ! withTags(2, "number3")
      actor3 ! withTags(3, "number3")

      eventually {
        countJournal shouldBe 3
      }

      currentEventsByTagAsList2("number3", Sequence(0)) should matchPattern {
        case List(EventEnvelope2(Sequence(1), _, _, _), EventEnvelope2(Sequence(2), _, _, _), EventEnvelope2(Sequence(3), _, _, _)) ⇒
      }

      currentEventsByTagAsList2("number3", Sequence(1)) should matchPattern {
        case List(EventEnvelope2(Sequence(1), _, _, _), EventEnvelope2(Sequence(2), _, _, _), EventEnvelope2(Sequence(3), _, _, _)) ⇒
      }

      currentEventsByTagAsList2("number3", Sequence(2)) should matchPattern {
        case List(EventEnvelope2(Sequence(2), _, _, _), EventEnvelope2(Sequence(3), _, _, _)) ⇒
      }

      currentEventsByTagAsList2("number3", Sequence(3)) should matchPattern {
        case List(EventEnvelope2(Sequence(3), _, _, _)) ⇒
      }

      currentEventsByTagAsList2("number3", Sequence(4)) should matchPattern {
        case Nil ⇒
      }

      withEventsByTag2()("number3", Sequence(0)) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope2(Sequence(1), _, _, _) => }
        tp.expectNextPF { case EventEnvelope2(Sequence(2), _, _, _) => }
        tp.expectNextPF { case EventEnvelope2(Sequence(3), _, _, _) => }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag2()("number3", Sequence(1)) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope2(Sequence(1), _, _, _) => }
        tp.expectNextPF { case EventEnvelope2(Sequence(2), _, _, _) => }
        tp.expectNextPF { case EventEnvelope2(Sequence(3), _, _, _) => }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag2()("number3", Sequence(2)) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope2(Sequence(2), _, _, _) => }
        tp.expectNextPF { case EventEnvelope2(Sequence(3), _, _, _) => }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag2()("number3", Sequence(3)) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope2(Sequence(3), _, _, _) => }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag2()("number3", Sequence(4)) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag2()("number3", Sequence(4)) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNoMsg(100.millis)

        // new event
        actor1 ! withTags(4, "number3")
        tp.expectNextPF { case EventEnvelope2(Sequence(4), _, _, _) => }
        tp.cancel()
      }
    }
  }
}

class LevelDbEventsByTagTest2 extends EventsByTagTest2("application.conf")

class InMemoryEventsByTagTest2 extends EventsByTagTest2("inmemory.conf")

class JdbcEventsByTagTest2 extends EventsByTagTest2("jdbc.conf")

class CassandraEventsByTagTest2 extends EventsByTagTest2("cassandra.conf")