package com.devalere.quickbite.restaurantservice;

import com.devalere.quickbite.shared.security.SharedSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * SharedSecurityConfig est exclu du scan : RestaurantSecurityConfig la remplace
 * entierement pour exposer /api/search/** sans JWT (cf. RestaurantSecurityConfig).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = "com.devalere.quickbite",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SharedSecurityConfig.class)
)
public class RestaurantServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(RestaurantServiceApplication.class, args);
    }

}
