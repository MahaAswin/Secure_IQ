package com.secureiq.SecureIQ.user.service;

import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.BadRequestException;
import com.secureiq.SecureIQ.user.dto.UserCreateRequest;
import com.secureiq.SecureIQ.user.dto.UserPatchRequest;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import com.secureiq.SecureIQ.user.dto.UserUpdateRequest;
import com.secureiq.SecureIQ.user.mapper.UserMapper;
import com.secureiq.SecureIQ.user.model.Status;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository,
                            UserMapper userMapper,
                            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Page<UserResponse> getAll(String name, String email, Pageable pageable) {
        Page<User> users = userRepository.findAllFiltered(name, email, pageable);
        return users.map(userMapper::toResponse);
    }

    @Override
    public UserResponse getById(Long id) {
        User user = getEntityById(id);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse getByEmail(String email) {
        User user = getEntityByEmail(email);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered");
        }
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getEntityById(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            if (user.getRole() != request.getRole() || user.getStatus() != request.getStatus()) {
                throw new AccessDeniedException("Access denied: Non-admin users cannot change their role or status");
            }
        }

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered");
        }

        userMapper.updateEntity(request, user);

        if (StringUtils.hasText(request.getPassword())) {
            if (request.getPassword().length() < 6) {
                throw new BadRequestException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse patch(Long id, UserPatchRequest request) {
        User user = getEntityById(id);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            if ((request.getRole() != null && request.getRole() != user.getRole()) ||
                (request.getStatus() != null && request.getStatus() != user.getStatus())) {
                throw new AccessDeniedException("Access denied: Non-admin users cannot change their role or status");
            }
        }

        if (request.getEmail() != null && !user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new ConflictException("Email is already registered");
            }
        }

        userMapper.patchEntity(request, user);

        if (StringUtils.hasText(request.getPassword())) {
            if (request.getPassword().length() < 6) {
                throw new BadRequestException("Password must be at least 6 characters");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = getEntityById(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponse updateStatus(Long id, Status status) {
        User user = getEntityById(id);
        user.setStatus(status);
        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public User getEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    @Override
    public User getEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with email: " + email));
    }
}
