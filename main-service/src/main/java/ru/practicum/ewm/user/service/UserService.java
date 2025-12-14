package ru.practicum.ewm.user.service;

import org.springframework.data.domain.Pageable;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest newUser);

    UserDto getUserById(Long id);

    List<UserDto> getUsers(List<Long> ids, Pageable pageable);

    void deleteUser(Long id);
}
