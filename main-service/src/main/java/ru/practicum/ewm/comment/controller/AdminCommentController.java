package ru.practicum.ewm.comment.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentAdminDto;
import ru.practicum.ewm.comment.dto.params.CommentSearchParams;
import ru.practicum.ewm.comment.model.CommentState;
import ru.practicum.ewm.comment.model.SortOrder;
import ru.practicum.ewm.comment.service.CommentService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminCommentController {

    private final CommentService service;

    // Обновление комментария админом (текст и/или статус)
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentDto> updateCommentByAdmin(
            @PathVariable Long commentId,
            @RequestBody UpdateCommentAdminDto dto) {
        CommentDto updated = service.updateCommentByAdmin(commentId, dto);
        return ResponseEntity.ok(updated);
    }

    // Физическое удаление комментария
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        service.deleteCommentByAdmin(commentId);
        return ResponseEntity.noContent().build();
    }

    // Получение всех комментариев (с фильтрацией, сортировкой и пагинацией)
    @GetMapping("/comments")
    public ResponseEntity<List<CommentDto>> getAllComments(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long eventId,
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

        List<CommentDto> comments = service.getAllCommentsAdmin(userId, eventId, params);

        return ResponseEntity.ok(comments);
    }

    // Получение конкретного комментария по id
    @GetMapping("/comments/{commentId}")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Long commentId) {
        CommentDto comment = service.getCommentByIdAdmin(commentId);
        return ResponseEntity.ok(comment);
    }
}


