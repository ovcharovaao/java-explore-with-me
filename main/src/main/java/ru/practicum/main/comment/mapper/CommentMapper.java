package ru.practicum.main.comment.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.main.comment.dto.CommentDto;
import ru.practicum.main.comment.dto.NewCommentDto;
import ru.practicum.main.comment.model.Comment;
import ru.practicum.main.event.model.Event;
import ru.practicum.main.user.mapper.UserMapper;
import ru.practicum.main.user.model.User;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "event", source = "event")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "createdOn", expression = "java(java.time.LocalDateTime.now())")
    @Mapping(target = "editedOn", ignore = true)
    Comment toComment(NewCommentDto newCommentDto, Event event, User author);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "author", source = "author", qualifiedByName = "toUserShortDto")
    CommentDto toCommentDto(Comment comment);
}
