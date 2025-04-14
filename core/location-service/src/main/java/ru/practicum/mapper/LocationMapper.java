package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.location.LocationDto;
import ru.practicum.model.Location;


@Mapper(componentModel = "spring")
public interface LocationMapper {

    LocationDto locationToLocationDto(Location location);

    Location locationDtoToLocation(LocationDto locationDto);
}
