 
## Building a Query Service

TODO Discuss CQRS and the need for seperate queries that are disconnected from the entities (Order, MenuItem) in order to create optimised queries.

....

### Understanding the Q in CQRS

TODO Through our tests, we have proven the structure of the data in the data stores, and so we can safely create code that reads and processes these formats, and converts it into different representations.

TODO You have already created a Query such as this! In section 2, when we used Map/ Reduce.

### Build a JDBC based query over the Orders data.

TODO Think up some custom query that we need a custom representation for .... ? Might need another table.

TODO Write a test, implement the query as an extension on the Orders repo, generating a new order related query only entity.


### Next Steps

Congratulations! You now have a rich domain model stored in two data stores, and you have created custom queries that operate on the data stores.

There is one part of the domain model that is still to be stored, Order Status.  In the next section, you will store that in a distributed data grid, Gemfire.

[Next…  Storing the Order Status in Gemfire](../5/)
