package com.example.toyrobot.api;

import com.example.toyrobot.constant.AppConstants.LogMessages;
import com.example.toyrobot.domain.Table;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

/** In-memory store of robots. Each robot is an independent simulation. */
@Repository
public class RobotRepository {

    private static final Logger log = LoggerFactory.getLogger(RobotRepository.class);

    private final Map<String, Robot> robots = new ConcurrentHashMap<>();

    public Robot create() {
        Robot robot = new Robot(UUID.randomUUID().toString(), Table.STANDARD);
        robots.put(robot.id(), robot);
        log.debug(LogMessages.REPOSITORY_SAVED, robot.id(), robots.size());
        return robot;
    }

    public Optional<Robot> find(String id) {
        Robot robot = robots.get(id);
        if (robot == null) {
            log.debug(LogMessages.REPOSITORY_MISS, id);
        } else {
            log.debug(LogMessages.REPOSITORY_HIT, id);
        }
        return Optional.ofNullable(robot);
    }

    public Collection<Robot> findAll() {
        log.debug(LogMessages.REPOSITORY_LIST, robots.size());
        return robots.values();
    }

    public boolean delete(String id) {
        boolean removed = robots.remove(id) != null;
        if (removed) {
            log.debug(LogMessages.REPOSITORY_REMOVED, id, robots.size());
        } else {
            log.debug(LogMessages.REPOSITORY_REMOVE_MISS, id);
        }
        return removed;
    }
}
