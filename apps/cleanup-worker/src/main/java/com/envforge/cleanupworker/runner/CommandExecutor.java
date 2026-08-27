package com.envforge.cleanupworker.runner;

import java.time.Duration;
import java.util.List;

public interface CommandExecutor {
    CommandResult execute(List<String> command, Duration timeout);
}
