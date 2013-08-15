package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.MenuItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MenuItemMemoryRepository implements MenuItemRepository {

  private Map<String, MenuItem> orders = new HashMap<String, MenuItem>();

  @Override
  public MenuItem save(MenuItem order) {
    return orders.put(order.getId(), order);
  }

  @Override
  public void delete(String key) {
    orders.remove(key);
  }

  @Override
  public MenuItem findById(String key) {
    for(MenuItem item: orders.values()) {
      if (item.getId().equals(key)) {
        return item;
      }
    }
    return null;
  }

  @Override
  public List<MenuItem> findAll() {
    return new ArrayList<MenuItem>(orders.values());
  }
}
