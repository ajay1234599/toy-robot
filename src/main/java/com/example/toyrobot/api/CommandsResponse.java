package com.example.toyrobot.api;

import com.example.toyrobot.command.CommandResult;
import java.util.List;

public record CommandsResponse(List<String> outputs, List<CommandResult> results, RobotResponse robot) {
}
