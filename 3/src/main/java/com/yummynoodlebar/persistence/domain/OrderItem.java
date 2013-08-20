package com.yummynoodlebar.persistence.domain;

public class OrderItem {

  long orderItemId;

  private Order order;

  private String menuId;
  private Integer amount;


  public String getMenuId() {
    return menuId;
  }

  public void setMenuId(String menuId) {
    this.menuId = menuId;
  }

  public Integer getAmount() {
    return amount;
  }

  public void setAmount(Integer amount) {
    this.amount = amount;
  }
}
