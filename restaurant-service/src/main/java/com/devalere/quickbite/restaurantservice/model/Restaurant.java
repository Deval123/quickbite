package com.devalere.quickbite.restaurantservice.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "cuisine_type")
    private String cuisineType;

    @Column(nullable = false)
    private String address;

    @Column
    private String phone;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "avg_rating", precision = 2, scale = 1)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Restaurant() {}

    public void setId(UUID id) { this.id = id; }

    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public void setName(String name) { this.name = name; }

    public void setDescription(String description) { this.description = description; }

    public void setCuisineType(String cuisineType) { this.cuisineType = cuisineType; }

    public void setAddress(String address) { this.address = address; }

    public void setPhone(String phone) { this.phone = phone; }

    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }

    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }

    public void setActive(boolean active) { this.active = active; }

    public void setAvgRating(BigDecimal avgRating) { this.avgRating = avgRating; }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
