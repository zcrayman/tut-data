
# Step 2: Storing Menu Data Using MongoDB

The Yummy Noodle Bar application core has been implemented, and you are tasked with extending it to
be able to store data.

We work within the Persistence domain to add this functionality. (life presever pic).

In that domain we have a representation of MenuItem optimised for persistence.

There is an event handler `MenuPersistenceEventHandler`, exchanging events with the application core,
and the repository `MenuItemRepository` whose responsibility is to persist and retrieve MenuItem data for the rest of the application.

You will implement `MenuItemRepository` using Spring Data MongoDB and integrate this with the `MenuPersistenceEventHandler`

## Aboout MongoDB

MongoDB is a document oriented database that stores data natively in a document format called BSON (Binary JSON). This is similar in structure to JSON, and is ultimately derived from it.

It does not enforce a schema or document structure beyond the concept of the *Collection*, whcih contains a set of documents.

Querying can be performed over the whole document structure, although Joins between Collections is not part of the document model.

Large scale data transformation and analysis is a native part of MongoDB, and can be performed either declaratively, or via JavaScript functions as part of a Map Reduce implementation.

## Install MongoDB

Before continuing, ensure that you have MongoDB installed correctly.

If you don't have it installed already, visit the [Mongo DB Project](http://www.mongodb.org] and follow the intructions to install MongoDB.  

Do not set up any authentication.

You should be able to run the command on your local machine

    mongo
 
And see the response 

    MongoDB shell version: 2.0.8
    connecting to: test
    >

## Import Spring Data MongoDB

Import spring-data-mongodb into your project, adding to build.gradle

```groovy
dependencies {
   ...
  compile 'org.springframework.data:spring-data-mongodb:1.2.3.RELEASE'
  compile 'cglib:cglib:2.2.2'
  ...
}
```

## Start with a (failing) test, introducing MongoTemplate

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


## Implement a CRUD repository

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

This will fail.


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

## Extend the Repository with a Custom Finder

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

## Extend the Repository with Map/ Reduce

A more esoteric requirement.  Users want to know the ingredients used in the most dishes.

MongoDB provides a system to perform this kind of analysis, Map/ Reduce (/ Filter).

To use Map/Reduce, we need to gain access to the MongoTemplate directly and add this into the Repository that
Spring Data is currently managing.

Create a new interface `com.yummynoodlebar.persistence.repository.AnalyseIngredients`

```java
package com.yummynoodlebar.persistence.repository;

import java.util.Map;

public interface AnalyseIngredients {

  public Map<String, Integer> analyseIngredientsByPopularity();

}
```

Next, update `MenuItemRepository` to include the `AnalyseIngredients` interface. This indicates to Spring Data that
it should look for an implementation of that interface for extension.

```java

public interface MenuItemRepository extends CrudRepository<MenuItem, String>, AnalyseIngredients {

  public List<MenuItem> findByIngredientsNameIn(String... name);

}
```

We can now write an implementation of this new interface. Before doing that though, you must write a test for the new behaviour.
Create a test class `com.yummynoodlebar.persistence.integration.MenuItemRepositoryAnalyseIngredientsIntegrationTests`

```java
package com.yummynoodlebar.persistence.integration;


import com.yummynoodlebar.config.MongoConfiguration;
import com.yummynoodlebar.persistence.repository.MenuItemRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;

import static com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture.eggFriedRice;
import static com.yummynoodlebar.persistence.domain.fixture.PersistenceFixture.standardItem;
import static junit.framework.TestCase.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {MongoConfiguration.class})
public class MenuItemRepositoryAnalyseIngredientsIntegrationTests {

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
  public void thatIngredientsAnalysisWorks() throws Exception {

    menuItemRepository.save(standardItem());
    menuItemRepository.save(standardItem());
    menuItemRepository.save(standardItem());
    menuItemRepository.save(eggFriedRice());
    menuItemRepository.save(eggFriedRice());

    Map<String, Integer> ingredients = menuItemRepository.analyseIngredientsByPopularity();

    assertEquals(4, ingredients.size());
    assertEquals(5, (int)ingredients.get("Egg"));
    assertEquals(3, (int)ingredients.get("Noodles"));
    assertEquals(3, (int)ingredients.get("Peanuts"));
    assertEquals(2, (int)ingredients.get("Rice"));
  }
}

```

This sets up some known test data and calls the analysis method, expecting certain ingredients in known relative quantities.

Lastly, create an implementation of this interface `com.yummynoodlebar.persistence.repository.MenuItemRepositoryImpl`.
The name of this class `MenuItemRepositoryImpl` is very important! This marks it out as an *extension* of the repository
named `MenuItemRepository`, and is automatically component scanned, instantiated and used as a delegate by Spring Data.

```java
package com.yummynoodlebar.persistence.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapreduce.MapReduceResults;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class MenuItemRepositoryImpl implements AnalyseIngredients {

  @Autowired
  private MongoTemplate mongoTemplate;

  @Override
  public Map<String, Integer> analyseIngredientsByPopularity() {
    MapReduceResults<IngredientAnalysis> results = mongoTemplate.mapReduce("menu",
        "classpath:ingredientsmap.js",
        "classpath:ingredientsreduce.js",
        IngredientAnalysis.class);

    Map<String, Integer> analysis = new HashMap<String, Integer>();

    for(IngredientAnalysis ingredientAnalysis: results) {
      analysis.put(ingredientAnalysis.getId(), ingredientAnalysis.getValue());
    }

    return analysis;
  }

  private static class IngredientAnalysis {
    private String id;
    private int value;

    private void setId(String name) {
      this.id = name;
    }

    private void setValue(int count) {
      this.value = count;
    }

    public String getId() {
      return id;
    }

    public int getValue() {
      return value;
    }
  }
}
```

This class references two javascript functions, the mapper and the reducer, respectively.

Create 2 new javascript files, in the src/main/resources directory

**ingredientsmap.js**

```javascript
function() {
    for (var idx=0; idx < this.ingredients.length; idx++) {
        emit(this.ingredients[idx].name, 1);
    }
}
```

**ingredientsreduce.js**
```javascript
function(name, current) {
    var red = 0;

    for (var idx = 0; idx < current.length; idx++) {
        red++
    }
    return red;
}

```

The test should now pass successfully.

Congratulations! You have created a custom data analysis task against MongoDB.

## Summary

Now that the Menu data is safely stored in Mongo, its time to turn your attention to the core of the system, Orders

[Next… Storing Order Data using JPA](../3/)