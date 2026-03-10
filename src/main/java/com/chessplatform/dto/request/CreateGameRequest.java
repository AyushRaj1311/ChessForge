package com.chessplatform.dto.request;

import com.chessplatform.model.GameMode;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGameRequest {
    @NotNull
    private GameMode gameMode;

    private boolean vsAi = false;
}
