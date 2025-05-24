package ru.practicum.main.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.user.dto.UserDto;
import ru.practicum.main.user.mapper.UserMapper;
import ru.practicum.main.user.model.User;
import ru.practicum.main.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto createUser(UserDto userDto) {
        log.info("Создание нового пользователя: {}", userDto.getEmail());

        try {
            User user = userRepository.save(userMapper.toEntity(userDto));
            log.info("Пользователь успешно создан с ID: {}", user.getId());
            return userMapper.toDto(user);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ConflictException("Пользователь с email " + userDto.getEmail() + " уже существует");
        }
    }

    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        PageRequest pageRequest = PageRequest.of(from / size, size);
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageRequest).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, pageRequest).getContent();
        }

        log.info("Получено {} пользователей", users.size());
        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        userRepository.deleteById(userId);
        log.info("Пользователь с ID {} успешно удален", userId);
    }
}
