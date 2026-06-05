# FuryKarma

[Leer en Español (README_ES.md)](README_ES.md)

**FuryKarma** is a modern and lightweight Minecraft Paper plugin (supporting 26.1.2+ and Java 25) designed to manage player karma. Players can give positive or negative ratings to others with a reason, view detailed profiles, check a leaderboard of the best-rated players, and customize all messages.

It supports both cracked (offline-mode) and normal (online-mode) servers, integrating natively with **nLogin** to ensure players must authenticate before they can manage karma. It stores all information securely and performantly using an embedded **SQLite** database.

---

## Features

- **Karma Voting**: Let players rate others using `/karma <positive|negative> <player> <reason>`.
- **Comprehensive Profile Info**: Players can query profiles (with `/karma info`) to view net karma, average positive rating, and a history of recent received ratings (relative timestamps included, e.g., `2h ago`).
- **Leaderboard (Top 10)**: Displays the top 10 players sorted by their positive average rating, including positive/negative counts.
- **Embedded SQLite Storage**: Efficient database queries for counts and leaderboard without bloat.
- **nLogin Compatibility**: Safely restricts command usage for unauthenticated players.
- **Customizable Cooldowns**: Set a cooldown period (defaulting to 24 hours) between votes to prevent spam. Can be bypassed with `furykarma.bypass.cooldown`.
- **Advanced Message Formatting**: Supports classic legacy ampersand codes (e.g., `&e`) and modern Adventure MiniMessage tags (e.g., `<green>Text</green>`).
- **Modern Brigadier System**: Dynamic command registration via Paper's Brigadier API, featuring smart tab-completion.

---

## Requirements

- **Server**: PaperMC, Purpur, or any other fork compatible with version **26.1.2** or higher.
- **Java**: Version **25** (Eclipse Temurin recommended).
- **Authentication (Optional)**: nLogin (fully supported for cracked networks).

---

## Installation & Compilation

1. Clone or download this project repository.
2. Open your terminal in the project's root directory.
3. Compile the JAR using the Gradle wrapper:
   ```bash
   ./gradlew build
   ```
4. The compiled JAR artifact will be generated at:
   `app/build/libs/FuryKarma.jar`
5. Copy `FuryKarma.jar` into your server's `plugins/` directory and restart the server.

---

## Configuration (`config.yml`)

The configuration file is automatically created at `plugins/FuryKarma/config.yml` on the first startup:

```yaml
# FuryKarma Configuration

# Cooldown in hours between giving karma.
# By default, a player can give karma every 24 hours.
cooldown-hours: 24

# Configuration of all in-game messages.
# You can use standard Bukkit color codes (e.g., &a, &c, &e) or MiniMessage tags (e.g., <green>, <red>, <gold>).
messages:
  prefix: "&e[&bFuryKarma&e] &r"
  no-permission: "&cYou do not have permission to execute this command."
  only-players: "&cOnly players can execute this command. Use /karma info <player> from the console."
  player-not-found: "&cPlayer &e%player% &chas never played on this server."
  cooldown: "&cYou must wait &e%time% &cbefore you can give karma again."
  cannot-self-karma: "&cYou cannot give karma to yourself!"
  karma-given-sender: "&aYou have given a &e%type% &akarma to &b%player% &afor: &7%reason%"
  karma-received-target: "&aYou have received a &e%type% &akarma from &b%player% &afor: &7%reason%"
  invalid-karma-type: "&cInvalid karma type! Use &e/karma <positive|negative> <player> <reason>&c."
  not-authenticated: "&cYou must login before using karma commands."
  config-reloaded: "&aConfiguration reloaded successfully."
  missing-arguments: "&cUsage: &e/karma <positive|negative> <player> <reason>"
  missing-reason: "&cYou must provide a reason for giving karma."
  positive-label: "&a&lPOSITIVE"
  negative-label: "&c&lNEGATIVE"

  # Help command formatting
  help:
    header: "&e&m-------&r &b&lFuryKarma Help &e&m-------"
    give: " &e/karma <positive|negative> <player> <reason> &7- Give karma to a player"
    info-self: " &e/karma info &7- Show your karma information"
    info-other: " &e/karma info <player> &7- Show another player's karma"
    info-top: " &e/karma top &7- Show the top 10 karma leaderboard"
    info-help: " &e/karma help &7- Show this help menu"
    footer: "&e&m----------------------------"

  # Info command formatting
  info:
    header: "&e&m-------&r &b&lKarma Info: &e%player% &e&m-------"
    total: "&7Total Karma (Net): &6%total%"
    average: "&7Average Rating: &b%average% &8(&a+%positives% &7/ &c-%negatives%&8)"
    no-logs: "&7No karma records received yet."
    logs-header: "&eRecent received ratings:"
    log-entry: " &8- %type% &7by &b%sender% &8(&e%time% ago&8): &7%reason%"
    footer: "&e&m----------------------------"

  # Top command formatting
  top:
    header: "&e&m-------&r &b&lTop 10 Karma Leaderboard &e&m-------"
    entry: "&e%index%. &b%player% &7- Average: &b%average% &8(&6Net: %net%&8, &a+%positives%&7/&c-%negatives%&8)"
    no-data: "&7No karma data recorded yet."
    footer: "&e&m---------------------------------------"
```

---

## Commands & Permissions

### Commands
- `/karma <positive|negative> <player> <reason>` - Gives positive or negative karma to another player with a reason. (Alias: `/furykarma ...`)
- `/karma info` - View your own karma profile (net karma, average, and last 5 received logs).
- `/karma info <player>` - View another player's karma profile (net karma, average, and last 10 received logs).
- `/karma top` - Displays the top 10 players ordered by average karma.
- `/karma help` - Displays the karma plugin commands help menu.
- `/karma reload` - Reloads the plugin configuration from disk.

### Permissions
- `furykarma.use`: Allows using basic commands (info, top, help). *Default: true*.
- `furykarma.give`: Allows rating other players. *Default: true*.
- `furykarma.bypass.cooldown`: Allows bypassing the vote cooldown. *Default: OP*.
- `furykarma.admin`: Allows reloading the plugin configuration. *Default: OP*.
