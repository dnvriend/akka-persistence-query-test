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

abstract class CurrentEventsByPersistenceIdDeleteEventsTest(config: String) extends TestSpec(config) {

  it should "not show deleted events in event stream" in {
    withTestActors() { (actor1, _, _) =>
      sendMessage("a", actor1).toTry should be a 'success
      sendMessage("b", actor1).toTry should be a 'success
      sendMessage("c", actor1).toTry should be a 'success

      deleteEvents(actor1, 0).toTry should be a 'success

      withCurrentEventsByPersistenceId()("my-1", 0) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, "a-1"))
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "b-2"))
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "c-3"))
          .expectComplete()
      }

      deleteEvents(actor1, 1).toTry should be a 'success

      withCurrentEventsByPersistenceId()("my-1", 0) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "b-2"))
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "c-3"))
          .expectComplete()
      }

      deleteEvents(actor1, 2).toTry should be a 'success

      withCurrentEventsByPersistenceId()("my-1", 0) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "c-3"))
          .expectComplete()
      }

      deleteEvents(actor1, 3).toTry should be a 'success

      withCurrentEventsByPersistenceId()("my-1", 0) { tp =>
        tp.request(Int.MaxValue)
          .expectComplete()
      }
    }
  }
}

class LevelDbCurrentEventsByPersistenceIdDeleteEventsTest extends CurrentEventsByPersistenceIdDeleteEventsTest("application.conf")

class InmemoryCurrentEventsByPersistenceIdDeleteEventsTest extends CurrentEventsByPersistenceIdDeleteEventsTest("inmemory.conf")
