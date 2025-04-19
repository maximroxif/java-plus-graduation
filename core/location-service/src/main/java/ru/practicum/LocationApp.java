package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.practicum.client.LikeServiceClient;
import ru.practicum.client.UserServiceClient;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(clients = {UserServiceClient.class, LikeServiceClient.class})
public class LocationApp {
    public static void main(String[] args) {
        SpringApplication.run(LocationApp.class, args);
    }
}
