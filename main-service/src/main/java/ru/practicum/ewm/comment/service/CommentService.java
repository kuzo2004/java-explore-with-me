package ru.practicum.ewm.comment.service;

import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentAdminDto;
import ru.practicum.ewm.comment.dto.UpdateCommentUserDto;
import ru.practicum.ewm.comment.dto.params.CommentSearchParams;

import java.util.List;
import java.util.Map;

public interface CommentService {

    CommentDto addComment(
            Long userId,
            Long eventId,
            NewCommentDto dto
    );

    List<CommentDto> getEventComments(
            Long eventId,
            CommentSearchParams params
    );

    CommentDto updateCommentByUser(
            Long userId,
            Long eventId,
            Long commentId,
            UpdateCommentUserDto dto
    );

    CommentDto updateCommentByAdmin(
            Long commentId,
            UpdateCommentAdminDto dto
    );

    void deleteCommentByAdmin(Long commentId);

    List<CommentDto> getUserCommentsForEvent(
            Long userId,
            Long eventId,
            CommentSearchParams params
    );

    List<CommentDto> getUserComments(
            Long userId,
            CommentSearchParams params
    );

    CommentDto getUserCommentById(
            Long userId,
            Long commentId
    );

    List<CommentDto> getAllCommentsAdmin(
            Long userId, 
            Long eventId, 
            CommentSearchParams params);

    CommentDto getCommentByIdAdmin(Long commentId);

    long countPublishedByEventId(Long id);

    Map<Long, Long> countPublishedByEventIds(List<Long> ids);
}