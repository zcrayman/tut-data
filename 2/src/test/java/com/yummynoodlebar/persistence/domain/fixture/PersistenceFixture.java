package com.yummynoodlebar.persistence.domain.fixture;

import com.yummynoodlebar.persistence.domain.Ingredient;
import com.yummynoodlebar.persistence.domain.MenuItem;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;

public class PersistenceFixture {

  public static MenuItem standardItem() {
    MenuItem item = new MenuItem();
    item.setDescription("Peanutty Noodles, perfect for every occasion.");
    item.setName("Yummy Noodles");
    item.setCost(new BigDecimal("12.99"));
    item.setIngredients(new HashSet<Ingredient>(
        Arrays.asList(
            new Ingredient("Noodles", "Crisp, lovely noodles"),
            new Ingredient("Egg", "Used in the noodles"),
            new Ingredient("Peanuts", "A Nut")
        )));

    return item;
  }

  public static MenuItem eggFriedRice() {
    MenuItem item = new MenuItem();
    item.setDescription("Rice, Egg Fried");
    item.setName("Yummy Rice");
    item.setCost(new BigDecimal("12.99"));
    item.setIngredients(new HashSet<Ingredient>(
        Arrays.asList(
            new Ingredient("Rice", "Straight White Rice"),
            new Ingredient("Egg", "Chicken Eggs")
        )));

    return item;
  }


}
