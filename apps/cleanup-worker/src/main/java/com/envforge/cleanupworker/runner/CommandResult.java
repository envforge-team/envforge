package com.envforge.cleanupworker.runner;

public record CommandResult(
        boolean successful,
        int exitCode,
        String output,
        String error
) {

    public static CommandResult success(String output) {
        return new CommandResult(true, 0, output, "");
    }

    public static CommandResult failure(int exitCode, String error) {
        return new CommandResult(false, exitCode, "", error);
    }
}
