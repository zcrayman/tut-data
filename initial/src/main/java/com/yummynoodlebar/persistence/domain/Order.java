package com.yummynoodlebar.persistence.domain;

import com.yummynoodlebar.events.orders.OrderDetails;
import org.springframework.beans.BeanUtils;

import java.util.*;

public class Order {

  private Date dateTimeOfSubmission;

  private Map<String, Integer> orderItems;

  private String id;

  public void setId(String id) {
    this.id = id;
  }

  public void setDateTimeOfSubmission(Date dateTimeOfSubmission) {
    this.dateTimeOfSubmission = dateTimeOfSubmission;
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

