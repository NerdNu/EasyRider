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

    /**
     * Database implementation name: "sqlite", "yaml" or "sqlite+yaml"
     */
    public String DATABASE_IMPLEMENTATION;

    /**
     * Ratio of distance travelled in one tick to the current speed of a horse
     * for its level.
     *
     * This should be no less than 4.5 (determined empirically). If the server
     * is sufficiently laggy, it may need to increase.
     */
    public double SPEED_LIMIT;

    /**
     * Speed ability.
     */
    public Ability SPEED = new Ability("speed", "Speed", Attribute.GENERIC_MOVEMENT_SPEED) {

        @Override
        public double getDisplayValue(int level) {
            return getValue(level) * 43;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            return String.format("%.2f %s", getDisplayValue(getLevel(savedHorse)), getValueUnits());
        }

        @Override
        public void setLevel(SavedHorse savedHorse, int level) {
            savedHorse.setSpeedLevel(level);
        }

        @Override
        public int getLevel(SavedHorse savedHorse) {
            return savedHorse.getSpeedLevel();
        }

        @Override
        public void setEffort(SavedHorse savedHorse, double effort) {
            savedHorse.setDistanceTravelled(effort);
        }

        @Override
        public double getEffort(SavedHorse savedHorse) {
            return savedHorse.getDistanceTravelled();
        }

        @Override
        public String getFormattedEffort(SavedHorse savedHorse) {
            return String.format("%.2f%s", savedHorse.getDistanceTravelled(), getEffortUnits());
        }

        @Override
        public String getValueUnits() {
            return "m/s";
        }

        @Override
        public String getEffortUnits() {
            return "m travelled";
        }
    };

    /**
     * Jump Height ability.
     */
    public Ability JUMP = new Ability("jump", "Jump Strength", Attribute.HORSE_JUMP_STRENGTH) {
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
            return String.format("%.2f%s", getDisplayValue(getLevel(savedHorse)), getValueUnits());
        }

        @Override
        public void setLevel(SavedHorse savedHorse, int level) {
            savedHorse.setJumpLevel(level);
        }

        @Override
        public int getLevel(SavedHorse savedHorse) {
            return savedHorse.getJumpLevel();
        }

        @Override
        public void setEffort(SavedHorse savedHorse, double effort) {
            savedHorse.setDistanceJumped(effort);
        }

        @Override
        public double getEffort(SavedHorse savedHorse) {
            return savedHorse.getDistanceJumped();
        }

        @Override
        public String getFormattedEffort(SavedHorse savedHorse) {
            return String.format("%.2f%s", savedHorse.getDistanceJumped(), getEffortUnits());
        }

        @Override
        public String getValueUnits() {
            return "m";
        }

        @Override
        public String getEffortUnits() {
            return "m jumped";
        }
    };

    /**
     * Health ability.
     */
    public Ability HEALTH = new Ability("health", "Max Health", Attribute.GENERIC_MAX_HEALTH) {
        @Override
        public double getDisplayValue(int level) {
            return getValue(level) * 0.5;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            return String.format("%.2g%s", getDisplayValue(getLevel(savedHorse)), getValueUnits());
        }

        @Override
        public void setLevel(SavedHorse savedHorse, int level) {
            savedHorse.setHealthLevel(level);
        }

        @Override
        public int getLevel(SavedHorse savedHorse) {
            return savedHorse.getHealthLevel();
        }

        @Override
        public void setEffort(SavedHorse savedHorse, double effort) {
            savedHorse.setNuggetsEaten((int) effort);
        }

        @Override
        public double getEffort(SavedHorse savedHorse) {
            return savedHorse.getNuggetsEaten();
        }

        @Override
        public String getFormattedEffort(SavedHorse savedHorse) {
            return String.format("%d %s", savedHorse.getNuggetsEaten(), getEffortUnits());
        }

        @Override
        public String getValueUnits() {
            return "♥";
        }

        @Override
        public String getEffortUnits() {
            return "gold nuggets eaten";
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

        DATABASE_IMPLEMENTATION = config.getString("database.implementation");
        SPEED_LIMIT = config.getDouble("speed-limit");

        SPEED.load(config.getConfigurationSection("abilities.speed"), logger);
        JUMP.load(config.getConfigurationSection("abilities.jump"), logger);
        HEALTH.load(config.getConfigurationSection("abilities.health"), logger);

        if (DEBUG_CONFIG) {
            logger.info("Configuration:");
            logger.info("DEBUG_EVENTS: " + DEBUG_EVENTS);
            logger.info("DEBUG_SAVES: " + DEBUG_SAVES);
            logger.info("DATABASE_IMPLEMENTATION: " + DATABASE_IMPLEMENTATION);
            logger.info("SPEED_LIMIT: " + SPEED_LIMIT);

            logAbility(logger, SPEED);
            logAbility(logger, JUMP);
            logAbility(logger, HEALTH);
        }
    } // reload

    // ------------------------------------------------------------------------
    /**
     * Save updated configuration.
     */
    public void save() {
        FileConfiguration config = EasyRider.PLUGIN.getConfig();
        config.set("database.implementation", DATABASE_IMPLEMENTATION);
        EasyRider.PLUGIN.saveConfig();
    }

    // ------------------------------------------------------------------------
    /**
     * Return ability with the specified name.
     * 
     * @param name the programmatic name of the ability.
     * @return the corresponding ability.
     */
    public Ability getAbility(String name) {
        switch (name) {
        case "health":
            return HEALTH;
        case "jump":
            return JUMP;
        case "speed":
            return SPEED;
        default:
            return null;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Describe the configuration of an Ability in the server logs, to aid
     * debugging.
     */
    protected void logAbility(Logger logger, Ability ability) {
        logger.info("----- " + ability.getName() + " (" + ability.getDisplayName() + ") -----");
        logger.info("Max Level: " + ability.getMaxLevel() +
                    " Max Effort: " + ability.getMaxEffort());
        logger.info("Effort Scale: " + ability.getEffortScale() +
                    " Effort Base: " + ability.getEffortBase());
        logger.info("Min Value: " + ability.getMinValue() +
                    " Max Value: " + ability.getMaxValue());
    }
} // class Configuration