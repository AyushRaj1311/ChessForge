package com.chessplatform.repository;

import com.chessplatform.model.GameMode;
import com.chessplatform.model.MatchmakingEntry;
import com.chessplatform.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchmakingEntryRepository extends JpaRepository<MatchmakingEntry, Long> {
    Optional<MatchmakingEntry> findByUserAndActiveTrue(User user);

    @Query("SELECT e FROM MatchmakingEntry e WHERE e.gameMode = :mode AND e.active = true " +
           "AND e.user != :user ORDER BY e.enqueuedAt ASC")
    List<MatchmakingEntry> findActiveByMode(@Param("mode") GameMode mode, @Param("user") User user);
}
