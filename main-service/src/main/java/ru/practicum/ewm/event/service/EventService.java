package ru.practicum.ewm.event.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.dto.params.EventAdminSearchParams;
import ru.practicum.ewm.event.dto.params.EventParams;
import ru.practicum.ewm.event.dto.params.EventSearchParams;

import java.util.List;

public interface EventService {

    EventFullDto createEvent(Long userId, NewEventDto dto);

    EventFullDto updateEvent(EventParams params);

    EventFullDto getUserEvent(Long userId, Long eventId);

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    List<EventFullDto> searchEvents(EventAdminSearchParams params, int from, int size);

    List<EventShortDto> searchPublicEvents(EventSearchParams params, int from, int size);

    EventFullDto getPublicEventById(Long id);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);
}
