package ru.practicum.ewm.user.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.exceptions.ConflictException;
import ru.practicum.ewm.exceptions.NotFoundException;
import ru.practicum.ewm.user.dto.NewUserRequest;
import ru.practicum.ewm.user.dto.UserDto;
import ru.practicum.ewm.user.mapper.UserMapper;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest newUser) {
        User user = userMapper.toUser(newUser);
        try {
            User saved = userRepository.save(user);
            return userMapper.toUserDto(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Email already exists: " + newUser.getEmail());
        }
    }

    @Override
    public UserDto getUserById(Long id) {
        User user = userRepository.findById(id)
                                  .orElseThrow(() -> new NotFoundException("User with id=" + id + " was not found"));
        return userMapper.toUserDto(user);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {

        List<User> users;

        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findAllByIdIn(ids, from, size, entityManager);
        } else {
            users = userRepository.findAll(from, size, entityManager);
        }

        return users.stream()
                    .map(userMapper::toUserDto)
                    .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new NotFoundException("User with id=" + id + " was not found");
        }
        userRepository.deleteById(id);
    }
}
