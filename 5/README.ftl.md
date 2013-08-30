# Extending the Persistence Domain to Send Events

The event handler and the repositories you have made that now make up the persistence domain will react to events and persist or retrieve data on demand.

Another team in the The Yummy Noodle Bar project is building a system to provide real time notifications to clients (for example, to a user on the website) while an order is being processed and cooked.

Complicating matters, the Yummy Noodle Bar application is to be deployed as a cluster for high availability and scalability.  This means that a status update could be delivered on one application instance, while a client that should be notified is being managed by another instance.

To support this, you need to extend the persistence domain to provide notifications across all application instances every time the status of an order is updated.

To achieve this, you will use the Continuous Query feature of GemFire to generate update notification events on every application instance when a modification is made.

## Data Grids and Continuous Queries

In a traditional data store, an application would have to regularly poll to receive updated data.  This is inefficient, as many queries will be executed unecessarily, and also introduces latency in between polls.  More realistically, applications will likely introduce some seperate messaging infrastructure, such as RabbitMQ to distribute notifications.

Gemfire is a distributed data grid, it is naturally clustered and provides a feature called Continuous Querying; this allows you to register a Gemfire Query with the cluster and for a simple POJO to receive events whenever a new piece of data is added that matches this query.

 
## Writing a continuous query

The outcome we require is that whenever an OrderStatus instance is saved into Gemfire, 
the method `com.yummynoodlebar.core.services.OrderStatusUpdateService.setOrderStatus()` is called with the appropriate event.

Approach this in a highly test driven way.  This is quite a complex requirement, so some setup is required for the test before writing it.

Create a stub implementation of `OrderStatusUpdateService`. This stub will receive events and count them off against a `javax.concurrent.CountDownLatch` to ensure that the correct number of events are received in the given time.

    <@snippet "src/test/java/com/yummynoodlebar/persistence/integration/fakecore/CountingOrderStatusService.java" />

Next, create a new test-only Spring Configuration.  This will stand in the place of the any Core domain Spring configuration.

    <@snippet "src/test/java/com/yummynoodlebar/persistence/integration/fakecore/FakeCoreConfiguration.java" />

This is a standard `@Configuration`, simply creating a new bean instance of the type `OrderStatusUpdateService`.

With that infrastructure in place, it is possible to write the test.

    <@snippet "src/test/java/com/yummynoodlebar/persistence/integration/OrderStatusNotificationsIntegrationTests.java" />

The test is naturally multi-threaded.  A Continuous Query will operate in a thread controlled by the GemFire DataSource, and so we must assume that update events will be asynchronous.  This is the reason why a CountDownLatch is used rather than a more standard stub.  We require a way to synchronise behaviour across multiple threads, and control the timeout of the test to stop it hanging our full test execution.

Continuous Queries require a reference to a spring bean that has a standardised method signature, of which there is a selection available.  A good compromise between ease of use and functionality is the signature.

```java
void handleEvent(CqEvent event);
```

This bean is then called whenever the Query obtains some new data that matches.

Create a new class `com.yummynoodlebar.persistence.services.StatusUpdateGemfireNotificationListener`

    <@snippet "src/main/java/com/yummynoodlebar/persistence/services/StatusUpdateGemfireNotificationListener.java" />

This class transforms the GemFire CqEvent into a SetOrderStatusEvent to be consumed by the Core domain, and gains a reference to OrderStatusUpdateService.

Update GemfireConfiguration to create an instance of this bean:

    <@snippet "src/main/java/com/yummynoodlebar/config/GemfireConfiguration.java" />

Now the Continuous Query itself may be implemented.  This is configured purely in the Spring XML configuration.

Open `resources/gemfire/client.xml` and alter it to read:

    <@snippet "src/main/resources/gemfire/client.xml" />

This addition creates a new Continuous Query, with the given query being continuously evaluated.  Matching data is passed to the bean named `statusUpdateListener`, which was declared above in GemfireConfiguration.

This will use the datasource created above, using the default name of gemfireCache.  This may be altered if desired.

First, run the local GemFire server

    ./gradlew run
    
Then execute `OrderStatusNotificationsIntegrationTests`

This indicates that events are being generated from OrderStatus instances being saved, and correctly transformed into event that are sent to OrderStatusUpdateService correctly.

## Summary

Congratulations, notifications about changing statuses are now being propogated across the application cluster, using Gemfire.

[Next…  Recap and Where to go Next](../6/)


