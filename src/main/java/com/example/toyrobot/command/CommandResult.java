package com.example.toyrobot.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CommandResult(String command, Status status, Reason reason, String output) {

    public enum Status { APPLIED, IGNORED }

    public enum Reason { NOT_PLACED, OFF_TABLE, WOULD_FALL }

    static CommandResult applied(Command command) {
        return new CommandResult(command.label(), Status.APPLIED, null, null);
    }

    static CommandResult applied(Command command, String output) {
        return new CommandResult(command.label(), Status.APPLIED, null, output);
    }

    static CommandResult ignored(Command command, Reason reason) {
        return new CommandResult(command.label(), Status.IGNORED, reason, null);
    }

    static CommandResult ignored(Command command, Reason reason, String output) {
        return new CommandResult(command.label(), Status.IGNORED, reason, output);
    }

    @JsonIgnore
    public boolean isIgnored() {
        return status == Status.IGNORED;
    }
}
