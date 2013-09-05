# Step 5: Extending the Persistence Domain to Send Events

The event handler and the repositories you have made that now make up the persistence domain will react to events and persist or retrieve data on demand.

Another team in the The Yummy Noodle Bar project is building a system to provide real time notifications to clients (for example, to a user on the website) while an order is being processed and cooked.

Complicating matters, the Yummy Noodle Bar application is to be deployed as a cluster for high availability and scalability.  This means that a status update could be delivered on one application instance, while a client that should be notified is being managed by another instance.

To support this, you need to extend the persistence domain to provide notifications across all application instances every time the status of an order is updated.

To achieve this, you will use the Continuous Query feature of GemFire to generate update notification events on every application instance when a modification is made.

## Data Grids and Continuous Queries

In a traditional data store, an application would have to regularly poll to receive updated data.  This is inefficient, as many queries will be executed unnecessarily, and also introduces latency in between polls.  More realistically, applications will likely introduce some separate messaging infrastructure, such as [RabbitMQ](http://rabbitmq.com), to distribute notifications.

GemFire is a distributed data grid. It is naturally clustered and provides a feature called Continuous Querying; this allows you to register a GemFire Query with the cluster and for a simple POJO to receive events whenever a new piece of data is added that matches this query.

 
## Writing a continuous query

The outcome we seek is that whenever an OrderStatus instance is saved into GemFire, 
the method `OrderStatusUpdateService.setOrderStatus()` is called with the appropriate event.

Approach this in a highly test driven way.  This is quite a complex requirement, so some setup is required for the test before writing it.

Create a stub implementation of `OrderStatusUpdateService`. This stub will receive events and count them off against a [`CountDownLatch`](http://docs.oracle.com/javase/7/docs/api/java/util/concurrent/CountDownLatch.html) to ensure that the correct number of events are received in the given time. When all expected threads submit their countdown, the latch proceeds to completion.

    <@snippet path="src/test/java/com/yummynoodlebar/persistence/integration/fakecore/CountingOrderStatusService.java" prefix="complete" />

Next, create a new test-only Spring Configuration.  This will stand in the place of any Core domain Spring configuration.

    <@snippet path="src/test/java/com/yummynoodlebar/persistence/integration/fakecore/FakeCoreConfiguration.java" prefix="complete"/>

This is a standard `@Configuration`, simply creating a new bean instance of the type `OrderStatusUpdateService`.

With that infrastructure in place, it is possible to write the test.

    <@snippet path="src/test/java/com/yummynoodlebar/persistence/integration/OrderStatusNotificationsIntegrationTests.java" prefix="complete" />

The test is naturally multi-threaded.  A Continuous Query will operate in a thread controlled by the GemFire DataSource, and so we must assume that update events will be asynchronous.  This is the reason why a CountDownLatch is used rather than a more standard stub.  We require a way to synchronise behaviour across multiple threads, and control the timeout of the test to stop it hanging our full test execution.

Continuous Queries require a reference to a Spring bean that has a standardised method signature, of which there is a selection available.  A good compromise between ease of use and functionality is the following signature:

```java
void handleEvent(CqEvent event);
```

This bean is then called whenever the Query obtains some matching new data.

Create a new class `StatusUpdateGemfireNotificationListener` as follows:

    <@snippet path="src/main/java/com/yummynoodlebar/persistence/services/StatusUpdateGemfireNotificationListener.java"  prefix="complete"/>

This class transforms the GemFire CqEvent into a SetOrderStatusEvent to be consumed by the Core domain, and gains a reference to OrderStatusUpdateService.

Update GemfireConfiguration to create an instance of this bean:

    <@snippet path="src/main/java/com/yummynoodlebar/config/GemfireConfiguration.java" prefix="complete"/>

Now the Continuous Query itself may be implemented.  This is configured purely in the Spring XML configuration.

Open `client.xml` and alter it to read:

    <@snippet path="src/main/resources/gemfire/client.xml" prefix="complete"/>

This addition creates a new Continuous Query, with the given query being continuously evaluated.  Matching data is passed to the bean named `statusUpdateListener`, which was declared above in GemfireConfiguration.

This will use the DataSource created above, using the default name of **gemfireCache**.  This may be altered if desired.

First, run the local GemFire server:

```sh
$ ./gradlew run
```

Then execute `OrderStatusNotificationsIntegrationTests`. You can either run the test case inside your IDE, or use Gradle to invoke the test suite in another shell.

```sh
$ ./gradlew test
```

This indicates that events are being generated from OrderStatus instances being saved, and correctly transformed into event that are sent to OrderStatusUpdateService correctly.

## Summary

Congratulations, notifications about changing statuses are now being propagated across the application cluster, using GemFire.

[Next…  Recap and Where to go Next](../6/)


