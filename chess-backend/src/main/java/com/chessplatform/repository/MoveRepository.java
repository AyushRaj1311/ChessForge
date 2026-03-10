package com.chessplatform.repository;

import com.chessplatform.model.Game;
import com.chessplatform.model.Move;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MoveRepository extends JpaRepository<Move, Long> {
    List<Move> findByGameOrderByMoveNumberAsc(Game game);
    long countByGame(Game game);
}
