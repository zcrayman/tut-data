package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface OrdersRepository extends CrudRepository<Order, UUID> {
  Order findById(UUID key);
}
