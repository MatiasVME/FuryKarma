package furykarma;

import furykarma.command.KarmaCommand;
import furykarma.storage.DatabaseManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class FuryKarma extends JavaPlugin {

    private DatabaseManager databaseManager;

    @Override
    public void onEnable() {
        // Save default config.yml if it does not exist
        saveDefaultConfig();

        // Initialize SQLite Database Manager
        databaseManager = new DatabaseManager(this);
        databaseManager.initialize();

        // Register commands dynamically using Paper's LifecycleEventManager
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                "karma",
                "FuryKarma main command.",
                List.of("furykarma"),
                new KarmaCommand(this)
            );
        });

        getLogger().info("FuryKarma has been successfully enabled.");
    }

    @Override
    public void onDisable() {
        // Close SQLite Connection
        if (databaseManager != null) {
            databaseManager.close();
        }

        getLogger().info("FuryKarma has been successfully disabled.");
    }

    /**
     * Gets the database manager instance.
     *
     * @return The database manager.
     */
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}
