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

import akka.persistence.query.{ EventEnvelope, Sequence }
import com.github.dnvriend.TestSpec

import scala.concurrent.duration._

class EventsByTagTest extends TestSpec {
  it should "not find events for unknown tags" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "one")
      actor2 ! withTags(2, "two")
      actor3 ! withTags(3, "three")

      eventually {
        countJournal shouldBe 3
      }

      withEventsByTag()("unknown", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNoMsg(100.millis)
        tp.cancel()
        tp.expectNoMsg(100.millis)
      }
    }
  }

  it should "find all events by tag" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "number")
      actor2 ! withTags(2, "number")
      actor3 ! withTags(3, "number")

      eventually {
        countJournal shouldBe 6
      }

      withEventsByTag(within = 2.seconds)("number", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor1 ! withTags(4, "number")
        tp.expectNextPF { case EventEnvelope(4, _, _, _) ⇒ }
        tp.cancel()
        tp.expectNoMsg(100.millis)
      }
    }
  }

  it should "find events by tag from an offset" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "number2")
      actor2 ! withTags(2, "number2")
      actor3 ! withTags(3, "number2")

      eventually {
        countJournal shouldBe 10
      }

      currentEventsByTagAsList("number2", 0) should matchPattern {
        case List(EventEnvelope(1, _, _, _), EventEnvelope(2, _, _, _), EventEnvelope(3, _, _, _)) ⇒
      }

      currentEventsByTagAsList("number2", 1) should matchPattern {
        case List(EventEnvelope(1, _, _, _), EventEnvelope(2, _, _, _), EventEnvelope(3, _, _, _)) ⇒
      }

      currentEventsByTagAsList("number2", 2) should matchPattern {
        case List(EventEnvelope(2, _, _, _), EventEnvelope(3, _, _, _)) ⇒
      }

      currentEventsByTagAsList("number2", 3) should matchPattern {
        case List(EventEnvelope(3, _, _, _)) ⇒
      }

      currentEventsByTagAsList("number2", 4) should matchPattern {
        case Nil ⇒
      }

      withEventsByTag()("number2", 2) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor1 ! withTags(4, "number2")
        tp.expectNextPF { case EventEnvelope(4, _, _, _) ⇒ }
        tp.cancel()
        tp.expectNoMsg(100.millis)
      }
    }
  }

  it should "persist and find tagged event for one tag" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      withEventsByTag(10.seconds)("one2", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNoMsg(100.millis)

        actor1 ! withTags(1, "one2")
        tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor2 ! withTags(1, "one2")
        tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor3 ! withTags(1, "one2")
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor1 ! withTags(2, "two2")
        tp.expectNoMsg(100.millis)
        actor2 ! withTags(2, "two2")
        tp.expectNoMsg(100.millis)
        actor3 ! withTags(2, "two2")
        tp.expectNoMsg(100.millis)

        actor1 ! withTags(3, "one2")
        tp.expectNextPF { case EventEnvelope(4, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor2 ! withTags(3, "one2")
        tp.expectNextPF { case EventEnvelope(5, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        actor3 ! withTags(3, "one2")
        tp.expectNextPF { case EventEnvelope(6, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)

        tp.cancel()
        tp.expectNoMsg(100.millis)
      }
    }
  }

  it should "persist and find tagged events when stored with multiple tags" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! withTags(1, "one3", "1", "prime")
      actor1 ! withTags(2, "two3", "2", "prime")
      actor1 ! withTags(3, "three3", "3", "prime")
      actor1 ! withTags(4, "four", "4")
      actor1 ! withTags(5, "five", "5", "prime")
      actor2 ! withTags(3, "three3", "3", "prime")
      actor3 ! withTags(3, "three3", "3", "prime")

      actor1 ! 6
      actor1 ! 7
      actor1 ! 8
      actor1 ! 9
      actor1 ! 10

      eventually {
        countJournal shouldBe 32
      }

      withEventsByTag(10.seconds)("prime", 3) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(4, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(5, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(6, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag(10.seconds)("three3", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag(10.seconds)("3", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(2, _, _, _) ⇒ }
        tp.expectNextPF { case EventEnvelope(3, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }

      withEventsByTag(10.seconds)("one3", 0) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(1, _, _, _) ⇒ }
        tp.expectNoMsg(100.millis)
        tp.cancel()
      }
    }
  }

}
