package ru.practicum.ewm.comment.mapper;


import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.dto.UpdateCommentAdminDto;
import ru.practicum.ewm.comment.dto.UpdateCommentUserDto;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;


@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CommentMapper {

    @Mapping(target = "author", source = "author")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "commentState", source = "commentState")
    @Mapping(target = "createdOn", source = "createdOn")
    @Mapping(target = "updatedOn", source = "updatedOn")
    CommentDto toDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "newCommentDto.text")
    Comment toEntity(NewCommentDto newCommentDto, User author, Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateByUser(UpdateCommentUserDto dto,
                      @MappingTarget Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateByAdmin(UpdateCommentAdminDto dto,
                       @MappingTarget Comment comment);
}