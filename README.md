# akka-persistence-query-test
Study on the [akka-persistence-query][query] API. As there is no Technology Compatibility Kit (TCK) as of yet for [akka-persistence-query][query], I will be looking at the [leveldb][query-leveldb] implementation that comes out of the box with akka and assert the behavior so that I can replicate it in the [akka-persistence-inmemory][inmemory] and [akka-persistence-jdbc][jdbc] plugins. 

## Conclusions
The following are my findings.

## CurrentPersistenceIds API
- Empty journal: Apparently the leveldb query API is magical, with an empty journal (no events), it still can determine 
the persistenceIds of any running persistent actor, this is something that cannot be reproduced with the [inmemory plugin][inmemory] 
or the [jdbc plugin][jdbc],
- The emitted element type is `String`,
- PersistenceIds, are streamed in a non-deterministic order,
- PersistenceIds are emitted only once, so the API is stateful.

## EventsByPersistenceIds
- The emitted element is [akka.persistence.query.EventEnvelope][eventenvelope]

```scala
final case class EventEnvelope(
  offset:        Long,
  persistenceId: String,
  sequenceNr:    Long,
  event:         Any)
```

- The _EventEnvelope_ has a field called `offset` which is kind of confusing. With the `eventsByPersistenceIds`, the offset
value is the same as the *event* sequenceNr. This behavior is different from the _byTag_ queries. 
- Using the from/to sequenceNr fields in the query, the following table applies when there are three events:

from | to  | result
---- | --- | ------
0 | 1 | EventEnvelope(1, persistenceId, 1, event)
1 | 1 | EventEnvelope(1, persistenceId, 1, event)
1 | 2 | EventEnvelope(1, persistenceId, 1, event), EventEnvelope(2, persistenceId, 2, event)
2 | 2 | EventEnvelope(2, persistenceId, 2, event)
2 | 3 | EventEnvelope(2, persistenceId, 2, event), EventEnvelope(3, persistenceId, 3, event)
3 | 3 | EventEnvelope(3, persistenceId, 3, event)
0 | 3 | EventEnvelope(1, persistenceId, 1, event), EventEnvelope(2, persistenceId, 2, event), EventEnvelope(3, persistenceId, 3, event)
1 | 3 | EventEnvelope(1, persistenceId, 1, event), EventEnvelope(2, persistenceId, 2, event), EventEnvelope(3, persistenceId, 3, event)

EventsByPersistenceId should terminate when the toSequenceNr has been reached.
Also, but not implemented by the levelDb journal, it should also terminate when the toSeqnr is equal to zero (0)

## EventsByTag API
- The emitted element is [akka.persistence.query.EventEnvelope][eventenvelope],
- The field `offset` is a generated number that is added to the event stream that tags that emitted event with a
unique number for that query,
- Using the offset field in the query, the following table applies when three events match the query and is inclusive:

offset | result 
------ | ------
0 | EventEnvelope(1, _, _, _), EventEnvelope(2, _, _, _), EventEnvelope(3, _, _, _)
1 | EventEnvelope(1, _, _, _), EventEnvelope(2, _, _, _), EventEnvelope(3, _, _, _)
2 | EventEnvelope(2, _, _, _), EventEnvelope(3, _, _, _)
3 | EventEnvelope(3, _, _, _)
4 | No events are emitted

## Cassandra
To test with cassandra, execute the following in sbt:

- lagomCassandraStart
- testOnly *CassandraEventsByTagTest2
- lagomCassandraStop

There is only one problem; Cassandra does not support the Sequence offset type.

## What's new?

## 1.0.0 (2016-06-09)
  - Initial

Have fun!

[akka]: http://akka.io/
[scala]: http://www.scala-lang.org/
[query]: http://doc.akka.io/docs/akka/current/scala/persistence-query.html
[query-leveldb]: http://doc.akka.io/docs/akka/current/scala/persistence-query-leveldb.html
[inmemory]: https://github.com/dnvriend/akka-persistence-inmemory
[jdbc]: https://github.com/dnvriend/akka-persistence-jdbc
[eventenvelope]: https://github.com/akka/akka/blob/4acc1cca6a27be0ff80f801de3640f91343dce94/akka-persistence-query/src/main/scala/akka/persistence/query/EventEnvelope.scala