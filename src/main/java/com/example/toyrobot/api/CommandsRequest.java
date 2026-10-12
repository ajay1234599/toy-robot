package com.example.toyrobot.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Documents the POST /commands JSON body. The controller reads the raw JSON so it can accept several shapes. */
@Schema(description = "Send either `commands` (a string or a list of strings) or a single `command`")
public record CommandsRequest(
        @Schema(description = "Space- or newline-separated commands, or a JSON array of them",
                example = "PLACE 1,2,EAST MOVE MOVE LEFT MOVE REPORT")
        String commands,
        @Schema(description = "One command", example = "MOVE")
        String command) {
}
