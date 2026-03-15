package com.chessplatform.repository;

import com.chessplatform.model.Game;
import com.chessplatform.model.GameStatus;
import com.chessplatform.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional<Game> findByGameId(String gameId);

    List<Game> findAllByStatus(GameStatus status);

    @Query("SELECT g FROM Game g WHERE g.whitePlayer = :user OR g.blackPlayer = :user ORDER BY g.createdAt DESC")
    Page<Game> findByPlayer(@Param("user") User user, Pageable pageable);

    @Query("SELECT g FROM Game g WHERE g.status = :status ORDER BY g.createdAt DESC")
    Page<Game> findByStatus(@Param("status") GameStatus status, Pageable pageable);

    @Query("SELECT COUNT(g) FROM Game g WHERE (g.whitePlayer = :user OR g.blackPlayer = :user) AND g.status = 'COMPLETED'")
    long countCompletedGamesByUser(@Param("user") User user);
}
