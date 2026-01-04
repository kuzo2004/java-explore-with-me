package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.params.CommentSearchParams;
import ru.practicum.ewm.comment.model.CommentState;
import ru.practicum.ewm.comment.model.SortOrder;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events/{eventId}/comments")
public class PublicCommentController {

    private final CommentService service;

    @GetMapping
    public ResponseEntity<List<CommentDto>> getPublishedComments(
            @PathVariable Long eventId,
            @RequestParam(defaultValue = "ASC") String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        // Всегда только PUBLISHED комментарии для публичного API
        CommentSearchParams params = CommentSearchParams.builder()
                                                        .state(CommentState.PUBLISHED)
                                                        .from(from)
                                                        .size(size)
                                                        .sort(SortOrder.from(sort))
                                                        .build();

        List<CommentDto> comments = service.getEventComments(eventId, params);

        return ResponseEntity.ok(comments);
    }
}


