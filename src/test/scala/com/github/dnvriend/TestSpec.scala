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

package com.github.dnvriend

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, PoisonPill, Props }
import akka.event.{ Logging, LoggingAdapter }
import akka.persistence.journal.Tagged
import akka.persistence.query.journal.leveldb.scaladsl.LeveldbReadJournal
import akka.persistence.query.{ EventEnvelope, PersistenceQuery }
import akka.stream.testkit.TestSubscriber
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ ActorMaterializer, Materializer }
import akka.testkit.TestProbe
import akka.util.Timeout
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach, FlatSpec, Matchers }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

trait TestSpec extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterAll with BeforeAndAfterEach with Eventually {
  implicit val system: ActorSystem = ActorSystem()
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  val log: LoggingAdapter = Logging(system, this.getClass)
  implicit val pc: PatienceConfig = PatienceConfig(timeout = 1.seconds)
  implicit val timeout = Timeout(30.seconds)

  val readJournal = PersistenceQuery(system).readJournalFor[LeveldbReadJournal](LeveldbReadJournal.Identifier)

  def randomId = UUID.randomUUID.toString.take(5)

  def terminate(actors: ActorRef*): Unit = {
    val tp = TestProbe()
    actors.foreach { (actor: ActorRef) ⇒
      tp watch actor
      actor ! PoisonPill
      tp expectTerminated actor
    }
  }

  implicit class PimpedFuture[T](self: Future[T]) {
    def toTry: Try[T] = Try(self.futureValue)
  }

  def setupEmpty(persistenceId: Int): ActorRef = {
    system.actorOf(Props(new TestActor(persistenceId)))
  }

  def clearEventStore(actors: ActorRef*): Future[Unit] = {
    import akka.pattern.ask
    Future.sequence(actors.map(_ ? TestActor.DeleteCmd())).map(_ ⇒ ())
  }

  def withTestActors()(f: (ActorRef, ActorRef, ActorRef) ⇒ Unit): Unit = {
    val actor1 = setupEmpty(1)
    val actor2 = setupEmpty(2)
    val actor3 = setupEmpty(3)
    try f(actor1, actor2, actor3) finally {
      //      clearEventStore(actor1, actor2, actor3).toTry should be a 'success
      terminate(actor1, actor2, actor3)
    }
  }

  def withTags(payload: Any, tags: String*) = Tagged(payload, Set(tags: _*))

  def withCurrentPersistenceIds(within: FiniteDuration = 1.second)(f: TestSubscriber.Probe[String] ⇒ Unit): Unit = {
    val tp = readJournal.currentPersistenceIds().filter(pid ⇒ (1 to 3).map(id ⇒ s"my-$id").contains(pid)).runWith(TestSink.probe[String])
    tp.within(within)(f(tp))
  }

  def withAllPersistenceIds(within: FiniteDuration = 1.second)(f: TestSubscriber.Probe[String] ⇒ Unit): Unit = {
    val tp = readJournal.allPersistenceIds().filter(pid ⇒ (1 to 3).map(id ⇒ s"my-$id").contains(pid)).runWith(TestSink.probe[String])
    tp.within(within)(f(tp))
  }

  def withCurrentEventsByPersistenceId(within: FiniteDuration = 1.second)(persistenceId: String, fromSequenceNr: Long = 0, toSequenceNr: Long = Long.MaxValue)(f: TestSubscriber.Probe[EventEnvelope] ⇒ Unit): Unit = {
    val tp = readJournal.currentEventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr).runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def withEventsByPersistenceId(within: FiniteDuration = 1.second)(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long)(f: TestSubscriber.Probe[EventEnvelope] ⇒ Unit): Unit = {
    val tp = readJournal.eventsByPersistenceId(persistenceId, fromSequenceNr, toSequenceNr).runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def withCurrentEventsByTag(within: FiniteDuration = 1.second)(tag: String, offset: Long)(f: TestSubscriber.Probe[EventEnvelope] ⇒ Unit): Unit = {
    val tp = readJournal.currentEventsByTag(tag, offset).runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def withEventsByTag(within: FiniteDuration = 1.second)(tag: String, offset: Long)(f: TestSubscriber.Probe[EventEnvelope] ⇒ Unit): Unit = {
    val tp = readJournal.eventsByTag(tag, offset).runWith(TestSink.probe[EventEnvelope])
    tp.within(within)(f(tp))
  }

  def currentEventsByTagAsList(tag: String, offset: Long): List[EventEnvelope] =
    readJournal.currentEventsByTag(tag, offset).runFold(List.empty[EventEnvelope])(_ :+ _).futureValue

  override protected def afterAll(): Unit = {
    println("===> " + deleteDirs)
    system.terminate().toTry should be a 'success
  }

  override protected def beforeEach(): Unit = {
  }

  override protected def beforeAll(): Unit = {
  }

  def countJournal: Long = {
    val numEvents = readJournal.currentPersistenceIds()
      .filter(pid ⇒ (1 to 3).map(id ⇒ s"my-$id").contains(pid))
      .mapAsync(1) { pid ⇒
        readJournal.currentEventsByPersistenceId(pid).map(_ ⇒ 1L).runFold(List.empty[Long])(_ :+ _).map(_.sum)
      }.runFold(List.empty[Long])(_ :+ _)
      .map(_.sum)
      .futureValue
    println("==> NumEvents: " + numEvents)
    numEvents
  }

  def deleteDirs: (Boolean, Boolean) = {
    def loop(dir: java.io.File): Unit = {
      Option(dir.listFiles).foreach(_.filter(_.isFile).foreach { file ⇒
        println(s"Deleting: ${file.getName}: ${file.delete}")
      })
    }
    val journalDir = new java.io.File("target/journal")
    val snapshotsDir = new java.io.File("target/snapshots")
    loop(journalDir)
    loop(snapshotsDir)
    (journalDir.delete(), snapshotsDir.delete())
  }
}
