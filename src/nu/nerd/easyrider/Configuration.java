package nu.nerd.easyrider;

import java.util.logging.Logger;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Reads and exposes the plugin configuration.
 */
public class Configuration {
    /**
     * If true, log configuration settings.
     */
    public boolean DEBUG_CONFIG;

    /**
     * If true, show debug logs of events.
     */
    public boolean DEBUG_EVENTS;

    /**
     * If true, log the time taken to save the database.
     */
    public boolean DEBUG_SAVES;

    public Ability SPEED = new Ability("speed", "Speed", Attribute.GENERIC_MOVEMENT_SPEED) {

        @Override
        public double getDisplayValue(int level) {
            return getValue(level) * 43;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            return String.format("%.3g m/s", getDisplayValue(getLevel(savedHorse)));
        }

        @Override
        public void setLevel(SavedHorse savedHorse, int level) {
            savedHorse.setSpeedLevel(level);
            savedHorse.setDistanceTravelled(getEffortForLevel(level));
        }

        @Override
        public int getLevel(SavedHorse savedHorse) {
            return savedHorse.getSpeedLevel();
        }

        @Override
        public String getFormattedEffort(SavedHorse savedHorse) {
            return String.format("%.3g m travelled", savedHorse.getDistanceTravelled());
        }
    };

    public Ability JUMP = new Ability("jump", "Jump Height", Attribute.HORSE_JUMP_STRENGTH) {
        /**
         * Display the jump height using the same algorithm as Zyin's HUD and
         * CobraCorral.
         */
        @Override
        public double getDisplayValue(int level) {
            double yVelocity = getValue(level);
            double jumpHeight = 0;
            while (yVelocity > 0) {
                jumpHeight += yVelocity;
                yVelocity -= 0.08;
                yVelocity *= 0.98;
            }
            return jumpHeight;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            return String.format("%.3g m", getDisplayValue(getLevel(savedHorse)));
        }

        @Override
        public void setLevel(SavedHorse savedHorse, int level) {
            savedHorse.setJumpLevel(level);
            savedHorse.setDistanceJumped(getEffortForLevel(level));
        }

        @Override
        public int getLevel(SavedHorse savedHorse) {
            return savedHorse.getJumpLevel();
        }

        @Override
        public String getFormattedEffort(SavedHorse savedHorse) {
            return String.format("%.3g m jumped", savedHorse.getDistanceJumped());
        }
    };

    public Ability HEALTH = new Ability("health", "Max Health", Attribute.GENERIC_MAX_HEALTH) {
        @Override
        public double getDisplayValue(int level) {
            return getValue(level) * 0.5;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            return String.format("%.2g ♥", getDisplayValue(getLevel(savedHorse)));
        }

        @Override
        public void setLevel(SavedHorse savedHorse, int level) {
            savedHorse.setHealthLevel(level);
            savedHorse.setNuggetsEaten((int) getEffortForLevel(level));
        }

        @Override
        public int getLevel(SavedHorse savedHorse) {
            return savedHorse.getHealthLevel();
        }

        @Override
        public String getFormattedEffort(SavedHorse savedHorse) {
            return String.format("%d gold nuggets eaten", savedHorse.getNuggetsEaten());
        }
    };

    // ------------------------------------------------------------------------
    /**
     * Load the plugin configuration.
     */
    public void reload() {
        EasyRider.PLUGIN.reloadConfig();
        FileConfiguration config = EasyRider.PLUGIN.getConfig();
        Logger logger = EasyRider.PLUGIN.getLogger();

        DEBUG_CONFIG = config.getBoolean("debug.config");
        DEBUG_EVENTS = config.getBoolean("debug.events");
        DEBUG_SAVES = config.getBoolean("debug.saves");

        SPEED.load(config.getConfigurationSection("abilities.speed"), logger);
        JUMP.load(config.getConfigurationSection("abilities.jump"), logger);
        HEALTH.load(config.getConfigurationSection("abilities.health"), logger);

        if (DEBUG_CONFIG) {
            // TODO: log config.

        }
    } // reload

    // ------------------------------------------------------------------------
} // class Configuration