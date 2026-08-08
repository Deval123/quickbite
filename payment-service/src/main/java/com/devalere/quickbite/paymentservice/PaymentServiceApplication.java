package com.devalere.quickbite.paymentservice;

import com.devalere.quickbite.shared.security.SharedSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication(scanBasePackages = "com.devalere.quickbite")
@ComponentScan(
        basePackages = "com.devalere.quickbite",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SharedSecurityConfig.class
        )
)
public class PaymentServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
