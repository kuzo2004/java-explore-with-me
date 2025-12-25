package ru.practicum.ewm.request.service;

import ru.practicum.ewm.event.dto.params.EventRequestUpdateParams;
import ru.practicum.ewm.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;

import java.util.List;
import java.util.Map;

public interface ParticipationRequestService {

    ParticipationRequestDto createRequest(Long userId, Long eventId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getRequestsForEvent(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateRequestsStatus(EventRequestUpdateParams params);

    long getConfirmedRequestsCount(Long eventId);

    Map<Long, Long> getConfirmedRequestsCountMap(List<Long> eventIds);

    Map<Long, Long> getAllRequestsCountMap(List<Long> eventIds);

    long getAllRequestsCount(Long eventId);
}
