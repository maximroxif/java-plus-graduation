package ru.practicum.ewm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import ru.practicum.client.StatClient;

@SpringBootApplication
@ComponentScan(basePackages = {"ru.practicum.ewm", "ru.practicum.client"})
@EnableFeignClients(clients = {StatClient.class})
public class MainServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(MainServiceApp.class, args);

    }
}
