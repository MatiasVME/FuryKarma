package furykarma.hook;

import com.nickuc.login.api.nLoginAPI;
import org.bukkit.entity.Player;

/**
 * Hook to interact with the nLogin API.
 * This class should only be loaded/accessed if nLogin is present and enabled on the server.
 */
public final class NLoginHook {

    private NLoginHook() {
    }

    /**
     * Checks if the player is currently authenticated/logged in via nLogin.
     *
     * @param player The player to check.
     * @return True if authenticated, false otherwise.
     */
    public static boolean isAuthenticated(Player player) {
        try {
            return nLoginAPI.getApi().isAuthenticated(player.getName());
        } catch (Throwable t) {
            // Default to true in case of API failure, so we don't block players from playing
            return true;
        }
    }
}
