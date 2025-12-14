package ru.practicum.ewm.event.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.practicum.ewm.category.mapper.CategoryMapper;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.UpdateEventUserRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.mapper.UserMapper;

@Mapper(
        componentModel = "spring",
        uses = {CategoryMapper.class, UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EventMapper {

    // ----------------------------
    // Event -> DTO
    // ----------------------------
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    EventShortDto toEventShortDto(Event event);

    @Mapping(target = "views", ignore = true)
    @Mapping(target = "confirmedRequests", ignore = true)
    EventFullDto toEventFullDto(Event event);

    // ----------------------------
    // NewEventDto -> Event
    // ----------------------------

    // устанавливается в сервисе
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "initiator", ignore = true)

    // Чтобы MapStruct не затирал значения по умолчанию
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "eventDate", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    Event toEvent(NewEventDto dto);

    // ----------------------------
    // Update mappings
    // если поле в DTO = null → НЕ менять его в сущности
    // ----------------------------
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // устанавливается в сервисе
    @Mapping(target = "category", ignore = true)
    // state и publishedOn никогда не приходят из DTO, но чтобы гарантированно избежать ошибок
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    void updateEventFromUserRequest(UpdateEventUserRequest dto, @MappingTarget Event event);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    // управляется вручную в сервисе
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "state", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    void updateEventFromAdminRequest(UpdateEventAdminRequest dto, @MappingTarget Event event);
}
