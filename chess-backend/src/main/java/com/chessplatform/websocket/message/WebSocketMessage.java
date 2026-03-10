package com.chessplatform.websocket.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketMessage {
    private String type;    // MOVE, JOIN, RESIGN, DRAW_OFFER, CHAT
    private String gameId;
    private Object payload;
    private String sender;
    private long timestamp;
}
