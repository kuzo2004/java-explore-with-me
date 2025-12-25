package ru.practicum.ewm.request.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;

@Mapper(componentModel = "spring")
public interface ParticipationRequestMapper {

    // Важно: здесь маппим только event.id и requester.id.
    // LAZY-прокси Hibernate позволяет получить ID без дополнительных запросов.
    // Будьте внимательны: обращение к другим полям Event/User может вызвать N+1.
    @Mapping(target = "event", source = "event.id")
    @Mapping(target = "requester", source = "requester.id")
    // автоматически преобразовать enum в строку,
    @Mapping(target = "status", source = "status")
    ParticipationRequestDto toDto(ParticipationRequest request);
}

