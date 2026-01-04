package ru.practicum.ewm.comment.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentUserDto;
import ru.practicum.ewm.comment.dto.params.CommentSearchParams;
import ru.practicum.ewm.comment.model.CommentState;
import ru.practicum.ewm.comment.model.SortOrder;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users/{userId}")
public class PrivateCommentController {

    private final CommentService service;

    @PostMapping("/events/{eventId}/comments")
    public ResponseEntity<CommentDto> addComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid NewCommentDto dto) {
        CommentDto createdComment = service.addComment(userId, eventId, dto);
        return ResponseEntity.status(201).body(createdComment);
    }

    @PatchMapping("/events/{eventId}/comments/{commentId}")
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @PathVariable Long commentId,
            @RequestBody UpdateCommentUserDto dto) {
        CommentDto updatedComment = service.updateCommentByUser(userId, eventId, commentId, dto);
        return ResponseEntity.ok(updatedComment);
    }

    @GetMapping("/events/{eventId}/comments")
    public ResponseEntity<List<CommentDto>> getUserCommentsForEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestParam(required = false) CommentState state,
            @RequestParam(required = false, defaultValue = "ASC") String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {

        CommentSearchParams params = CommentSearchParams.builder()
                                                        .state(state)
                                                        .from(from)
                                                        .size(size)
                                                        .sort(SortOrder.from(sort))
                                                        .build();

        List<CommentDto> comments =
                service.getUserCommentsForEvent(userId, eventId, params);

        return ResponseEntity.ok(comments);
    }

    @GetMapping("/comments")
    public ResponseEntity<List<CommentDto>> getUserComments(
            @PathVariable Long userId,
            @RequestParam(required = false) CommentState state,
            @RequestParam(required = false, defaultValue = "ASC") String sort,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        CommentSearchParams params = CommentSearchParams.builder()
                                                        .state(state)
                                                        .from(from)
                                                        .size(size)
                                                        .sort(SortOrder.from(sort))
                                                        .build();

        List<CommentDto> comments =
                service.getUserComments(userId, params);

        return ResponseEntity.ok(comments);
    }

    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentDto> getUserCommentById(
            @PathVariable Long userId,
            @PathVariable Long commentId
    ) {
        CommentDto comment = service.getUserCommentById(userId, commentId);
        return ResponseEntity.ok(comment);
    }
}


