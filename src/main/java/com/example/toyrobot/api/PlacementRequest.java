package com.example.toyrobot.api;

import com.example.toyrobot.domain.Direction;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Documents the PUT /placement body. The controller reads the raw JSON itself so it can reject
 * strings and decimals for x and y, which Jackson would otherwise coerce.
 */
@Schema(description = "Position to place the robot at")
public record PlacementRequest(
        @Schema(description = "Column, 0 (WEST edge) to 4", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        long x,
        @Schema(description = "Row, 0 (SOUTH edge) to 4", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        long y,
        @Schema(description = "Direction the robot faces (case-insensitive)", example = "EAST",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Direction facing) {
}
