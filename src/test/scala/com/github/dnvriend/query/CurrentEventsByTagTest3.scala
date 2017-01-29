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
import akka.pattern.ask

abstract class CurrentEventsByTagTest3(config: String) extends TestSpec(config) {

  it should "find events from an offset" in {
    withTestActors() { (actor1, actor2, actor3) =>
      (for {
        _ <- actor1 ? withTags("a", "number2")
        _ <- actor2 ? withTags("b", "number2")
        _ <- actor3 ? withTags("c", "number2")
      } yield ()).toTry should be a 'success

      eventually {
        countJournal shouldBe 3
      }

      withCurrentEventsByTag()("number2", 0) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(Sequence(1), "my-1", 1, "a-1") => }
        tp.expectNextPF { case EventEnvelope(Sequence(2), "my-2", 1, "b-1") => }
        tp.expectNextPF { case EventEnvelope(Sequence(3), "my-3", 1, "c-1") => }
        tp.expectComplete()
      }

      withCurrentEventsByTag()("number2", 1) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(Sequence(2), "my-2", 1, "b-1") => }
        tp.expectNextPF { case EventEnvelope(Sequence(3), "my-3", 1, "c-1") => }
        tp.expectComplete()
      }

      withCurrentEventsByTag()("number2", 2) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(Sequence(3), "my-3", 1, "c-1") => }
        tp.expectComplete()
      }

      withCurrentEventsByTag()("number2", 3) { tp =>
        tp.request(Int.MaxValue)
        tp.expectComplete()
      }
    }
  }
}

class LevelDbCurrentEventsByTagTest3 extends CurrentEventsByTagTest3("application.conf")

class InMemoryCurrentEventsByTagTest3 extends CurrentEventsByTagTest3("inmemory.conf")