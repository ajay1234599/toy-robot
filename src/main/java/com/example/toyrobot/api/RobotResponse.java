package com.example.toyrobot.api;

import com.example.toyrobot.command.Simulator;
import com.example.toyrobot.constant.AppConstants.ApiPaths;
import com.example.toyrobot.constant.AppConstants.JsonFields;
import com.example.toyrobot.domain.Position;
import com.example.toyrobot.domain.Table;
import java.util.Map;

public record RobotResponse(
        String id,
        boolean placed,
        Position position,
        String report,
        Table table,
        Map<String, String> links) {


    static RobotResponse of(Robot robot, Position position) {
        String self = ApiPaths.robot(robot.id());
        return new RobotResponse(
                robot.id(),
                position != null,
                position,
                position != null ? position.report() : Simulator.ROBOT_MISSING,
                robot.table(),
                Map.of(
                        JsonFields.LINK_SELF, self,
                        JsonFields.LINK_PLACEMENT, self + ApiPaths.PLACEMENT,
                        JsonFields.LINK_COMMANDS, self + ApiPaths.COMMANDS,
                        JsonFields.LINK_REPORT, self + ApiPaths.REPORT));
    }

    static RobotResponse of(Robot robot) {
        return of(robot, robot.position());
    }
}
