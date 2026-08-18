package com.pawlingo.api.user.service;

import com.pawlingo.api.user.dto.response.UserSummaryResponse;
import com.pawlingo.api.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserSummaryResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(user -> new UserSummaryResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getGoal(),
                        user.getAuthProvider(),
                        user.getGoogleId(),
                        user.getCreatedAt()))
                .toList();
    }
}
