
# Designing and Building Data Driven Services with Spring

## Persistence and Storage

The field of Data Persistence has progressed enormously over the past 20 years and in the present day covers a myriad of data models and data storage implementations.

Adopting the right data model and data store for your application can be a complex process and is often decided on at the start of a project and set in stone from thereon.

Since you're reading this tutorial, it's likely that you are considering implementing a Data Driven Service because:-

TBD


## What you'll need
To work through this tutorial you'll need a few things:

* About an hour of your time to complete the tutorial.
* An installation of the [Gradle](http://www.gradle.org) build tool, version 1.6 or above.
* A copy of the code (TODO - downloadable as Zip and/or git clone).
* An IDE of your choice; we recommend [SpringSource Tool Suite](http://www.springsource.org/sts) which is available as a [free download](http://www.springsource.org/sts).

## The Home of Data Driven Services in your Application Architecture

TBD Where Data Driven services sit in the Life Preserver diagram to be added.

Data Driven Services are an integration between the external, persistent storage engines and your core application. As such Data Driven Services can be seen as living in their own integration domain on the periphery of your applications core as shown in the above diagram [1]

As an integration between your application core and the outside world, there are a number of concerns that need to be addressed in the design and implementation of the components that make up the Data Driven Services :-

* The primary purpose of the Data Driven Service components is to integrate your application with the data stores in a natural and optimised way.
* The components that make up your persistence domain will need to evolve at a rate that is appropriate for the data they are managing.
* Your Data Driven Service components should not contain any of the core logic to your application but will collaborate with other components in the Core domains of your application in order to orchestrate the necessary functionality to provide the service interface.


That's enough on the design constraints placed on the components that implement your Data Driven Services, let's now look at how to implement those components using Spring.

## Modelling the Core Domain

TODO Zoomed in on the core components of the system in the Life Preserver.

Currently the core, application internal domain of the Yummy Noodle Bar is made up of the following components:

* **Orders**

    The collection of all orders currently in the system, regardless of status. In the terminology of [Domain Driven Design](http://en.wikipedia.org/wiki/Domain-driven_design), Orders is an Aggregate Root that ensures consistency across all of the Orders in the system.

* **Order**

    An individual order in the system that has an associated status and status history for tracking purposes.

* **OrderStatus**

    The current allocated status to an order.

* **OrderStatusHistory**

    Associated with an order, this is an ordered collection of the previous status' that the order has transitioned through.

* **PaymentDetails**
* **PaymentStatus**
* **Menu**
* **MenuItem**
* **MenuItemAvailability**

Focussing primarily on Orders, these can be acted upon by a number of events:

* **OrderCreatedEvent**

    Creates a new order for a number of menu-items.

* **OrderUpdatedEvent**

    Updates an existing Order with some additional information, possibly payment information.

* **OrderDeletedEvent**

    Deletes an existing order if it is not being cooked.

* **RequestAllCurrentOrdersEvent**

    Requests the full list of all current orders be returned.

## Modelling the Persistence Domain

For the first version of the Yummy Noodle Bar persistence services, the ability to created, update and remove Orders is the focus.

It can be tempting to simply map the core Order domain to data stores and work from there, but that would ignore the boundary between the Core and the Persistence service domain (TODO highlight this boundary on a focus on the Life Preserver).

The data model of your persisted data will need to change at a rate that is manageable and technically feasible given the data store implementation, and the core will need to evolve at whatever rate the Yummy Noodle bar system need to internally evolve at. So there is potentially friction between the two domains as they may need to evolve at different rates.

To manage this friction you need to create concepts and components in the Persistence Service domain that are unique to, and can evolve at the rate needed by, the Persistence domain itself. This may result in similar types of components but since their purpose will be very different the similarities are superficial.

## Understanding differing data models and their implementations

TODO, describe document, relational and key store and some of their properties.

## Step 1: Building a Relational Model based Data Driven Service using JPA

TBD A CRUD service for orders

## Step 2: Building a Document Model based Data Driven Service using Mongo

TBD A CRUD service for menu data

## Step 3: Building a Query Service

TBD A Query service for orders against the relational model.

## Step 4: Building an Object Store Model based Data Driven Service using Gemfire

TBD A CRUD service for orders, optionally replacing the JPA version, and providing the same interface.

## Step 5: Building a Continuous Query Service

TBD A Service that manages continuous queries and issues events to the Core based on their results. 

## Recap and Where to go Next?