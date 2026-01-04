package ru.practicum.ewm.comment.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentAdminDto;
import ru.practicum.ewm.comment.dto.UpdateCommentUserDto;
import ru.practicum.ewm.comment.dto.params.CommentSearchParams;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.model.CommentState;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exceptions.BadRequestException;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipationRequestRepository participationRequestRepository;
    private final CommentMapper mapper;
    private final EntityManager entityManager;


    // -------------------------
    // Private API Methods
    // -------------------------

    //Private API: Add comment for a user to a specific event
    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long eventId, NewCommentDto dto) {

        User user = userRepository.findById(userId)
                                  .orElseThrow(() -> new NotFoundException("User not found"));

        Event event = eventRepository.findById(eventId)
                                     .orElseThrow(() -> new NotFoundException("Event not found"));

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot comment unpublished event");
        }

        if (!participationRequestRepository.existsByEventIdAndRequesterIdAndStatus(
                eventId,
                userId,
                RequestStatus.CONFIRMED
        )) {
            throw new ConflictException("User is not a confirmed participant of this event");
        }

        Comment comment = Comment.builder()
                                 .text(dto.getText())
                                 .author(user)
                                 .event(event)
                                 .createdOn(LocalDateTime.now())
                                 .commentState(CommentState.PUBLISHED)
                                 .build();

        return mapper.toDto(commentRepository.save(comment));
    }


    // Private API: Update a comment by the user
    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long userId, Long eventId, Long commentId, UpdateCommentUserDto dto) {

        Comment comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Comment does not belong to the user");
        }

        if (!comment.getEvent().getId().equals(eventId)) {
            throw new ConflictException("Comment does not belong to the event");
        }

        boolean isTextUpdate = dto.getText() != null;
        boolean isStateUpdate = dto.getCommentState() != null;

        if (isTextUpdate && isStateUpdate) {
            throw new ConflictException("Cannot update both text and state at the same time");
        }

        boolean updated = false;

        if (isTextUpdate) {
            String newText = dto.getText();
            if (newText.length() < 2 || newText.length() > 2000) {
                throw new BadRequestException("Text length must be between 2 and 2000 characters");
            }
            if (!newText.equals(comment.getText())) {
                comment.setText(newText);
                updated = true;
            }
        }

        if (isStateUpdate && dto.getCommentState() != comment.getCommentState()) {
            comment.setCommentState(dto.getCommentState());
            updated = true;
        }

        if (updated) {
            comment.setUpdatedOn(LocalDateTime.now());
            commentRepository.save(comment);
        }

        return mapper.toDto(comment);
    }


    // Private API: Get all comments of a user for a specific event
    @Override
    public List<CommentDto> getUserCommentsForEvent(Long userId, Long eventId, CommentSearchParams params) {

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException("User not found"));

        eventRepository.findById(eventId)
                       .orElseThrow(() -> new NotFoundException("Event not found"));

        List<Comment> comments = commentRepository.searchComments(userId, eventId, params, entityManager);

        return comments.stream()
                       .map(mapper::toDto)
                       .toList();
    }

    // Private API: Get all comments of a user across all events
    @Override
    public List<CommentDto> getUserComments(Long userId, CommentSearchParams params) {

        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException("User not found"));

        List<Comment> comments =
                commentRepository.searchUserComments(userId, params, entityManager);

        return comments.stream()
                       .map(mapper::toDto)
                       .toList();
    }

    // Private API: Get specific comments of a user for a specific event
    @Override
    public CommentDto getUserCommentById(Long userId, Long commentId) {
        userRepository.findById(userId)
                      .orElseThrow(() -> new NotFoundException("User not found"));

        Comment comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new NotFoundException("Comment not found"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new ConflictException("Comment does not belong to this user");
        }

        return mapper.toDto(comment);
    }


    // -------------------------
    // Admin API Methods
    // -------------------------

    // Admin API: Update a comment by the admin
    @Override
    @Transactional
    public CommentDto updateCommentByAdmin(Long commentId, UpdateCommentAdminDto dto) {

        Comment comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new NotFoundException("Comment not found"));


        // Обновляем текст, если пришёл, причем можно пустой текст
        if (dto.getText() != null) {
            String newText = dto.getText();
            if (newText.length() > 2000) {
                throw new BadRequestException("Text length must <= 2000 characters");
            }
            if (!newText.equals(comment.getText())) {
                comment.setText(newText);
            }
        }

        // Обновляем статус, если пришёл
        if (dto.getCommentState() != null && dto.getCommentState() != comment.getCommentState()) {
            comment.setCommentState(dto.getCommentState());
        }

        // ---- createdOn (ТОЛЬКО ДЛЯ АДМИНА) ----
        if (dto.getCreatedOn() != null &&
                !dto.getCreatedOn().equals(comment.getCreatedOn())) {

            comment.setCreatedOn(dto.getCreatedOn());
        }

        // ---- updatedOn (если админ передал руками) ----
        if (dto.getUpdatedOn() != null &&
                !dto.getUpdatedOn().equals(comment.getUpdatedOn())) {

            comment.setUpdatedOn(dto.getUpdatedOn());
        }

        // Если меняли ТЕКСТ или СТАТУС — обновляем updatedOn автоматически
        if (dto.getText() != null || dto.getUpdatedOn() == null) {
            comment.setUpdatedOn(LocalDateTime.now());
        }

        commentRepository.save(comment);

        return mapper.toDto(comment);
    }

    // Admin API: Delete a comment by the admin
    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new NotFoundException("Comment not found"));

        commentRepository.delete(comment);
    }


    // Admin API: Get all comments, optionally filtered by user and/or event
    @Override
    public List<CommentDto> getAllCommentsAdmin(Long userId, Long eventId, CommentSearchParams params) {

        // проверяем существование пользователя (если передан userId)
        if (userId != null) {
            userRepository.findById(userId)
                          .orElseThrow(() -> new NotFoundException("User not found"));
        }

        // проверяем существование события (если передан eventId)
        if (eventId != null) {
            eventRepository.findById(eventId)
                           .orElseThrow(() -> new NotFoundException("Event not found"));
        }

        List<Comment> comments = commentRepository.searchCommentsAdmin(userId, eventId, params, entityManager);

        return comments.stream()
                       .map(mapper::toDto)
                       .toList();
    }

    // Admin API: Get a specific comment by ID (admin)
    @Override
    public CommentDto getCommentByIdAdmin(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                                           .orElseThrow(() -> new NotFoundException("Comment not found"));
        return mapper.toDto(comment);
    }


    // -------------------------
    // Private API Methods
    // -------------------------

    // Private API: Get all published comments for a specific event (publicly visible)
    @Override
    public List<CommentDto> getEventComments(Long eventId, CommentSearchParams params) {
        return commentRepository.searchEventCommentsPublic(eventId, params, entityManager)
                                .stream()
                                .map(mapper::toDto)
                                .toList();
    }

    // Возвращает количество опубликованных (PUBLISHED) комментариев для события по его id.
    public long countPublishedByEventId(Long id) {
        return commentRepository.countByEventIdAndCommentState(
                id,
                CommentState.PUBLISHED
        );
    }


    // Возвращает мапу - количество опубликованных (PUBLISHED) комментариев для списка событий.
    public Map<Long, Long> countPublishedByEventIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        return commentRepository.countPublishedByEventIds(ids, entityManager);
    }
}