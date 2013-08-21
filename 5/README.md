## Extending the Persistence Domain to Send Events

The event handler and the repositories you have made that now make up the persistence domain will react to events and persist or retrieve data on demand.

Another team in the The Yummy Noodle Bar project is building a system to provide real time notifications to clients (for example, to a user on the website) while an order is being processed and cooked.

Complicating matters, the Yummy Noodle Bar application is to be deployed as a cluster for high availability and scalability.  This means that a status update could be delivered on one application instance, while a client that should be notified is being managed by another instance.

To support this, you need to extend the persistence domain to provide notifications across all application instances every time the status of an order is updated.

To achieve this, you will use the Continuous Query feature of Gemfire to generate update notification events on every application instance when a modification is made.

### Data Grids and Continuous Queries

In a traditional data store, an application would have to regularly poll to receive updated data.  This is inefficient, as many queries will be executed unecessarily, and also introduces latency in between polls.  More realistically, applications will likely introduce some seperate messaging infrastructure, such as RabbitMQ to distribute notifications.

Gemfire is a distributed data grid, it is naturally clustered and provides a feature called Continuous Querying; this allows you to register a Gemfire Query with the cluster and for a simple POJO to receive events whenever a new piece of data is added that matches this query.

 
### Writing a continuous query

The outcome we require is that whenever an OrderStatus instance is saved into Gemfire, 
the method `com.yummynoodlebar.core.services.OrderStatusUpdateService.setOrderStatus()` is called with the appropriate event.

Approach this in a highly test driven way.  This is quite a complex requirement, so some setup is required for the test before writing it.

Create a stub implementation of `OrderStatusUpdateService`. This stub will receive events and count them off against a `javax.concurrent.CountDownLatch` to ensure that the correct number of events are recieved in the given time.

```java
package com.yummynoodlebar.persistence.integration.fakecore;

import com.yummynoodlebar.core.services.OrderStatusUpdateService;
import com.yummynoodlebar.events.orders.OrderStatusEvent;
import com.yummynoodlebar.events.orders.SetOrderStatusEvent;

import java.util.concurrent.CountDownLatch;

/**
 * A testing Stub.
 * Stands in the place of the Core OrderStatusUpdateService. Allows tests to count
 * the number of update status events via a CountDownLatch, which will be set manually
 * in the test
 */
public class CountingOrderStatusService implements OrderStatusUpdateService {

  private CountDownLatch latch;

  public void setLatch(CountDownLatch latch) {
    this.latch = latch;
  }

  @Override
  public OrderStatusEvent setOrderStatus(SetOrderStatusEvent orderStatusEvent) {
    latch.countDown();
    return OrderStatusEvent.notFound(orderStatusEvent.getKey());
  }
}
```
Next, create a new test only Spring Configuration.  This will stand in the place of the any Core domain Spring configuration.

```java
package com.yummynoodlebar.persistence.integration.fakecore;

import com.yummynoodlebar.core.services.OrderStatusUpdateService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FakeCoreConfiguration {

  @Bean OrderStatusUpdateService orderStatusUpdateService() {
    return new CountingOrderStatusService();
  }
}
```

This is a standard `@Configuration`, simply creating a new bean instance of the type `OrderStatusUpdateService`.

With that infrastructure in place, it is possible to write the test.

```java
package com.yummynoodlebar.persistence.integration;

import com.yummynoodlebar.config.GemfireConfiguration;
import com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture;
import com.yummynoodlebar.persistence.integration.fakecore.CountingOrderStatusService;
import com.yummynoodlebar.persistence.integration.fakecore.FakeCoreConfiguration;
import com.yummynoodlebar.persistence.repository.OrderStatusRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {FakeCoreConfiguration.class, GemfireConfiguration.class})
public class OrderStatusNotificationsIntegrationTests {

  @Autowired
  OrderStatusRepository ordersStatusRepository;

  @Autowired
  CountingOrderStatusService orderStatusUpdateService;

  @Before
  public void setup() {
    ordersStatusRepository.deleteAll();
  }

  @After
  public void teardown() {
    ordersStatusRepository.deleteAll();
  }

  @Test
  public void thatCQNotificationsPropogateToCore() throws Exception {

    CountDownLatch countdown = new CountDownLatch(3);
    orderStatusUpdateService.setLatch(countdown);

    UUID orderId = UUID.randomUUID();

    ordersStatusRepository.save(PersistenceFixture.orderReceived(orderId));
    ordersStatusRepository.save(PersistenceFixture.orderReceived(orderId));
    ordersStatusRepository.save(PersistenceFixture.orderReceived(orderId));

    boolean completedWithinTimeout = countdown.await(5, TimeUnit.SECONDS);

    assertTrue("Did not send enough notifications within timeout", completedWithinTimeout);
  }
}
```

The test is naturally multi threaded.  A Continuous Query will operate in a thread controlled by the Gemfire DataSource, and so we must assume that update events will be asynchronous.  This is the reason why a CountDownLatch is used rather than a more standard stub.  We require a way to synchronise behaviour across multiple threads, and control the timeout of the test to stop it hanging our full test execution.

Continuous Queries require a reference to a spring bean that has a standardised method signature, of which there is a selection available.  A good compromise between ease of use and functionality is the signature.

```java
void handleEvent(CqEvent event);
```

This bean is then called whenever the Query obtains some new data that matches.

Create a new class `com.yummynoodlebar.persistence.services.StatusUpdateGemfireNotificationListener`

```java
package com.yummynoodlebar.persistence.services;

import com.gemstone.gemfire.cache.query.CqEvent;
import com.yummynoodlebar.core.services.OrderStatusUpdateService;
import com.yummynoodlebar.events.orders.SetOrderStatusEvent;
import com.yummynoodlebar.persistence.domain.OrderStatus;
import org.springframework.beans.factory.annotation.Autowired;

public class StatusUpdateGemfireNotificationListener {

  @Autowired
  private OrderStatusUpdateService orderStatusUpdateService;

  public void setOrderStatusUpdateService(OrderStatusUpdateService orderStatusUpdateService) {
    this.orderStatusUpdateService = orderStatusUpdateService;
  }

  public void handleEvent(CqEvent event) {

    if (!event.getBaseOperation().isCreate()) {
      return;
    }

    OrderStatus status = (OrderStatus) event.getNewValue();

    orderStatusUpdateService.setOrderStatus(
        new SetOrderStatusEvent(status.getOrderId(),
                                status.toStatusDetails()));

  }
}
```

This class transforms the Gemfire CqEvent into a SetOrderStatusEvent to be consumed by the Core domain, and gains a reference to OrderStatusUpdateService.

Update GemfireConfiguration to create an instance of this bean

```java
package com.yummynoodlebar.config;

import com.yummynoodlebar.persistence.repository.OrderStatusRepository;
import com.yummynoodlebar.persistence.services.StatusUpdateGemfireNotificationListener;
import org.springframework.context.annotation.*;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ImportResource({"classpath:gemfire/client.xml"})
@EnableTransactionManagement
@EnableGemfireRepositories(basePackages = "com.yummynoodlebar.persistence.repository",
    includeFilters = @ComponentScan.Filter(value = {OrderStatusRepository.class}, type = FilterType.ASSIGNABLE_TYPE))
public class GemfireConfiguration {

  @Bean public StatusUpdateGemfireNotificationListener statusUpdateListener() {
    return new StatusUpdateGemfireNotificationListener();
  }
}
```

Now, the Continuous Query itself may be implemented.  This is configured purely in the Spring XML configuration.

Open `resources/gemfire/client.xml` and alter it to read

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:cache="http://www.springframework.org/schema/cache"
       xmlns:gfe="http://www.springframework.org/schema/gemfire"
       xmlns:context="http://www.springframework.org/schema/context"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:gfe-data="http://www.springframework.org/schema/data/gemfire"
       xsi:schemaLocation="http://www.springframework.org/schema/gemfire http://www.springframework.org/schema/gemfire/spring-gemfire.xsd
        http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/cache http://www.springframework.org/schema/cache/spring-cache.xsd
		http://www.springframework.org/schema/data/gemfire http://www.springframework.org/schema/data/gemfire/spring-data-gemfire.xsd
		http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">

    <tx:annotation-driven/>

    <gfe-data:datasource subscription-enabled="true">
        <gfe-data:server host="localhost" port="40404" />
    </gfe-data:datasource>

    <bean id="yummyTemplate" class="org.springframework.data.gemfire.GemfireTemplate">
        <property name="region" ref="YummyNoodleOrder"/>
    </bean>

    <gfe:transaction-manager/>

    <gfe:cq-listener-container id="transactionManager">
        <gfe:listener ref="statusUpdateListener" query="SELECT * from /YummyNoodleOrder" />
    </gfe:cq-listener-container>

</beans>
```

This addition creates a new Continuous Query, with the given query being continuously evaluated.  Matching data is passed to the bean named `statusUpdateListener`, which was declared above in GemfireConfiguration 

This will use the datasource created above, using the default name of gemfireCache.  This may be altered if desired.

First, run the local Gemfire server

    ./gradlew run
    
Then execute `OrderStatusNotificationsIntegrationTests`

This indicates that events are being generated from OrderStatus instances being saved, and correctly transformed into event that are sent to OrderStatusUpdateService correctly.

### Next Steps

Congratulations, notifications about changing statuses are now being propogated across the application cluster, using Gemfire.

[Next…  Recap and Where to go Next](../7/)


