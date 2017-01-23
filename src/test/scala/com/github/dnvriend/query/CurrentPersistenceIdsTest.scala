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

import akka.persistence.query.EventEnvelope
import com.github.dnvriend.TestSpec

class CurrentPersistenceIdsTest extends TestSpec {

  it should "not find any events for unknown pid" in
    withCurrentEventsByPersistenceId()("unkown-pid", 0L, Long.MaxValue) { tp ⇒
      tp.request(Int.MaxValue)
      tp.expectComplete()
    }

  it should "find events from an offset" in {
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! 1
      actor1 ! 2
      actor1 ! 3
      actor1 ! 4

      eventually {
        countJournal shouldBe 4
      }

      withCurrentEventsByPersistenceId()("my-1", 2, 3) { tp ⇒
        tp.request(Int.MaxValue)
        tp.expectNext(EventEnvelope(2, "my-1", 2, 2))
        tp.expectNext(EventEnvelope(3, "my-1", 3, 3))
        tp.expectComplete()
      }
    }
  }

  it should "find events for actors" in
    withTestActors() { (actor1, actor2, actor3) ⇒
      actor1 ! 1
      actor1 ! 2
      actor1 ! 3

      eventually {
        countJournal shouldBe 7
      }

      withCurrentEventsByPersistenceId()("my-1", 1, 1) { tp ⇒
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(1, "my-1", 1, 1))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 2, 2) { tp ⇒
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(2, "my-1", 2, 2))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 3, 3) { tp ⇒
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(3, "my-1", 3, 3))
          .expectComplete()
      }

      withCurrentEventsByPersistenceId()("my-1", 2, 3) { tp ⇒
        tp.request(Int.MaxValue)
          .expectNext(EventEnvelope(2, "my-1", 2, 2), EventEnvelope(3, "my-1", 3, 3))
          .expectComplete()
      }
    }
}
