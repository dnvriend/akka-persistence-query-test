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

import com.github.dnvriend.TestSpec

abstract class CurrentEventsByTagTest1(config: String) extends TestSpec(config) {

  it should "not find an event by tag for unknown tag" in {
    withTestActors() { (actor1, actor2, actor3) =>
      List(
        sendMessage(withTags("a", "one"), actor1),
        sendMessage(withTags("a", "two"), actor2),
        sendMessage(withTags("a", "three"), actor3)
      ).toTry should be a 'success

      withCurrentEventsByTag()("unknown", 0) { tp =>
        tp.request(Int.MaxValue)
        tp.expectComplete()
      }
    }
  }
}

class LevelDbCurrentEventsByTagTest1 extends CurrentEventsByTagTest1("application.conf")

class InMemoryCurrentEventsByTagTest1 extends CurrentEventsByTagTest1("inmemory.conf")