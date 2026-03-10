package com.chessplatform.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MakeMoveRequest {
    @NotBlank
    private String from;    // e.g. "e2"

    @NotBlank
    private String to;      // e.g. "e4"

    private String promotion; // Q, R, B, N
}
