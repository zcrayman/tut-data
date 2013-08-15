
## Step 2: Storing Menu Data Using MongoDB

It's time to write your first Repository for the Yummy Noodle Bar, managing Menu data.

### Import Spring Data MongoDB

Import spring-data-mongodb into your project, add to build.gradle

```groovy
  compile 'org.springframework.data:spring-data-mongodb:1.2.3.RELEASE'
  compile 'cglib:cglib:2.2.2'
```

### Set the scene (need better section name)

The Yummy Noodle Bar application core has been implemented, and you are tasked with extending it to
be able to store data.

We create a new persistence domain (life presever pic).

In that domain we have a representation of MenuItem optimised for persistence.

There is an event handler `MenuPersistenceEventHandler`, exchanging events with the application core,
and the repository `MenuItemRepository` whose responsibility is to persist and retrieve MenuItem data for the rest of the application.

You will implement `MenuItemRepository` using Spring Data MongoDB and integrate this with the `MenuPersistenceEventHandler`


### Start with a (failing) test, introducing MongoTemplate

Before we can implement the Repository, we have to have something to use with it, the Domain, in this case meaning persistence, class.

Create a new test class `com.yummynoodlebar.persistence.integration.MenuItemMappingIntegrationTests`

This class will test that `com.yummynoodlebar.persistence.domain.MenuItem` works as expected against a real, running MongoDB instance.

Before we can pass that test, however, we need some tools to test with.

`MongoTemplate` is on of the core classes provided by Spring Data MongoDB.  It follows the familiar template pattern that is used extensively in other parts of Spring,
such as `JMSTemplate`, `JDBCTemplate` and `RESTTemplate`.

It allows quick and easy connection to a MongoDB server and exposes a large amount of functionality in a single place.

Test Driven Development guides us to test the smallest piece of the system we can, and build our tests outwards from that; building confidence in the system as a whole.

The smallest piece in this case is the domain class, MenuItem.  It will contain mapping and configuration information describing how it should be persisted into the database.

There are two existing helper classes, `com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture` and `com.yummynoodlebar.persistence.domain.fixture.MongoAssertions`
They provide some methods we can use to make our tests a bit more readable.

Update `MenuItemMappingIntegrationTests` to read

```java
public class MenuItemMappingIntegrationTests {

  MongoOperations mongo;

  @Before
  public void setup() throws Exception {
    mongo = new MongoTemplate(new SimpleMongoDbFactory(new Mongo(), "yummynoodle"));

    mongo.dropCollection("menu");
  }

  @After
  public void teardown() {
    mongo.dropCollection("menu");
  }

  @Test
  public void thatItemIsInsertedIntoCollectionHasCorrectIndexes() throws Exception {

    mongo.insert(standardItem());

    assertEquals(1, mongo.getCollection("menu").count());

    assertTrue(usingMongo(mongo).collection("menu").hasIndexOn("_id"));
    assertTrue(usingMongo(mongo).collection("menu").hasIndexOn("name"));
  }

  @Test
  public void thatItemCustomMappingWorks() throws Exception {
    mongo.insert(standardItem());

    assertTrue(usingMongo(mongo).collection("menu").first().hasField("itemName"));
  }
}
```

This is a simple usage of MongoTemplate, using the persistence.domain.MenuItem class to push data into and out of a Mongo Collection.

TODO, describe the connection process - localhost, no security, default port 27017

Here, the test ensure that the mapping works as expected, and the document appears in the expected shape in the collection.  It also tests that the indexes that we expect
have also been initialised.

Run this test, it will fail, as the mapping is not as expected, the collection being used is wrong, and the indexes are not being fully created
We can now move onto altering our domain class to ensure it persists correctly.

Open `com.yummynoodlebar.persistence.domain.MenuItem`.

Add the annotations @Document, @Id and @Indexed to bring the domain into line with the test expectations.

```java

@Document(collection = "menu")
public class MenuItem {

  @Id
  private String id;

  @Field("itemName")
  @Indexed
  private String name;

  ....
```

This alters the collection used to be *menu*, ensures that the field *id* is used as the Mongo document *_id* and that the field *name* is stored as *itemName* and is indexed.

None of these are annotations are necessary, a bare POJO (Plain old Java Object) can be passed to MongoTemplate and it will apply its default behaviour.  We have chosen to alter that behaviour using these mapping annotations on the persistence entity.


### Implement a CRUD repository

MenuItem is now ready to persist.  We could now write an implementation of `MenuItemRepository` using MongoTemplate,
many applications do this successfully.

Spring Data gives us another option, however.  It can create an implementation of the Repository interface for us, at runtime.

To take advantage of this, we first need to update `MenuItemRepository` into something that Spring Data can handle.

Before we do that though, a test!

Create a new class at `com.yummynoodlebar.config.MongoConfiguration`, leaving it empty for now.

Then create a test like so

```java
package com.yummynoodlebar.persistence.integration;


import com.yummynoodlebar.config.MongoConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture.standardItem;
import static junit.framework.TestCase.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoConfiguration.class})
public class MenuItemRepositoryIntegrationTests {

  @Autowired
  MenuItemRepository menuItemRepository;

  @Autowired
  MongoOperations mongo;

  @Before
  public void setup() throws Exception {
    mongo.dropCollection("menu");
  }

  @After
  public void teardown() {
    mongo.dropCollection("menu");
  }

  @Test
  public void thatItemIsInsertedIntoRepoWorks() throws Exception {

    assertEquals(0, mongo.getCollection("menu").count());

    menuItemRepository.save(standardItem());

    assertEquals(1, mongo.getCollection("menu").count());
  }

}

```

This will fall in a heap, with the correct error that the Application Context cannot be initialised.


Open `com.yummynoodlebar.persistence.repository.MenuItemRepository`

It looks like this

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.MenuItem;

public interface MenuItemRepository {

  MenuItem save(MenuItem order);

  void delete(String key);

  MenuItem findById(String key);

  Iterable<MenuItem> findAll();
}
```

Alter it to read like this instead

```java
package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.MenuItem;
import org.springframework.data.repository.CrudRepository;

public interface MenuItemRepository extends CrudRepository<MenuItem, String> {

  MenuItem findById(String key);

}
```

CrudRepository is part of the Spring Data repository hierarchy that act as a marker for Spring Data to expand the methods at runtime into real, living breathing implementations of a repository.

Everything will still compile in the project, however the test will not pass.

Open `MongoConfiguration` again, and alter it to read like so

```java
import com.mongodb.Mongo;
import com.yummynoodlebar.persistence.repository.MenuItemRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.net.UnknownHostException;

@Configuration
@EnableMongoRepositories(basePackages = "com.yummynoodlebar.persistence.repository",
      includeFilters = @ComponentScan.Filter(value = MenuItemRepository.class, type = FilterType.ASSIGNABLE_TYPE))

public class MongoConfiguration {

  public @Bean MongoTemplate mongoTemplate(Mongo mongo) throws UnknownHostException {
    return new MongoTemplate(mongo, "yummynoodle");
  }

  public @Bean Mongo mongo() throws UnknownHostException {
    return new Mongo("localhost");
  }

}
```

`@Configuration` marks the class as a Spring Configuration/ Java Config class, able to generate part of a Spring ApplicationContext.
`@EnableMongoRepositories` is part of Spring Data, and works to construct the repository implementation discussed earlier. The values passed into the annotation
select the class(es) that we want to be considered.  In this case, only `MenuItemRepository` is to be considered, and so it is referred to explicitly.

The guts of the configuration deal with connecting to MongoDB.  Creating a Mongo driver connection and a MongoTemplate to wrap it.

The auto generated Repositories use the MongoTemplate created here to connect to MongoDB.

Run the test again, it should now pass cleanly.

Congratulations!  You have a working repository, without having to actually implement it yourself.

It only does CRUD, is that enough?

### Extend the Respository with a Custom Finder

A late breaking requirement has been uncovered!

Users are going to be given the opportunity to select dishes by the names of the ingredients that they contain.

Looking at MenuItem, the document that is stored in MongoDB will look similar to

```json
{
        "_id" : ObjectId("520d388bea7e3adc2a054886"),
        "_class" : "com.yummynoodlebar.persistence.domain.MenuItem",
        "ingredients" : [
                {
                        "name" : "Noodles",
                        "description" : "Crisp, lovely noodles"
                },
                {
                        "name" : "Peanuts",
                        "description" : "A Nut"
                }
        ],
        "cost" : "12.99",
        "minutesToPrepare" : 0
}
```

This will require querying deep inside the document structure to correctly identify the matching documents.

A test.

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoConfiguration.class})
public class MenuItemRepositoryFindByIngredientsIntegrationTests {

  @Autowired
  MenuItemRepository menuItemRepository;

  @Autowired
  MongoOperations mongo;

  @Before
  public void setup() throws Exception {
    mongo.dropCollection("menu");
  }

  @After
  public void teardown() {
    mongo.dropCollection("menu");
  }

  @Test
  public void thatItemIsInsertedIntoRepoWorks() throws Exception {

    menuItemRepository.save(standardItem());
    menuItemRepository.save(standardItem());
    menuItemRepository.save(eggFriedRice());

    List<MenuItem> peanutItems = menuItemRepository.findByIngredientsNameIn("Peanuts");

    assertEquals(2, peanutItems.size());
  }
}
```

This assumes a method named `findByIngredientsNameIn` on the repository, which is a descriptive enough, let's see how to implement that.
`standardItem()` has peanuts, `eggFriedRice()` doesn't.  So we should see just two documents come back from the query.

Add the method to `MenuItemRepository`

```java
public List<MenuItem> findByIngredientsNameIn(String... name);
```

Try running the test....  it passes, right?

Spring Data has generated an implementation of this method, doing what we wanted. There is a rich vocabulary that you can express using the method names, see more in the [Reference Documentation](http://static.springsource.org/spring-data/data-mongodb/docs/current/reference/html/mongo.repositories.html#mongodb.repositories.queries)

### Extend the Repository with Map/ Reduce

A more esoteric requriement.  Users want to know the ingredients used in the most dishes.

MongoDB provides a system to perform this kind of analysis, Map/ Reduce (/ Filter).

To use Map/Reduce, we need to gain access to the MongoTemplate directly.

Create a new interface `

TODO

```java

```



Now that the Menu data is safely stored in Mongo, its time to turn your attention to the core of the system, Orders

[Next… Storing Order Data using JPA](../3/)