 

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

It can be tempting to simply map the core Order domain to the data stores and work from there, but that would ignore the boundary between the Core and the Persistence domain (TODO highlight this boundary on a focus on the Life Preserver).

The data model of your persisted data will need to change at a rate that is manageable and technically feasible given the data store implementation, and the core will need to evolve at whatever rate the Yummy Noodle bar system need to internally evolve at. So there is potentially friction between the two domains as they may need to evolve at different rates.

To manage this friction you need to create concepts and components in the Persistence Service domain that are unique to, and can evolve at the rate needed by, the Persistence domain itself. This may result in similar types of components but since their purpose will be very different the similarities are superficial.

## Understanding differing data models and their implementations

TODO, describe document, relational and key store and some of their properties.