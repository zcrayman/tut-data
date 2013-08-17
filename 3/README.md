 
## Storing Order Data Using JPA

Your next task for the Yummy Noodle Bar persistence project is to store Order data.  Yummy Noodle Bar has decided to use PostgreSQL to store this data, a freely available, robust, relational database.

Continuing from the last section, we will work within the Persistence domain.

In that domain we have a representation of an Order, `com.yummynoodle.persistence.domain.Order`, that we can optimise for persistence without affecting the rest of the application.

There is an event handler `OrderPersistenceEventHandler`, exchanging events with the application core,
and the repository `OrdersRepository` whose responsibility is to persist and retrieve Order data for the rest of the application.

You will implement `OrdersRepository` using Spring Data JPA and integrate this with the `OrderPersistenceEventHandler`

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

Import spring-data-jpa into your project, adding to build.gradle

```groovy
dependencies {
   ...
  compile 'org.springframework.data:spring-data-jpa:1.3.4.RELEASE'
  ...
}
```

### Start with a (failing) test, introducing JDBC Template (/or JPA for testing?)

test the low level mapped tables to ensure that data is being persisted as we expect it to.
Say that this is important, as we need to be able to optimise the data in the data store, using indexes.  It is also needed if we want to read the data directly (not via the mapped obejcts), which we will do in the next section.


### Implement a CRUD repository




### Extend the Respository with a Custom Finder




### Next Steps

Congratulations, Order data is safely stored in PostgreSQL.

Next, you can learn how and why to create queries unrestrained by your domain model.

[Next…  Building a Query Service](../4/)
