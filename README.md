
# Designing and Building Persistence Repositories with Spring

## Persistence and Storage

The field of Data Persistence has progressed enormously over the past 20 years and in the present day covers a myriad of data models and data storage implementations.

Adopting the right data model and data store for your application can be a complex process and is often decided on at the start of a project and set in stone from thereon.

Since you're reading this tutorial, it's likely that you are considering implementing a Repository because:-

TBD


## What you'll need
To work through this tutorial you'll need a few things:

* About an hour of your time to complete the tutorial.
* An installation of the [Gradle](http://www.gradle.org) build tool, version 1.6 or above.
* A copy of the code (TODO - downloadable as Zip and/or git clone).
* An IDE of your choice; we recommend [SpringSource Tool Suite](http://www.springsource.org/sts) which is available as a [free download](http://www.springsource.org/sts).

## The Home of Repositories in your Application Architecture

TBD Where Repositories sit in the Life Preserver diagram to be added.

Repositories are an integration between the external, persistent storage engines and your core application. As such Repositories can be seen as living in their own integration domain on the periphery of your applications core as shown in the above diagram [1]

As an integration between your application core and the outside world, there are a number of concerns that need to be addressed in the design and implementation of the components that make up the Repositories :-

* The primary purpose of the Repository components is to integrate your application with the data stores in a natural and optimised way.
* The components that make up your persistence domain will need to evolve at a rate that is appropriate for the data they are managing.
* Your Repository components should not contain any of the core logic to your application but will collaborate with other components in the Core domains of your application in order to orchestrate the necessary functionality to provide the service interface.


That's enough on the design constraints placed on the components that implement your Repositories, let's now look at how to implement those components using Spring.


The rest of this tutorial is spread out over the following pages:

* [Step 1: Modelling RESTful Service Domain](1/)
* [Step 2: Building Your First RESTful Service](2/)
* [Step 3: Wiring Up and Deploying your Service](3/)
* [Step 4: Testing your Service using RESTTemplate](4/)
* [Step 5: Securing your Service with Spring Security](5/)
* [Step 6: Make your Service Discoverable using Spring HATEOAS](6/)
* [Recap and Where to go Next?](7/)

