package com.devalere.quickbite.gatewayservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.cloud.gateway.routes[0].id=test-route",
        "spring.cloud.gateway.routes[0].uri=http://localhost:8081",
        "spring.cloud.gateway.routes[0].predicates[0]=Path=/test/**",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379"
})
class GatewayServiceApplicationTests
{

    @Test
    void contextLoads()
    {
    }

}
