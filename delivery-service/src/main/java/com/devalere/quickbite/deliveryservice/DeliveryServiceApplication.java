package com.devalere.quickbite.deliveryservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.devalere.quickbite")
public class DeliveryServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(DeliveryServiceApplication.class, args);
    }

}
