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

class CurrentEventsByTagTest3 extends TestSpec {

  it should "find events from an offset" in {
    withTestActors() { (actor1, actor2, actor3) =>
      actor1 ! withTags(1, "number2")
      actor2 ! withTags(2, "number2")
      actor3 ! withTags(3, "number2")

      eventually {
        countJournal shouldBe 3
      }

      withCurrentEventsByTag()("number2", 0) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(Sequence(1), _, _, _) => }
        tp.expectNextPF { case EventEnvelope(Sequence(2), _, _, _) => }
        tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
        tp.expectComplete()
      }

      withCurrentEventsByTag()("number2", 1) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(Sequence(2), _, _, _) => }
        tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
        tp.expectComplete()
      }

      withCurrentEventsByTag()("number2", 2) { tp =>
        tp.request(Int.MaxValue)
        tp.expectNextPF { case EventEnvelope(Sequence(3), _, _, _) => }
        tp.expectComplete()
      }

      withCurrentEventsByTag()("number2", 3) { tp =>
        tp.request(Int.MaxValue)
        tp.expectComplete()
      }
    }
  }
}
