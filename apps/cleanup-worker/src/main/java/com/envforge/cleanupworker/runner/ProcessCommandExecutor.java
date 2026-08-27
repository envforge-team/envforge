package com.envforge.cleanupworker.runner;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class ProcessCommandExecutor implements CommandExecutor {

    @Override
    public CommandResult execute(List<String> command, Duration timeout) {
        Process process;

        try {
            process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException exception) {
            return CommandResult.failure(
                    127,
                    "Failed to start command: " + exception.getMessage()
            );
        }

        StringBuilder output = new StringBuilder();

        Thread reader = Thread.startVirtualThread(() -> {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            )) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            } catch (IOException exception) {
                output.append("Output read error: ")
                        .append(exception.getMessage())
                        .append(System.lineSeparator());
            }
        });

        try {
            boolean finished = process.waitFor(
                    timeout.toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!finished) {
                process.destroyForcibly();
                reader.join(2000);

                return CommandResult.failure(
                        124,
                        "Command timed out after "
                                + timeout.toSeconds()
                                + " seconds"
                );
            }

            reader.join(2000);

            String text = output.toString().trim();
            int exitCode = process.exitValue();

            if (exitCode == 0) {
                return CommandResult.success(text);
            }

            return CommandResult.failure(exitCode, text);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();

            return CommandResult.failure(
                    130,
                    "Command execution interrupted"
            );
        }
    }
}
