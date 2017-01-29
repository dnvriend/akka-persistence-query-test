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

class CurrentEventsByTagTest5 extends TestSpec {

  it should "persist and find a tagged event with multiple tags" in
    withTestActors() { (actor1, actor2, actor3) =>
      withClue("Persisting multiple tagged events") {
        actor1 ! withTags(1, "one", "1", "prime")
        actor1 ! withTags(2, "two", "2", "prime")
        actor1 ! withTags(3, "three", "3", "prime")
        actor1 ! withTags(4, "four", "4")
        actor1 ! withTags(5, "five", "5", "prime")

        actor2 ! withTags(3, "three", "3", "prime")
        actor3 ! withTags(3, "three", "3", "prime")

        actor1 ! 1
        actor1 ! 1

        eventually {
          countJournal shouldBe 9
        }
      }

      withClue("query should find events for tag 'one'") {
        withCurrentEventsByTag()("one", 0) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(1), _, _, _) => }
          tp.expectComplete()
        }
      }

      withClue("query should find events for tag 'prime'") {
        withCurrentEventsByTag()("prime", 0) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(1), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(2), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(4), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(5), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(6), _, _, _) => }
          tp.expectComplete()
        }
        withCurrentEventsByTag()("prime", 1) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(2), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(4), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(5), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(6), _, _, _) => }
          tp.expectComplete()
        }
        withCurrentEventsByTag()("prime", 2) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(4), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(5), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(6), _, _, _) => }
          tp.expectComplete()
        }
        withCurrentEventsByTag()("prime", 3) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(4), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(5), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(6), _, _, _) => }
          tp.expectComplete()
        }
        withCurrentEventsByTag()("prime", 4) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(5), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(6), _, _, _) => }
          tp.expectComplete()
        }
        withCurrentEventsByTag()("prime", 5) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(6), _, _, _) => }
          tp.expectComplete()
        }
        withCurrentEventsByTag()("prime", 6) { tp =>
          tp.request(Int.MaxValue)
          tp.expectComplete()
        }
      }

      withClue("query should find events for tag '3'") {
        withCurrentEventsByTag()("3", 0) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(1), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(2), _, _, _) => }
          tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
          tp.expectComplete()
        }
      }

      withClue("query should find events for tag '3'") {
        withCurrentEventsByTag()("4", 0) { tp =>
          tp.request(Int.MaxValue)
          tp.expectNextPF { case EventEnvelope(Sequence(1), _, _, _) => }
          tp.expectComplete()
        }
      }
    }
}
