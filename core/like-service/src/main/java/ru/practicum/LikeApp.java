package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.practicum.client.EventServiceClient;
import ru.practicum.client.LocationServiceClient;


@SpringBootApplication
@EnableFeignClients(clients = {EventServiceClient.class, LocationServiceClient.class})
public class LikeApp {
    public static void main(String[] args) {
        SpringApplication.run(LikeApp.class, args);
    }
}
