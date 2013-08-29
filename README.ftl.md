
In this tutorial you'll use Spring to store data in multiple data stores.

## Persistence and Storage

The field of Data Persistence has progressed enormously over the past 20 years and in the present day covers a myriad of data models and data storage implementations.

Adopting the right data model and data store for your application can be a complex process and is often decided on at the start of a project and set in stone from thereon.

Since you're reading this tutorial, it's likely that you are considering implementing a Repository because:-

* You have a data store that you need to access
* You have an existing Spring Data application that you need to maintain
* You want to investigate different data stores in the context of Spring Data

This tutorial covers three data stores, MongoDB, a relational database using JPA and Pivotal Gemfire. 

## What you'll build

Yummy Noodle Bar is going global.  As part of its planned expansion, it needs to be able to store and update its Menu, and store Orders in the appropriate database(s).

You will extend Yummmy Noodle Bar's internal application to store Menu data in MongoDB, Order data in a Relational Database and track the Status of an Order using Gemfire. Spring Data will provide the bedrock of your persistence project, and you will discover how it makes your data access simpler, more consistent and more robust.

![Yummy Noodle Bar](images/yummynoodle.jpg)


## What you'll need
To work through this tutorial you'll need a few things:

* About an hour of your time to complete the tutorial.
* A copy of the code (TODO - downloadable as Zip and/or git clone).
* An IDE of your choice; we recommend [SpringSource Tool Suite](http://www.springsource.org/sts) which is available as a [free download](http://www.springsource.org/sts).
* An installation of [MongoDB](http://www.mongodb.org/)

## The Home of Repositories in your Application Architecture

Repositories are an integration between the external, persistent storage engines and your core application. Repositories can be seen as living in their own integration domain on the periphery of your applications core as shown in the following diagram:

TBD Where Repositories sit in the Life Preserver diagram to be added.

As an integration between your application core and the outside world there are a number of concerns that need to be addressed in the design and implementation of the components that make up the Repositories:

* The primary purpose of the Repository components is to integrate your application with the data stores in a natural and optimised way.
* The components that make up your persistence domain will need to evolve at a rate that is appropriate for the data they are managing.
* Your Repository components should not contain any of the core logic to your application but will collaborate with other components in the Core domains of your application in order to orchestrate the necessary functionality to provide the service interface.


That's enough on the design constraints placed on the components that implement your Repositories, the rest of this tutorial looks at how to implement those components using Spring:

* [Step 1: Understanding your Data Model and Modelling the Persistence Domain](1/)
* [Step 2: Storing Menu Data Using MongoDB](2/)
* [Step 3: Storing Order Data Using JPA](3/)
* [Step 4: Storing the Order Status in Gemfire using Spring Data Gemfire](4/)
* [Step 5: Extending the Persistence Domain to Send Events](5/)
* [Recap and Where to go Next?](6/)

