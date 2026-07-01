#!/bin/sh
# Launches the Tunnel Bore dev client (a test Minecraft instance with the mod loaded).
#
# Your system `java` is Java 8, which can't build/run MC 1.21. This points Gradle at
# the Homebrew JDK 21 instead. Double-click this file in Finder, or run ./dev-run.command
# from Terminal. Pass "runServer" as an arg to launch a dev server instead of a client.
cd "$(dirname "$0")" || exit 1
TASK="${1:-runClient}"
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home exec ./gradlew "$TASK"
