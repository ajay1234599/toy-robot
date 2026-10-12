package com.example.toyrobot.api;

import com.example.toyrobot.command.Command;
import com.example.toyrobot.command.Simulator;
import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.example.toyrobot.domain.Position;
import com.example.toyrobot.domain.Table;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Robot {

    private static final Logger log = LoggerFactory.getLogger(Robot.class);

    private final String id;
    private final Table table;
    private Position position;

    Robot(String id, Table table) {
        this.id = id;
        this.table = table;
    }

    public String id() {
        return id;
    }

    public Table table() {
        return table;
    }

    public synchronized Position position() {
        return position;
    }

    public synchronized Simulator.Run execute(List<Command> commands) {
        log.debug(LogMessages.BATCH_STARTED, id, commands.size(), Simulator.describe(position));
        Simulator.Run run = Simulator.run(table, position, commands);
        log.debug(LogMessages.BATCH_FINISHED, id, Simulator.describe(position), Simulator.describe(run.state()));
        position = run.state();
        return run;
    }
}
