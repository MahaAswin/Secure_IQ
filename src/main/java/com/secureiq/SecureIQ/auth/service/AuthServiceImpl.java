package com.secureiq.SecureIQ.auth.service;

import com.secureiq.SecureIQ.auth.dto.AuthResponse;
import com.secureiq.SecureIQ.auth.dto.LoginRequest;
import com.secureiq.SecureIQ.auth.dto.RegisterRequest;
import com.secureiq.SecureIQ.common.exception.ConflictException;
import com.secureiq.SecureIQ.common.exception.NotFoundException;
import com.secureiq.SecureIQ.common.exception.UnauthorizedException;
import com.secureiq.SecureIQ.security.jwt.JwtTokenProvider;
import com.secureiq.SecureIQ.user.dto.UserResponse;
import com.secureiq.SecureIQ.user.model.User;
import com.secureiq.SecureIQ.user.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager,
                           JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email is already registered");
        }

        String firstName = request.getFirstName();
        String lastName = request.getLastName();

        if ((firstName == null || firstName.trim().isEmpty()) && (lastName == null || lastName.trim().isEmpty())) {
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                throw new com.secureiq.SecureIQ.common.exception.BadRequestException("First name and last name, or full name is required");
            }
            String fullName = request.getName().trim();
            int lastSpaceIndex = fullName.lastIndexOf(' ');
            if (lastSpaceIndex > 0) {
                firstName = fullName.substring(0, lastSpaceIndex).trim();
                lastName = fullName.substring(lastSpaceIndex + 1).trim();
            } else {
                firstName = fullName;
                lastName = "";
            }
        } else {
            if (firstName == null) firstName = "";
            if (lastName == null) lastName = "";
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .status(com.secureiq.SecureIQ.user.model.Status.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        String token = tokenProvider.generateToken(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = tokenProvider.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setLastLogin(java.time.LocalDateTime.now());
        User savedUser = userRepository.save(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(UserResponse.fromEntity(savedUser))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMe() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedException("User is not authenticated");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.fromEntity(user);
    }
}
