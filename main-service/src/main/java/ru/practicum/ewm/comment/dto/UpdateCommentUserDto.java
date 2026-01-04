package ru.practicum.ewm.comment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.comment.model.CommentState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCommentUserDto {

    private String text;

    private CommentState commentState;
}