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

abstract class CurrentEventsByPersistenceId1Test(config: String) extends TestSpec(config) {

  it should "not find any events for unknown pid" in
    withCurrentEventsByPersistenceId()("unkown-pid", 0L, Long.MaxValue) { tp =>
      tp.request(Int.MaxValue)
      tp.expectComplete()
    }

  it should "find events for actors" in
    withTestActors() { (actor1, _, _) =>
      List.fill(3)(sendMessage("a", actor1)).toTry should be a 'success

      withCurrentEventsByPersistenceId()("my-1", 0, 1) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, "a-1"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 1, 1) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, "a-1"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 1, 2) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, "a-1"))
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "a-2"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 2, 2) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "a-2"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 2, 3) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "a-2"))
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "a-3"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 3, 3) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "a-3"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 0, 3) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, "a-1"))
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "a-2"))
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "a-3"))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 1, 3) { tp =>
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(Sequence(1), "my-1", 1, "a-1"))
          .expectNext(EventEnvelope(Sequence(2), "my-1", 2, "a-2"))
          .expectNext(EventEnvelope(Sequence(3), "my-1", 3, "a-3"))
          .expectComplete()
      }
    }
}

class LevelDbCurrentEventsByPersistenceId1Test extends CurrentEventsByPersistenceId1Test("application.conf")

class InMemoryCurrentEventsByPersistenceId1Test extends CurrentEventsByPersistenceId1Test("inmemory.conf")