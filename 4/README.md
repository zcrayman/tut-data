 
## Storing the Order Status in Gemfire using Spring Data Gemfire

brief description of the gemfire architecture, direct to main docs for more information.

### Run a Gemfire Cache Server

say that a cache server can be started using the gemfire-server project in this repo on the default port 40404

describe that project? link to the spring xml setup?

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

### Start with a (failing) test, introducing Gemfire Template

create GemfireConfiguration, empty

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
