package com.example.toyrobot.api;

import com.example.toyrobot.domain.Position;

public record ReportResponse(String output, boolean placed, Position position) {
}
