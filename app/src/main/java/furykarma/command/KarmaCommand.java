package furykarma.command;

import furykarma.FuryKarma;
import furykarma.hook.NLoginHook;
import furykarma.storage.DatabaseManager;
import furykarma.storage.DatabaseManager.KarmaLogEntry;
import furykarma.storage.DatabaseManager.LeaderboardEntry;
import furykarma.util.MessageUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class KarmaCommand implements BasicCommand {

    private final FuryKarma plugin;

    public KarmaCommand(FuryKarma plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();

        // 1. nLogin Authentication Check
        if (sender instanceof Player player) {
            if (Bukkit.getPluginManager().isPluginEnabled("nLogin") && !NLoginHook.isAuthenticated(player)) {
                sendMessage(sender, "messages.not-authenticated");
                return;
            }
        }

        // 2. Base Permission Check
        if (!sender.hasPermission("furykarma.use")) {
            sendMessage(sender, "messages.no-permission");
            return;
        }

        // 3. No arguments -> Send Help
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "positive", "negative" -> handleGiveKarma(sender, sub, args);
            case "info" -> handleInfo(sender, args);
            case "top" -> handleTop(sender);
            case "help" -> sendHelp(sender);
            case "reload" -> handleReload(sender);
            default -> sendHelp(sender);
        }
    }

    private void handleGiveKarma(CommandSender sender, String type, String[] args) {
        // Must be a player to give karma
        if (!(sender instanceof Player playerSender)) {
            sendMessage(sender, "messages.only-players");
            return;
        }

        // Check permission to give karma
        if (!playerSender.hasPermission("furykarma.give")) {
            sendMessage(playerSender, "messages.no-permission");
            return;
        }

        // Validate arguments: /karma <positive|negative> <player> <reason...>
        if (args.length < 2) {
            sendMessage(playerSender, "messages.missing-arguments");
            return;
        }
        if (args.length < 3) {
            sendMessage(playerSender, "messages.missing-reason");
            return;
        }

        String targetName = args[1];
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).trim();
        
        if (reason.isEmpty()) {
            sendMessage(playerSender, "messages.missing-reason");
            return;
        }

        // Check self-karma
        if (targetName.equalsIgnoreCase(playerSender.getName())) {
            sendMessage(playerSender, "messages.cannot-self-karma");
            return;
        }

        // Check Target Existence (cracked-friendly)
        OfflinePlayer targetPlayer = getOfflinePlayer(targetName);
        if (targetPlayer == null || (!targetPlayer.hasPlayedBefore() && !targetPlayer.isOnline())) {
            String msg = getMsg("messages.player-not-found").replace("%player%", targetName);
            playerSender.sendMessage(MessageUtils.format(msg));
            return;
        }

        UUID senderUuid = playerSender.getUniqueId();
        UUID targetUuid = targetPlayer.getUniqueId();
        String targetRealName = targetPlayer.getName() != null ? targetPlayer.getName() : targetName;

        // Check Cooldown
        if (!playerSender.hasPermission("furykarma.bypass.cooldown")) {
            long lastGiven = plugin.getDatabaseManager().getLastKarmaTime(senderUuid);
            if (lastGiven > 0) {
                long cooldownHours = plugin.getConfig().getLong("cooldown-hours", 24);
                long cooldownMs = cooldownHours * 60 * 60 * 1000;
                long elapsed = System.currentTimeMillis() - lastGiven;
                if (elapsed < cooldownMs) {
                    long remainingMs = cooldownMs - elapsed;
                    String timeStr = formatTimeDuration(remainingMs);
                    String msg = getMsg("messages.cooldown").replace("%time%", timeStr);
                    playerSender.sendMessage(MessageUtils.format(msg));
                    return;
                }
            }
        }

        // Log to database
        plugin.getDatabaseManager().logKarma(
                senderUuid,
                playerSender.getName(),
                targetUuid,
                targetRealName,
                type,
                reason,
                System.currentTimeMillis()
        );

        // Notify Sender
        String label = type.equalsIgnoreCase("positive") ? getMsg("messages.positive-label") : getMsg("messages.negative-label");
        String senderMsg = getMsg("messages.karma-given-sender")
                .replace("%type%", label)
                .replace("%player%", targetRealName)
                .replace("%reason%", reason);
        playerSender.sendMessage(MessageUtils.format(senderMsg));

        // Notify Target if online
        Player targetOnline = targetPlayer.getPlayer();
        if (targetOnline != null && targetOnline.isOnline()) {
            if (!Bukkit.getPluginManager().isPluginEnabled("nLogin") || NLoginHook.isAuthenticated(targetOnline)) {
                String targetMsg = getMsg("messages.karma-received-target")
                        .replace("%type%", label)
                        .replace("%player%", playerSender.getName())
                        .replace("%reason%", reason);
                targetOnline.sendMessage(MessageUtils.format(targetMsg));
            }
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        UUID targetUuid;
        String targetName;
        boolean self = false;

        if (args.length == 1) {
            // Self info
            if (!(sender instanceof Player player)) {
                sendMessage(sender, "messages.only-players");
                return;
            }
            targetUuid = player.getUniqueId();
            targetName = player.getName();
            self = true;
        } else {
            // Other player info
            String inputName = args[1];

            // 1. Try online players first
            Player online = Bukkit.getPlayer(inputName);
            if (online != null) {
                targetUuid = online.getUniqueId();
                targetName = online.getName();
            } else {
                // 2. Try Bukkit offline player cache (vanilla / premium servers)
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(inputName);
                if (offlinePlayer.hasPlayedBefore()) {
                    targetUuid = offlinePlayer.getUniqueId();
                    targetName = offlinePlayer.getName() != null ? offlinePlayer.getName() : inputName;
                } else {
                    // 3. Fallback: look up in the database by name (cracked / offline players)
                    String[] dbInfo = plugin.getDatabaseManager().getPlayerInfoByName(inputName);
                    if (dbInfo != null) {
                        targetUuid = UUID.fromString(dbInfo[0]);
                        targetName = dbInfo[1];
                    } else {
                        String msg = getMsg("messages.player-not-found").replace("%player%", inputName);
                        sender.sendMessage(MessageUtils.format(msg));
                        return;
                    }
                }
            }
        }

        // Fetch statistics from Database
        int pos = plugin.getDatabaseManager().getKarmaCount(targetUuid, "POSITIVE");
        int neg = plugin.getDatabaseManager().getKarmaCount(targetUuid, "NEGATIVE");
        int total = pos - neg; // Net Karma
        int sumTotal = pos + neg; // Sum of ratings

        String averageStr;
        if (sumTotal == 0) {
            averageStr = "100%"; // Or N/A, let's say 100% since no negative reviews
        } else {
            double avg = ((double) pos / sumTotal) * 100.0;
            averageStr = String.format("%.1f%%", avg);
        }

        int logsLimit = self ? 5 : 10;
        List<KarmaLogEntry> logs = plugin.getDatabaseManager().getRecentReceivedKarma(targetUuid, logsLimit);

        // Header
        sender.sendMessage(MessageUtils.format(getMsg("messages.info.header").replace("%player%", targetName)));
        
        // Stats
        sender.sendMessage(MessageUtils.format(getMsg("messages.info.total").replace("%total%", (total >= 0 ? "+" : "") + total)));
        sender.sendMessage(MessageUtils.format(getMsg("messages.info.average")
                .replace("%average%", averageStr)
                .replace("%positives%", String.valueOf(pos))
                .replace("%negatives%", String.valueOf(neg))
        ));

        // Logs
        if (logs.isEmpty()) {
            sender.sendMessage(MessageUtils.format(getMsg("messages.info.no-logs")));
        } else {
            sender.sendMessage(MessageUtils.format(getMsg("messages.info.logs-header")));
            for (KarmaLogEntry entry : logs) {
                String typeLabel = entry.getKarmaType().equalsIgnoreCase("POSITIVE") 
                        ? getMsg("messages.positive-label") 
                        : getMsg("messages.negative-label");
                String logMsg = getMsg("messages.info.log-entry")
                        .replace("%type%", typeLabel)
                        .replace("%sender%", entry.getSenderName())
                        .replace("%time%", formatTimeAgo(entry.getTimestamp()))
                        .replace("%reason%", entry.getReason());
                sender.sendMessage(MessageUtils.format(logMsg));
            }
        }

        // Footer
        sender.sendMessage(MessageUtils.format(getMsg("messages.info.footer")));
    }

    private void handleTop(CommandSender sender) {
        List<LeaderboardEntry> top = plugin.getDatabaseManager().getTopKarma(10);

        sender.sendMessage(MessageUtils.format(getMsg("messages.top.header")));

        if (top.isEmpty()) {
            sender.sendMessage(MessageUtils.format(getMsg("messages.top.no-data")));
        } else {
            for (int i = 0; i < top.size(); i++) {
                LeaderboardEntry entry = top.get(i);
                String entryMsg = getMsg("messages.top.entry")
                        .replace("%index%", String.valueOf(i + 1))
                        .replace("%player%", entry.getName())
                        .replace("%average%", String.format("%.1f%%", entry.getAverage()))
                        .replace("%net%", (entry.getNet() >= 0 ? "+" : "") + entry.getNet())
                        .replace("%positives%", String.valueOf(entry.getPositives()))
                        .replace("%negatives%", String.valueOf(entry.getNegatives()));
                sender.sendMessage(MessageUtils.format(entryMsg));
            }
        }

        sender.sendMessage(MessageUtils.format(getMsg("messages.top.footer")));
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("furykarma.admin")) {
            sendMessage(sender, "messages.no-permission");
            return;
        }

        plugin.reloadConfig();
        sendMessage(sender, "messages.config-reloaded");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.header")));
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.give")));
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.info-self")));
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.info-other")));
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.top")));
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.info-help")));
        sender.sendMessage(MessageUtils.format(getMsg("messages.help.footer")));
    }

    private void sendMessage(CommandSender sender, String configPath) {
        String msg = getMsg(configPath);
        if (!msg.isEmpty()) {
            sender.sendMessage(MessageUtils.format(msg));
        }
    }

    private String getMsg(String path) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String text = plugin.getConfig().getString(path, "");
        if (text.isEmpty()) {
            return "";
        }
        
        // Don't prepend prefix to header/footer/entry elements or specific labels
        if (path.startsWith("messages.help.") || path.startsWith("messages.info.") || path.startsWith("messages.top.") || path.endsWith("-label")) {
            return text;
        }
        return prefix + text;
    }

    private OfflinePlayer getOfflinePlayer(String name) {
        // Try online players first
        Player online = Bukkit.getPlayer(name);
        if (online != null) {
            return online;
        }
        
        // Otherwise, fetch offline player profile
        return Bukkit.getOfflinePlayer(name);
    }

    private String formatTimeDuration(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    private String formatTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 0) diff = 0;
        
        long seconds = diff / 1000;
        if (seconds < 60) {
            return "just now";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "m";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "h";
        }
        
        long days = hours / 24;
        return days + "d";
    }

    @Override
    public @NotNull Collection<String> suggest(@NotNull CommandSourceStack source, @NotNull String[] args) {
        CommandSender sender = source.getSender();
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("furykarma.use")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            List<String> subs = new ArrayList<>(List.of("positive", "negative", "info", "top", "help"));
            if (sender.hasPermission("furykarma.admin")) {
                subs.add("reload");
            }
            for (String sub : subs) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("positive") || sub.equals("negative") || sub.equals("info")) {
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Prevent suggesting self for positive/negative commands
                    if ((sub.equals("positive") || sub.equals("negative")) && player.getName().equalsIgnoreCase(sender.getName())) {
                        continue;
                    }
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("positive") || sub.equals("negative")) {
                completions.add("<reason>");
            }
        }

        return completions;
    }
}
