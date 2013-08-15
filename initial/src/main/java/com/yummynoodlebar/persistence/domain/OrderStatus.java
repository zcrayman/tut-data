package com.yummynoodlebar.persistence.domain;

import com.yummynoodlebar.events.orders.OrderStatusDetails;

import java.util.Date;

public class OrderStatus {

  private Date statusDate;
  private String status;

  public OrderStatus(final Date date, final String status) {
    this.status = status;
    this.statusDate = date;
  }

  public Date getStatusDate() {
    return statusDate;
  }

  public String getStatus() {
    return status;
  }

  public OrderStatusDetails toStatusDetails() {
    return new OrderStatusDetails(statusDate, status);
  }

  public static OrderStatus fromStatusDetails(OrderStatusDetails orderStatusDetails) {
    return new OrderStatus(orderStatusDetails.getStatusDate(), orderStatusDetails.getStatus());
  }
}
