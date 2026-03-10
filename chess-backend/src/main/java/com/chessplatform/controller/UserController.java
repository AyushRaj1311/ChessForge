package com.chessplatform.controller;

import com.chessplatform.dto.response.ApiResponse;
import com.chessplatform.dto.response.LeaderboardEntry;
import com.chessplatform.dto.response.UserProfileResponse;
import com.chessplatform.model.User;
import com.chessplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getMyProfile(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserProfile(user.getId())));
    }

    @GetMapping("/users/{username}")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(@PathVariable String username) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserProfileByUsername(username)));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntry>>> getLeaderboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(userService.getLeaderboard(page, size)));
    }

    // Admin endpoints
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminGetUsers() {
        return ResponseEntity.ok(ApiResponse.success("Admin endpoint - list users", "OK"));
    }
}
