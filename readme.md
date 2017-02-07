Factory line challenge
-----------------------------

Stream processing code challenge. Your goal is to make all tests green by changing only FactoryService implementation (no cheating). You are free to use any library that you want (RX, Akka Streams, Scalaz-streams, etc).

### Task

You may consume orders stream and build present boxes with items from stream. After that you may check quality of boxes and put it in bad or good result stream. 

All orders and items must be consumed.

### Test run

Just: `sbt test`