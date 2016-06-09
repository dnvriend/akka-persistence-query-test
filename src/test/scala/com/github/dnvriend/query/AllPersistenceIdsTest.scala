/*
 * Copyright 2016 Dennis Vriend
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

import com.github.dnvriend.TestSpec
import scala.concurrent.duration._

class AllPersistenceIdsTest extends TestSpec {
  it should "not terminate the stream when there are not pids" in
    withAllPersistenceIds() { tp ⇒
      tp.request(Long.MaxValue)
      tp.expectNoMsg(100.millis)
      tp.cancel()
      tp.expectNoMsg(100.millis)
    }

  it should "find persistenceIds for actors" in
    withTestActors() { (actor1, actor2, actor3) ⇒
      withAllPersistenceIds() { tp ⇒
        tp.request(Int.MaxValue)

        countJournal shouldBe 0 // note, there are *no* events

        // curious, empty event store but the
        // read-journal knows about the persistent actors
        tp.expectNextPF {
          case "my-1" ⇒
          case "my-2" ⇒
          case "my-3" ⇒
        }
        tp.expectNextPF {
          case "my-1" ⇒
          case "my-2" ⇒
          case "my-3" ⇒
        }
        tp.expectNextPF {
          case "my-1" ⇒
          case "my-2" ⇒
          case "my-3" ⇒
        }
        tp.expectNoMsg(100.millis)

        actor1 ! 1
        eventually {
          countJournal shouldBe 1
        }

        tp.expectNoMsg(100.millis)

        actor2 ! 1
        eventually {
          countJournal shouldBe 2
        }
        tp.expectNoMsg(100.millis)

        actor3 ! 1
        eventually {
          countJournal shouldBe 3
        }
        tp.expectNoMsg(100.millis)

        actor1 ! 1
        eventually {
          countJournal shouldBe 4
        }

        tp.expectNoMsg(100.millis)

        actor2 ! 1
        eventually {
          countJournal shouldBe 5
        }

        tp.expectNoMsg(100.millis)

        actor3 ! 1
        eventually {
          countJournal shouldBe 6
        }

        tp.expectNoMsg(100.millis)

        tp.cancel()
        tp.expectNoMsg(100.millis)
      }
    }
}
