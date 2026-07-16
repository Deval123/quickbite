package com.devalere.quickbite.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.devalere.quickbite")
public class OrderServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
