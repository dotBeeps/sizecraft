# SizeCraft — NeoForge/Kotlin Minecraft mod. Plain `just` lists recipes.

default:
    @just --list --unsorted

# One-time setup: install lefthook git hooks and the commit message template
setup:
    lefthook install
    git config commit.template .gitmessage

# Compile and package the mod jar (build/libs/sizecraft-*.jar)
build:
    ./gradlew build

# Run the JUnit test suite
test:
    ./gradlew test

# Gradle verification suite (aggregates test); config cache off, matching the pre-push/CI gate
check:
    ./gradlew check -Dorg.gradle.configuration-cache=false

# Non-mutating full gate, same as GitHub CI: gradle check + build
ci:
    ./gradlew check build -Dorg.gradle.configuration-cache=false

# Launch the dev client with the mod loaded
run:
    ./gradlew runClient

# Launch the dev server (nogui) with the mod loaded
run-server:
    ./gradlew runServer

# Delete Gradle build outputs
clean:
    ./gradlew clean
