package com.chessplatform.service;

import com.chessplatform.dto.response.LeaderboardEntry;
import com.chessplatform.dto.response.UserProfileResponse;
import com.chessplatform.exception.ResourceNotFoundException;
import com.chessplatform.model.User;
import com.chessplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = username == null ? "" : username.trim();
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(normalized, normalized)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserProfileResponse getUserProfile(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return mapToProfile(user);
    }

    public UserProfileResponse getUserProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return mapToProfile(user);
    }

    public List<LeaderboardEntry> getLeaderboard(int page, int size) {
        Page<User> users = userRepository.findTopPlayersByRating(PageRequest.of(page, size));
        AtomicInteger rank = new AtomicInteger(page * size + 1);
        return users.stream()
            .map(u -> LeaderboardEntry.builder()
                .rank(rank.getAndIncrement())
                .userId(u.getId())
                .username(u.getUsername())
                .rating(u.getRating())
                .gamesPlayed(u.getGamesPlayed())
                .gamesWon(u.getGamesWon())
                .winRate(u.getGamesPlayed() > 0
                    ? (double) u.getGamesWon() / u.getGamesPlayed() * 100 : 0)
                .build())
            .toList();
    }

    @Transactional
    public RatingChange updateRatingAfterGame(User winner, User loser, boolean isDraw) {
        int kFactor = 32;
        int ratingChange;
        if (isDraw) {
            ratingChange = calculateEloChange(winner.getRating(), loser.getRating(), 0.5, kFactor);
            winner.setRating(Math.max(100, winner.getRating() + ratingChange));
            loser.setRating(Math.max(100, loser.getRating() - ratingChange));
            winner.setGamesDraw(winner.getGamesDraw() + 1);
            loser.setGamesDraw(loser.getGamesDraw() + 1);
        } else {
            ratingChange = calculateEloChange(winner.getRating(), loser.getRating(), 1.0, kFactor);
            winner.setRating(Math.max(100, winner.getRating() + ratingChange));
            loser.setRating(Math.max(100, loser.getRating() - ratingChange));
            winner.setGamesWon(winner.getGamesWon() + 1);
            loser.setGamesLost(loser.getGamesLost() + 1);
        }
        winner.setGamesPlayed(winner.getGamesPlayed() + 1);
        loser.setGamesPlayed(loser.getGamesPlayed() + 1);
        userRepository.save(winner);
        userRepository.save(loser);
        return new RatingChange(ratingChange, -ratingChange);
    }

    public record RatingChange(int winnerChange, int loserChange) {}

    private int calculateEloChange(int ratingA, int ratingB, double score, int k) {
        double expected = 1.0 / (1.0 + Math.pow(10, (ratingB - ratingA) / 400.0));
        return (int) Math.round(k * (score - expected));
    }

    private UserProfileResponse mapToProfile(User user) {
        return UserProfileResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole())
            .rating(user.getRating())
            .gamesPlayed(user.getGamesPlayed())
            .gamesWon(user.getGamesWon())
            .gamesLost(user.getGamesLost())
            .gamesDraw(user.getGamesDraw())
            .winRate(user.getGamesPlayed() > 0
                ? (double) user.getGamesWon() / user.getGamesPlayed() * 100 : 0)
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .build();
    }
}
