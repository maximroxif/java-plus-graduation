package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import ru.practicum.client.LikeServiceClient;
import ru.practicum.client.LocationServiceClient;
import ru.practicum.client.RequestServiceClient;
import ru.practicum.client.StatClient;
import ru.practicum.client.UserServiceClient;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum", "ru.practicum.client"})
@EnableFeignClients(clients = {
        StatClient.class, UserServiceClient.class, LocationServiceClient.class,
        LikeServiceClient.class, RequestServiceClient.class})
public class EventApp {

    public static void main(String[] args) {
        SpringApplication.run(EventApp.class, args);

    }
}
