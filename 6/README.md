## Extending the Persistence Domain to Send Events

The event handler and the repositories you have made that now make up the persistence domain will react to events and persist or retrieve data on demand.

Another team in the The Yummy Noodle Bar project is building a system to provide real time notifications to clients (for example, to a user on the website) while an order is being processed and cooked.

Complicating matters, the Yummy Noodle Bar application is to be deployed as a cluster for high availability and scalability.  This means that a status update could be delivered on one application instance, while a client that should be notified is being managed by another instance.

To support this, you need to extend the persistence domain to provide notifications across all application instances every time the status of an order is updated.

To achieve this, you will use the Continuous Query feature of Gemfire to receive updates on every application instance when a modification is made.

### Data Grids and Continuous Queries

In a traditional data store, an application would have to regularly poll to receive updated data.  This is inefficient, as many queries will be executed unecessarily, and also introduces latency in between polls.  More realistically, applications will likely introduce some seperate messaging infrastructure, such as RabbitMQ to distribute notifications.

Gemfire is a distributed data grid, it is naturally clustered and provides a feature called Continuous Querying; this allows you to register a Gemfire Query with the cluster and for a simple POJO to receive events whenever a new piece of data is added that matches this query.

### Sending Events to the Core

 write a simple interaction test between a new POJO and the core OrderStatusService (which will already exist)
 
 Implement the POJO.
 
### Writing a continuous query

(need to check the exact flow for this post implementation)

 Write an integration test hooking up the POJO with a configured gemfire CQ.
 
 Write the gemfire/ spring config.
 
 profit...


 
### Next Steps

Congratulations, notifications about changing statuses are now being propogated across the application cluster, using Gemfire.

[Next…  Recap and Where to go Next](../7/)


