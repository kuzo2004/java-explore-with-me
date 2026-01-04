package ru.practicum.ewm.comment.dto.params;

import lombok.Builder;
import lombok.Data;
import ru.practicum.ewm.comment.model.CommentState;
import ru.practicum.ewm.comment.model.SortOrder;

@Data
@Builder
public class CommentSearchParams {

    private CommentState state;
    private Integer from;
    private Integer size;
    private SortOrder sort;
}