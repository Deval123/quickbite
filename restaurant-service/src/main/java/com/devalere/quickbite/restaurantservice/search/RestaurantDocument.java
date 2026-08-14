package com.devalere.quickbite.restaurantservice.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.math.BigDecimal;
import java.util.List;

@Document(indexName = "quickbite-restaurants")
@Setting(settingPath = "/elasticsearch/settings.json")
public class RestaurantDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "french")
    private String name;

    @Field(type = FieldType.Text, analyzer = "french")
    private String description;

    @Field(type = FieldType.Keyword)
    private String cuisineType;

    @Field(type = FieldType.Text)
    private String address;

    @Field(type = FieldType.Text)
    private String phone;

    @GeoPointField
    private GeoPoint location;

    @Field(type = FieldType.Float)
    private BigDecimal avgRating;

    @Field(type = FieldType.Boolean)
    private boolean active;

    @Field(type = FieldType.Nested)
    private List<MenuItemDoc> menuItems;

    public RestaurantDocument() {}

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCuisineType() { return cuisineType; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public GeoPoint getLocation() { return location; }
    public BigDecimal getAvgRating() { return avgRating; }
    public boolean isActive() { return active; }
    public List<MenuItemDoc> getMenuItems() { return menuItems; }

    // --- Setters ---
    public void setId(String id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }
    public void setAddress(String address) { this.address = address; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setLocation(GeoPoint location) { this.location = location; }
    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }
    public void setActive(boolean active) { this.active = active; }
    public void setMenuItems(List<MenuItemDoc> menuItems) { this.menuItems = menuItems; }

    // --- Nested document for menu items ---
    public static class MenuItemDoc {

        @Field(type = FieldType.Text, analyzer = "french")
        private String name;

        @Field(type = FieldType.Float)
        private double price;

        @Field(type = FieldType.Keyword)
        private String category;

        @Field(type = FieldType.Boolean)
        private boolean available;

        public MenuItemDoc() {}

        public MenuItemDoc(String name, double price, String category, boolean available) {
            this.name = name;
            this.price = price;
            this.category = category;
            this.available = available;
        }

        public String getName() { return name; }
        public double getPrice() { return price; }
        public String getCategory() { return category; }
        public boolean isAvailable() { return available; }

        public void setName(String name) { this.name = name; }
        public void setPrice(double price) { this.price = price; }
        public void setCategory(String category) { this.category = category; }
        public void setAvailable(boolean available) { this.available = available; }
    }
}