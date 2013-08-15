package com.yummynoodlebar.persistence.repository;

import com.yummynoodlebar.persistence.domain.MenuItem;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface MenuItemRepository extends CrudRepository<MenuItem, String> {

  public List<MenuItem> findByIngredientsNameIn(String... name);

}
