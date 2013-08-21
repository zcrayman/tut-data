 
## Storing the Order Status in Gemfire using Spring Data Gemfire

Gemfire is a high performance distributed data grid.  It scales from a small embedded cache implementation to large scale wide area network implementations with data residency and access control.

Spring Data allows the creation of both server and client connections, data access, caching and deep integration with the Spring Application Context.

You will see here the creation of a Spring Data interface to a Gemfire server. The next section of the tutorial shows the extension of this functionality to cover the use of Continuous Queries and how to integrate those into the Yummy Noodle application.

### Import Spring Data Gemfire

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

The springsource repo is required to access the gemfire libraries, which are not available from maven central.

### Run a Gemfire Cache Server

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

This server will have access to all of the classpath of the project, most notably the OrderStatus class.  It is necessary for the Gemfire server to have access to this class if we want to persist it within the grid.  When you create a standalone Gemfire grid, you will need to provide any classes you wish to persist within a jar file on the classpath of every gemfire server.

### Start with a (failing) test, introducing Gemfire Template



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

We will be accessing gemfire in client/ server mode.  (make sure that we describe whats going on)

Start up the cache server and leave it running (TODO, make an embedded version for this)

mention @Transactional, rollback, how it relates to <gfe:transaction-manager id="transactionManager"/>,
and the integration with the spring transaction stuff


### Implement a CRUD repository

Create a new test `OrderStatusRepositoryIntegrationTests`


Update `OrderStatusRepository` to read

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.OrderStatus;
import org.springframework.data.gemfire.repository.GemfireRepository;

import java.util.UUID;

public interface OrderStatusRepository extends GemfireRepository<OrderStatus, UUID> {

}
```

Update `OrderStatus` to add @Region and @Id annotations [EXPLAIN]
ID for the findOne/ generated finders/ save key that we did explicitly for GemfireTemplate

@Region configures the default region that the repository will access.
This can be overridden in the @Query entries.

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.mapping.Region;

@Region("YummyNoodleOrder")
public class OrderStatus implements Serializable {

  private UUID orderId;
  @Id
  private UUID id;
```

### Extend the Repository with a Custom Finder

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

This will pass, with the correct ordering of the history.


### Next Steps

Congratulations, Order Status data is safely stored in Gemfire.

Next, you can learn to take advantage of Gemfire Continuous Queries to create a scalable, event driven architecture using your data.

[Next…  Extending the Persistence Domain to Send Events](../5/)
