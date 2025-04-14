package ru.practicum.ewm.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@RequiredArgsConstructor
public class Constants {

    public static final String JSON_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

}
