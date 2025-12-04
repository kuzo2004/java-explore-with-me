package ru.practicum.ewm.mapper;

import org.mapstruct.Mapper;
import ru.practicum.ewm.dto.EndpointHitDto;
import ru.practicum.ewm.model.EndpointHit;

@Mapper(componentModel = "spring")
public interface EndpointHitMapper {

    // DTO → Entity
    EndpointHit toEntity(EndpointHitDto dto);

    // Entity → DTO
    EndpointHitDto toDto(EndpointHit entity);
}
