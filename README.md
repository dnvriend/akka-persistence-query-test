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


## Akka Persistence 2.4 -> 2.5 Migration Guide
### Removal of PersistentView
After being deprecated for a long time, and replaced by Persistence Query, `PersistentView` has now been removed.

The consuming actor may be a plain Actor or an PersistentActor if it needs to store its own state (e.g. fromSequenceNr offset).

Please note that Persistence Query is __not__ experimental anymore in Akka 2.5.0, so you can safely upgrade to it
(albeit there is no TCK so no way to uniformly test the implementations).

### Persistence Plugin Proxy
A new persistence plugin proxy was added, that allows sharing of an otherwise non-sharable journal or snapshot store. 
The proxy is available by setting `akka.persistence.journal.plugin` or `akka.persistence.snapshot-store.plugin` to 
`akka.persistence.journal.proxy` or `akka.persistence.snapshot-store.proxy`, respectively. 
The proxy supplants the Shared LevelDB journal.

### Persistence Query
Persistence Query has been promoted to a stable module. Only slight API changes were made since the module was introduced:

### Query naming consistency improved
Queries always fall into one of the two categories: infinite or finite ("current"). The naming convention for these 
categories of queries was solidified and is now as follows:

- __"infinite"__ - e.g. eventsByTag, persistenceIds - which will keep emitting events as they are persisted and match the query.
- __"finite"__, also known as "current" - e.g. currentEventsByTag, currentPersistenceIds - which will complete the stream once the query completed, for the journal's definition of "current". For example in an SQL store it would mean it only queries the database once.

### AllPersistenceIdsQuery Change 
Only the `AllPersistenceIdsQuery` class and method name changed due to this. The class is now called `PersistenceIdsQuery`, 
and the method which used to be allPersistenceIds is now `persistenceIds`.

### Queries now use Offset instead of Long for offsets
This change was made to better accomodate the various types of Journals and their understanding what an offset is. 
For example, in some journals an offset is always a time, while in others it is a numeric offset (like a sequence id).

Instead of the previous Long offset you can now use the provided Offset factories (and types):

- akka.persistence.query.Offset.sequence(value: Long),
- akka.persistence.query.Offset.timeBasedUUID(value: UUID)
- and finally NoOffset if not offset should be used.

Journals are also free to provide their own specific Offset types. Consult your journal plugin's documentation for details.



## Resources
- [Akka 2.5-M1](http://akka.io/news/2017/01/26/akka-2.5-M1-released.html)
- [Akka 2.4 -> 2.5 Migration Guide](http://doc.akka.io/docs/akka/2.5-M1/project/migration-guide-2.4.x-2.5.x.html?_ga=1.105859075.164285114.1438178939)

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