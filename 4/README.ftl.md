 
# Storing the Order Status in Gemfire using Spring Data Gemfire

In the Yummy Noodle Bar application, the statuses of Orders will be stored in Gemfire.
These statuses will be coming into the application from the kitchen and order processing side of the business, as opposed to the orders themselves that will come from the system that accepts orders from clients.

## About Gemfire

Gemfire is a high performance distributed data grid.  It scales from a small embedded cache implementation to large scale wide area network implementations with data residency and access control.

Spring Data allows the creation of both server and client connections, data access, caching and deep integration with the Spring Application Context.

You will see here the creation of a Spring Data interface to a Gemfire server. The next section of the tutorial shows the extension of this functionality to cover the use of Continuous Queries and how to integrate those into the Yummy Noodle application.

## Import Spring Data Gemfire

In build.gradle, add the following

```groovy
repositories {
  mavenCentral()
  maven { url 'http://repo.springsource.org/libs-release'}
}

dependencies {
  ...
  compile 'org.springframework.data:spring-data-gemfire:1.3.2.RELEASE'
  ...
}
```

This Maven-style repository is required to access the gemfire libraries, which are not available from maven central.

## Run a Gemfire Cache Server

In order to run the tests and perform Gemfire development, it is necessary to have access to a Gemfire server.

While it would be possible to download a full distribution, configure and run that, for the purposes of this tutorial, it is preferable to set up a server within this project.

In build.gradle, add to the end of the file

```groovy
task run(type: JavaExec) {
  description = 'Runs a simple Gemfire Cache Server'
  main = "com.yummynoodlebar.persistence.services.LocalGemfireServer"
  classpath = sourceSets.main.runtimeClasspath
  standardInput = System.in
}
```

Create a new XML file in src/main/resources/server

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xmlns:cache="http://www.springframework.org/schema/cache"
	xmlns:gfe="http://www.springframework.org/schema/gemfire"
	xmlns:context="http://www.springframework.org/schema/context"
	xmlns:util="http://www.springframework.org/schema/util"
	xmlns:gfe-data="http://www.springframework.org/schema/data/gemfire"
	xsi:schemaLocation="http://www.springframework.org/schema/gemfire http://www.springframework.org/schema/gemfire/spring-gemfire.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/util http://www.springframework.org/schema/util/spring-util.xsd
		http://www.springframework.org/schema/cache http://www.springframework.org/schema/cache/spring-cache.xsd
		http://www.springframework.org/schema/data/gemfire http://www.springframework.org/schema/data/gemfire/spring-data-gemfire.xsd
		http://www.springframework.org/schema/context http://www.springframework.org/schema/context/spring-context.xsd">
    
    
    <gfe:cache properties-ref="gemfire-props"/>
    
    <util:properties id="gemfire-props">
        <prop key="log-level">warning</prop>
    </util:properties>
     
    <gfe:cache-server/>
     
    <gfe:replicated-region id="YummyNoodleOrder">
    </gfe:replicated-region>
</beans>
```

This configures a basic Gemfire server, and creates a *Region*, a logical partition within Gemfire, that we have named 'YummyNoodleOrder'.

Lastly, create the driving class `com.yummynoodlebar.persistence.services.LocalGemfireServer`

```java
package com.yummynoodlebar.persistence.services;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

public class LocalGemfireServer {
  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws IOException,
      InterruptedException {
    ApplicationContext context = new ClassPathXmlApplicationContext("server/cache-config.xml");

    System.out.println("Press <ENTER> to terminate the cache server");
    System.in.read();
    System.exit(0);
  }
}
```

You may now start a Gemfire server (on port 40404) by running 

    ./gradlew run

This server will have access to the classpath of the project, most notably the OrderStatus class.  It is necessary for the Gemfire server to have access to this class if we want to persist it within the grid.  When you create a standalone Gemfire grid, you will need to provide any classes you wish to persist within a jar file on the classpath of every gemfire server.

This tutorial does not show how to set up an embedded Gemfire node.  While it would be possible to run much of this section of the tutorial using an embedded node, the next section requires the use of a seperate Gemfire server.

## Start with a (failing) test, introducing Gemfire Template

In a similar way as with MongoDB and JPA, the first test you will write is to check that OrderStatus can be correctly persisted into Gemfire.

Firstly, create a new empty class `com.yummynoodlebar.config.GemfireConfiguration`. This is a placeholder for the configuration until we have written a test.

Create a new test `com.yummynoodlebar.persistence.integration.OrderStatusMappingIntegrationTests` with the content.

```java
package com.yummynoodlebar.persistence.integration;

import com.gemstone.gemfire.GemFireCheckedException;
import com.gemstone.gemfire.GemFireException;
import com.gemstone.gemfire.cache.Region;
import com.yummynoodlebar.config.GemfireConfiguration;
import com.yummynoodlebar.persistence.domain.OrderStatus;
import com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.gemfire.GemfireCallback;
import org.springframework.data.gemfire.GemfireOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;

import static junit.framework.TestCase.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GemfireConfiguration.class})
public class OrderStatusMappingIntegrationTests {

  @Resource(name = "yummyTemplate")
  GemfireOperations yummyTemplate;

  @Before
  public void setup() {
    clear();
  }
  @After
  public void teardown() {
    clear();
  }
  public void clear() {
    yummyTemplate.execute(new GemfireCallback<Object>() {
      @Override
      public Object doInGemfire(Region<?, ?> region) throws GemFireCheckedException, GemFireException {
        region.clear();
        return "completed";
      }
    });
  }

  @Test
  public void thatItemCustomMappingWorks() throws Exception {
    OrderStatus status = PersistenceFixture.startedCooking();

    yummyTemplate.put(4L, status);

    OrderStatus results = yummyTemplate.findUnique("SELECT * from /YummyNoodleOrder");

    System.out.println("Found " + results.getId());

    assertEquals(status.getId(), results.getId());
    assertEquals(status.getOrderId(), results.getOrderId());
    assertEquals(status.getStatus(), results.getStatus());
  }
}
```

This test uses `GemfireTemplate`, seen in this test via its API interface `GemfireOperations`.  This follows the same pattern as other Spring Template classes, exposing the most common operations using consistent, simple methods, and also providing access to the low level Gemfire API in a managed way via callbacks.

You can see the access to the low level Gemfire API in the clear() method.  This accesses the Region instance and clears it of all data.  Region implements the Map interface, as it is also conceptually a Map.  Gemfire provides many features around this core concept, but you can see the map usage in the test method itself 

```java
    yummyTemplate.put(4L, status);
```
GemfireTemplate exposes a Map oriented method to interact with its configured region.

The test inserts a single OrderStatus into the Gemfire Region and then performs a query, using the Gemfire Object Query Language (OQL).  This is a declarative language conceptually similar to the JPA Query Language/ Hibernate Query Language, providing a syntax to query against a set of Objects and their properties and perform selections, ordering, grouping and projections against the results.

Now that a test is in place, implement GemfireConfiguration with the content

```java
package com.yummynoodlebar.config;

import org.springframework.context.annotation.*;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ImportResource({"classpath:gemfire/client.xml"})
@EnableTransactionManagement
public class GemfireConfiguration {}
```

This class is mainly used, at the moment, to allow the consistent use of Spring Java Configuration in tests and other context creation.  It will be extended below and in the next tutorial section.
Currently, the important line is

```java
@ImportResource({"classpath:gemfire/client.xml"})
```

This imports a traditional XML based Spring configuration.  Currently, Spring Data Gemfire is significantly easier to configure using XML, and certain features are not yet fully implemented.  For this reason, XML configuration is still recommended for Spring Data Gemfire.

Create a new file `src/main/resources/gemfire/client.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
       xmlns:gfe="http://www.springframework.org/schema/gemfire"
       xmlns:tx="http://www.springframework.org/schema/tx"
       xmlns:gfe-data="http://www.springframework.org/schema/data/gemfire"
       xsi:schemaLocation="http://www.springframework.org/schema/gemfire http://www.springframework.org/schema/gemfire/spring-gemfire.xsd
        http://www.springframework.org/schema/tx http://www.springframework.org/schema/tx/spring-tx.xsd
		http://www.springframework.org/schema/beans http://www.springframework.org/schema/beans/spring-beans.xsd
		http://www.springframework.org/schema/data/gemfire http://www.springframework.org/schema/data/gemfire/spring-data-gemfire.xsd">

    <gfe-data:datasource subscription-enabled="true">
        <gfe-data:server host="localhost" port="40404" />
    </gfe-data:datasource>

    <bean id="yummyTemplate" class="org.springframework.data.gemfire.GemfireTemplate">
        <property name="region" ref="YummyNoodleOrder"/>
    </bean>

    <tx:annotation-driven/>
    <gfe:transaction-manager/>

</beans>
```

This configuration uses the Gemfire Spring configuration namespace.

Firstly, it creates a Gemfire DataSource.  This is a connection to a Gemfire data grid. In this case, connecting to the Gemfire server running on localhost:40404, which the local server configured above will run on.

The bean `yummyTemplate` is the instance of GemfireTemplate that is used in the test above. It is set up to communicate with a specific Gemfire Region, YummyNoodleOrder, which must exist in the server. Again, this is configured in the server above.

The last two configurations that set up the Gemfire transactional behaviour and integrate it with the Spring Transaction management system.


## Implement a CRUD repository

You have seen the creation of two Repository implementations against both MongoDB and JPA.  The process for creating a Spring Data Gemfire Repository is consistent with the others.

First, create a new test `OrderStatusRepositoryIntegrationTests`

```java
package com.yummynoodlebar.persistence.integration;

import com.yummynoodlebar.config.GemfireConfiguration;
import com.yummynoodlebar.persistence.domain.OrderStatus;
import com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture;
import com.yummynoodlebar.persistence.repository.OrderStatusRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GemfireConfiguration.class})
public class OrderStatusRepositoryIntegrationTests {

  @Autowired
  OrderStatusRepository ordersStatusRepository;

  @Before
  public void setup() {
    ordersStatusRepository.deleteAll();
  }

  @After
  public void teardown() {
    ordersStatusRepository.deleteAll();
  }

  @Test
  public void thatItemIsInsertedIntoRepoWorks() throws Exception {

    UUID key = UUID.randomUUID();

    OrderStatus orderStatus = PersistenceFixture.startedCooking(key);

    ordersStatusRepository.save(orderStatus);

    OrderStatus retrievedOrderStatus = ordersStatusRepository.findOne(key);

    assertNotNull(retrievedOrderStatus);
    assertEquals(key, retrievedOrderStatus.getId());
  }
}
```

This test generates a new OrderStatus with a known key and passes it to OrderStatusRepository for persisting. It then retrieves the data using the method `findOne`, which will query against the *key* that is passed into the Gemfire Region Map structure.

Note that data is being managed explicitly in the test, rather than using the declarative transaction management that was introduced in the JPA tests.  While Gemfire does integrate with the Spring provided transactions, it only support Isolation.READ_COMMITTED.  This means that once you write data, it cannot be read, by any thread or process, until the surrounding transaction is committed.  Any test that wrote data within a transaction would be unable to read it until the transaction finished.

For this reason, the test is not marked as @Transactional, so all data access will not be transactionally managed within the tests.  At the start and end of the test, the region is purged by using the repository deleteAll method generated by Spring Data.

To implement the Repository, update `OrderStatusRepository` to read

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.OrderStatus;
import org.springframework.data.gemfire.repository.GemfireRepository;

import java.util.UUID;

public interface OrderStatusRepository extends GemfireRepository<OrderStatus, UUID> {

}
```

Update `GemfireConfiguration` to enable Gemfire Repositories

```java
package com.yummynoodlebar.config;

import org.springframework.context.annotation.*;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@ImportResource({"classpath:gemfire/client.xml"})
@EnableTransactionManagement
@EnableGemfireRepositories(basePackages = "com.yummynoodlebar.persistence.repository",
    includeFilters = @ComponentScan.Filter(value = {OrderStatusRepository.class}, type = FilterType.ASSIGNABLE_TYPE))
public class GemfireConfiguration { }

```

As with the other data stores, explicitly choose the Repository interface for Spring Data Gemfire to implement.

As with the other data stores, the Entity/ persistence class, in this case OrderStatus, requires annotating to control how it is persisted into the data store.

Gemfire understand Java objects more thoroughly than either MongoDB or H2 are able to, as its underlying data model is built to do so. This means that OrderStatus requires no additions to persist naturally into Gemfire, as we saw above in `OrderStatusMappingIntegrationTests`.

Two things are necessary, however for Spring Data to be able to generate a Repository implementation, configuring the default Region, and specifying the property to use as the Region key/ ID.

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.mapping.Region;

@Region("YummyNoodleOrder")
public class OrderStatus implements Serializable {

  private UUID orderId;
  @Id
  private UUID id;
```

First, run the local gemfire server

    ./gradlew run
    
Then run the test `OrderStatusRepositoryIntegrationTests` to check that the OrderStatusRepository is being correctly generated and works as expected.

## Extend the Repository with a Custom Finder

An Order requires a history of the status updates made to it. A history is a list of OrderStatus in date order.

This requires a more complex query than simply by ID or Order ID. It will also require a sort by date. 

Create a new test `OrderStatusGetHistoryIntegrationTests`

```java
package com.yummynoodlebar.persistence.integration;

import com.yummynoodlebar.config.GemfireConfiguration;
import com.yummynoodlebar.persistence.domain.OrderStatus;
import com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture;
import com.yummynoodlebar.persistence.repository.OrderStatusRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {GemfireConfiguration.class})
public class OrderStatusGetHistoryIntegrationTests {

  @Autowired
  OrderStatusRepository ordersStatusRepository;

  @Before
  public void setup() {
    ordersStatusRepository.deleteAll();
  }

  @After
  public void teardown() {
    ordersStatusRepository.deleteAll();
  }

  @Test
  public void thatGetHistoryWorks() throws Exception {

    UUID orderId = UUID.randomUUID();

    UUID key0 = ordersStatusRepository.save(PersistenceFixture.orderReceived(orderId)).getId();
    UUID key1 = ordersStatusRepository.save(PersistenceFixture.orderReceived(orderId)).getId();
    UUID key2 = ordersStatusRepository.save(PersistenceFixture.orderReceived(orderId)).getId();

    List<OrderStatus> history = new ArrayList<OrderStatus>(ordersStatusRepository.getOrderHistory(orderId));

    assertNotNull(history);
    assertEquals(3, history.size());
    assertEquals(key0, history.get(0).getId());
    assertEquals(key1, history.get(1).getId());
    assertEquals(key2, history.get(2).getId());
  }
}
```

This test creates a sequential history of a single order id, saves that list into Gemfire, and then retrieves it using a new custom method.

Update the repository to read

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.OrderStatus;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.Query;

import java.util.Collection;
import java.util.UUID;

public interface OrderStatusRepository extends GemfireRepository<OrderStatus, UUID> {

  @Query("SELECT DISTINCT * FROM /YummyNoodleOrder WHERE orderId = $1 ORDER BY statusDate")
  public Collection<OrderStatus> getOrderHistory(UUID orderId);
}
```

This looks similar to the JPA custom method, and the concept is the same.  Create a new method and annotate it with a @Query, passing a string containing OQL to perform the query with.
This query selects the distinct elements from the YummyNoodleBar Region where the order is given and then orders by statusDate, which is a property on OrderStatus.

This will pass, with the correct ordering of the history, ordered by status date.

## Summary

Congratulations, Order Status data is safely stored in Gemfire.

Next, you will learn to take advantage of Gemfire Continuous Queries to extend the scalable, event driven architecture to include the data store itself.

[Next…  Extending the Persistence Domain to Send Events](../5/)
