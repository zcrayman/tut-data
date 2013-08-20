 
## Storing Order Data Using Java Persistence API (JPA)

Your next task for the Yummy Noodle Bar persistence project is to store Order data.  Yummy Noodle Bar has decided to use PostgreSQL to store this data, a freely available, robust, relational database.

Continuing from the last section, we will work within the Persistence domain.

In that domain we have a representation of an Order, `com.yummynoodle.persistence.domain.Order`, that we can optimise for persistence without affecting the rest of the application.

There is an event handler `OrderPersistenceEventHandler`, exchanging events with the application core,
and the repository `OrdersRepository` whose responsibility is to persist and retrieve Order data for the rest of the application.

You will implement `OrdersRepository` using Spring Data JPA and integrate this with the `OrderPersistenceEventHandler`

JPA is the Java standard for accessing relational databases, including Postgres.  It provides an API to map Java Objects to relational concepts and gives aq rich vocabulary for querying across these objects, transparently translating to SQL (most of the time).

Since JPA is a standard, there are many implementations, referred to as JPA *Providers*.

In addition to Spring Data JPA, Hibernate has been chosen as the JPA provider.

### Install PostgreSQL

Before continuing, ensure that you have MongoDB installed correctly.

Visit the [PostgreSQL](http://www.postgresql.org/) project and follow the instructions there to install PostgreSQL in your local environment.

Create a new user in the database with the username `yummy` and the password `noodle`

You should be able to run the command on your local machine

    localhost> psql -U yummy -W -h localhost
    
And see the response. Enter the password `noodle`
    
    Password for user yummy: 
    psql (9.1.9)
    SSL connection (cipher: DHE-RSA-AES256-SHA, bits: 256)
    Type "help" for help.
    
    yummy=#

Once you can do this, continue.  If this is not the case, follow the instructions on the PostgreSQL website to create a new user.
    
### Import Spring Data JPA

UPDATE WITH PROSE BELOW THIS.

Import Spring Data JPA and the Hibernate JPA Provider into your project, adding to build.gradle
Also here is the JDBC Driver for H2, the in memory relational database used for running tests.

```groovy
dependencies {
   ...
  compile 'org.springframework.data:spring-data-mongodb:1.2.3.RELEASE'
  compile 'org.springframework.data:spring-data-jpa:1.3.4.RELEASE'
  compile 'org.hibernate.javax.persistence:hibernate-jpa-2.0-api:1.0.1.Final'
  compile 'org.hibernate:hibernate-entitymanager:4.0.1.Final'
  runtime 'com.h2database:h2:1.3.173'
  ...
}
```

### Start with a (failing) test, introducing JDBC Template

Create a new, empty, class `JPAConfiguration`

test the low level mapped tables to ensure that data is being mapped as we expect it to.
Say that this is important, as we need to be able to optimise the data in the data store, using indexes.  It is also needed if we want to read the data directly (not via the mapped obejcts), which we will do in the next section.

```java
package com.yummynoodlebar.persistence.integration;

import com.yummynoodlebar.config.JPAConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

import static com.yummynoodlebar.persistence.domain.fixture.JPAAssertions.assertTableExists;
import static com.yummynoodlebar.persistence.domain.fixture.JPAAssertions.assertTableHasColumn;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPAConfiguration.class})
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class OrderMappingIntegrationTests {

  @Autowired
  EntityManager manager;

  @Test
  public void thatItemCustomMappingWorks() throws Exception {
    assertTableExists(manager, "NOODLE_ORDERS");
    assertTableExists(manager, "ORDER_ORDER_ITEMS");

    assertTableHasColumn(manager, "NOODLE_ORDERS", "ID");
    assertTableHasColumn(manager, "NOODLE_ORDERS", "SUBMISSION_DATETIME");
  }
}
```

 This test will fail as the context is not set up.


```java
package com.yummynoodlebar.config;

import com.yummynoodlebar.persistence.repository.OrdersRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.hibernate4.HibernateExceptionTranslator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@EnableTransactionManagement
public class JPAConfiguration {

  @Bean
  public DataSource dataSource() throws SQLException {

    EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
    return builder.setType(EmbeddedDatabaseType.H2).build();
  }

  @Bean
  public EntityManagerFactory entityManagerFactory() throws SQLException {

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    vendorAdapter.setGenerateDdl(true);

    LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
    factory.setJpaVendorAdapter(vendorAdapter);
    factory.setPackagesToScan("com.yummynoodlebar.persistence.domain");
    factory.setDataSource(dataSource());
    factory.afterPropertiesSet();

    return factory.getObject();
  }

  @Bean
  public EntityManager entityManager(EntityManagerFactory entityManagerFactory) {
    return entityManagerFactory.createEntityManager();
  }

  @Bean
  public PlatformTransactionManager transactionManager() throws SQLException {

    JpaTransactionManager txManager = new JpaTransactionManager();
    txManager.setEntityManagerFactory(entityManagerFactory());
    return txManager;
  }

  @Bean
  public HibernateExceptionTranslator hibernateExceptionTranslator() {
    return new HibernateExceptionTranslator();
  }
}
```

The test will now run without compilation or runtime errors, but will fail as the JPA entity is not set up.

Update `com.yummynoodle.persistence.domain.Order` to read

```java
package com.yummynoodlebar.persistence.domain;

import com.yummynoodlebar.events.orders.OrderDetails;

import javax.persistence.*;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Entity(name = "NOODLE_ORDERS")
public class Order {

  @Column(name = "SUBMISSION_DATETIME")
  private Date dateTimeOfSubmission;

  @ElementCollection(fetch = FetchType.EAGER, targetClass = java.lang.Integer.class)
  @JoinTable(name="ORDER_ORDER_ITEMS", joinColumns=@JoinColumn(name="ID"))
  @MapKeyColumn(name="MENU_ID")
  @Column(name="VALUE")
  private Map<String, Integer> orderItems;

  @Transient
  private OrderStatus orderStatus;

  @Id
  @Column(name = "ORDER_ID")
  private String id;

  public void setId(String id) {
    this.id = id;
  }

  public void setDateTimeOfSubmission(Date dateTimeOfSubmission) {
    this.dateTimeOfSubmission = dateTimeOfSubmission;
  }

  public OrderStatus getStatus() {
    return orderStatus;
  }

  public void setStatus(OrderStatus orderStatus) {
    this.orderStatus = orderStatus;
  }

  public Date getDateTimeOfSubmission() {
    return dateTimeOfSubmission;
  }

  public String getId() {
    return id;
  }

  public void setOrderItems(Map<String, Integer> orderItems) {
    if (orderItems == null) {
      this.orderItems = Collections.emptyMap();
    } else {
      this.orderItems = Collections.unmodifiableMap(orderItems);
    }
  }

  public Map<String, Integer> getOrderItems() {
    return orderItems;
  }

  public OrderDetails toOrderDetails() {
    OrderDetails details = new OrderDetails();

    details.setKey(UUID.fromString(this.id));
    details.setDateTimeOfSubmission(this.dateTimeOfSubmission);
    details.setOrderItems(this.getOrderItems());

    return details;
  }

  public static Order fromOrderDetails(OrderDetails orderDetails) {
    Order order = new Order();

    order.id = orderDetails.getKey().toString();
    order.dateTimeOfSubmission = orderDetails.getDateTimeOfSubmission();
    order.orderItems = orderDetails.getOrderItems();

    return order;
  }
}
```

TODO< describe what we've done here.

The tests will now pass, indicating that the mapping is all working as expected.


### Implement a CRUD repository

Write a test..

```java
package com.yummynoodlebar.persistence.integration;


import com.yummynoodlebar.config.JPAConfiguration;
import com.yummynoodlebar.persistence.domain.Order;
import com.yummynoodlebar.persistence.repository.OrdersRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPAConfiguration.class})
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class OrdersRepositoryIntegrationTests {

  @Autowired
  OrdersRepository ordersRepository;

  @Test
  public void thatItemIsInsertedIntoRepoWorks() throws Exception {
    String key = UUID.randomUUID().toString();

    Order order = new Order();
    order.setDateTimeOfSubmission(new Date());
    order.setId(key);

    Map<String, Integer> items = new HashMap<String, Integer>();

    items.put("yummy1", 15);
    items.put("yummy3", 12);
    items.put("yummy5", 7);

    order.setOrderItems(items);

    ordersRepository.save(order);

    Order retrievedOrder = ordersRepository.findById(key);

    assertNotNull(retrievedOrder);
    assertEquals(key, retrievedOrder.getId());
    assertEquals(items.get("yummy1"), retrievedOrder.getOrderItems().get("yummy1"));
  }

}

```

This will fail, as the repo is not set up

Update the JPAConfiguration to include JPA Repository component scanning

Again, the desired repository is selected explicitly as multiple data sources/ Spring Data configurations are being used.

```java

@Configuration
@EnableJpaRepositories(basePackages = "com.yummynoodlebar.persistence.repository",
    includeFilters = @ComponentScan.Filter(value = {OrdersRepository.class}, type = FilterType.ASSIGNABLE_TYPE))
@EnableTransactionManagement
public class JPAConfiguration {

```

And replace the contents of `OrdersRepository` to extend the Spring Data `CrudRepository`

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.Order;
import org.springframework.data.repository.CrudRepository;

public interface OrdersRepository extends CrudRepository<Order, UUID> {

}
```

Test will pass...

### Extend the Repository with a Custom Finder

Users want to be able to find orders that contain certain menu items.

a test.

```java
package com.yummynoodlebar.persistence.integration;

import com.yummynoodlebar.config.JPAConfiguration;
import com.yummynoodlebar.persistence.domain.Order;
import com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture;
import com.yummynoodlebar.persistence.repository.OrdersRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPAConfiguration.class})
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class OrdersRepositoryFindOrdersContainingTests {

  @Autowired
  OrdersRepository ordersRepository;

  @Autowired
  EntityManager entityManager;

  @Test
  public void thatSearchingForOrdesContainingWorks() throws Exception {

    ordersRepository.save(PersistenceFixture.standardOrder());
    ordersRepository.save(PersistenceFixture.standardOrder());
    ordersRepository.save(PersistenceFixture.yummy16Order());
    ordersRepository.save(PersistenceFixture.yummy16Order());

    List<Order> retrievedOrders = ordersRepository.findOrdersContaining("yummy16");

    assertNotNull(retrievedOrders);
    assertEquals(2, retrievedOrders.size());
    assertEquals(22, (int) retrievedOrders.get(0).getOrderItems().get("yummy16"));

    retrievedOrders = ordersRepository.findOrdersContaining("yummy3");

    assertNotNull(retrievedOrders);
    assertEquals(2, retrievedOrders.size());
  }

}
```

Update the repository to read

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.Order;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdersRepository extends CrudRepository<Order, String> {
  Order findById(String key);

  @Query(value = "select no.* from NOODLE_ORDERS no where no.ORDER_ID in (select ID from ORDER_ORDER_ITEMS where MENU_ID = :menuId)", nativeQuery = true)
  List<Order> findOrdersContaining(@Param("menuId") String menuId);
}
```

Here we have introduced a new query, using raw SQL.

### Next Steps

Congratulations, Order data is safely stored in XXX.

Next, you can learn how and why to create queries unrestrained by your domain model.

[Next…  Building a Query Service](../4/)
