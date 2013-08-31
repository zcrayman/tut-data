package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.Order;

public interface OrdersRepository {

  Order save(Order order);

  void delete(String key);

  Order findOne(String key);

  Iterable<Order> findAll();
}
