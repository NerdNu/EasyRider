package nu.nerd.easyrider;

import java.util.HashMap;
import java.util.logging.Logger;

import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
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
     * If true, log the time taken to scan worlds for horses.
     */
    public boolean DEBUG_SCANS;

    /**
     * If true, log the time taken to find horses.
     */
    public boolean DEBUG_FINDS;

    /**
     * Database implementation name: "sqlite", "yaml" or "sqlite+yaml"
     */
    public String DATABASE_IMPLEMENTATION;

    /**
     * If true, eject the rider from the horse when he logs off.
     */
    public boolean EJECT_ON_LOGOFF;

    /**
     * If true, allow players to harm owned horses that are being ridden by a
     * player.
     */
    public boolean ALLOW_PVP;

    /**
     * Ratio of distance travelled in one tick to the current speed of a horse
     * for its level.
     *
     * This should be no less than 4.5 (determined empirically). If the server
     * is sufficiently laggy, it may need to increase.
     */
    public double SPEED_LIMIT;

    /**
     * Distance a horse must be ridden to reduce hydration from full (1.0) to
     * none (0.0).
     */
    public double DEHYDRATION_DISTANCE;

    /**
     * Amount of hydration added by one water bucket.
     */
    public double BUCKET_HYDRATION;

    /**
     * Number of consecutive days an untrained horse must not be accessed by its
     * owner for it to be abandoned.
     */
    public int ABANDONED_DAYS;

    /**
     * Period in seconds between horse search task runs.
     */
    public int SCAN_PERIOD_SECONDS;

    /**
     * Maximum duration of the horse search task in a single tick, expressed in
     * microseconds.
     */
    public int SCAN_TIME_LIMIT_MICROS;

    /**
     * Map from world name to WorldBorder radius. (Assumed square.)
     */
    public HashMap<String, Integer> SCAN_WORLD_RADIUS = new HashMap<String, Integer>();

    /**
     * Speed ability.
     */
    public Ability SPEED = new Ability("speed", "Speed", Attribute.GENERIC_MOVEMENT_SPEED) {
        @Override
        public double toDisplayValue(double value) {
            return value * 43;
        }

        @Override
        public double toAttributeValue(double displayValue) {
            return displayValue / 43;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            double displayValue = toDisplayValue(getValue(getLevel(savedHorse)));
            return String.format("%.2f %s", displayValue, getValueUnits());
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
         * Compute the jump height using the same algorithm as Zyin's HUD and
         * CobraCorral.
         */
        @Override
        public double toDisplayValue(double value) {
            double yVelocity = value;
            double jumpHeight = 0;
            while (yVelocity > 0) {
                jumpHeight += yVelocity;
                yVelocity -= 0.08;
                yVelocity *= 0.98;
            }
            return jumpHeight;
        }

        @Override
        public double toAttributeValue(double displayValue) {
            // Reversing the jump physics is a waste of time if never used.
            throw new IllegalStateException("not implemented");
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            double displayValue = toDisplayValue(getValue(getLevel(savedHorse)));
            return String.format("%.2f%s", displayValue, getValueUnits());
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
        public double toDisplayValue(double value) {
            return value * 0.5;
        }

        @Override
        public double toAttributeValue(double displayValue) {
            return displayValue * 2.0;
        }

        @Override
        public String getFormattedValue(SavedHorse savedHorse) {
            double displayValue = toDisplayValue(getValue(getLevel(savedHorse)));
            return String.format("%.2g%s", displayValue, getValueUnits());
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
        DEBUG_SCANS = config.getBoolean("debug.scans");
        DEBUG_FINDS = config.getBoolean("debug.finds");

        DATABASE_IMPLEMENTATION = config.getString("database.implementation");
        EJECT_ON_LOGOFF = config.getBoolean("eject-on-logoff");
        ALLOW_PVP = config.getBoolean("allow-pvp");
        SPEED_LIMIT = config.getDouble("speed-limit");
        DEHYDRATION_DISTANCE = config.getDouble("dehydration-distance");
        BUCKET_HYDRATION = config.getDouble("bucket-hydration");
        ABANDONED_DAYS = config.getInt("abandoned-days");

        SCAN_PERIOD_SECONDS = config.getInt("scan.period-seconds");
        SCAN_TIME_LIMIT_MICROS = config.getInt("scan.time-limit-micros");
        SCAN_WORLD_RADIUS.clear();
        ConfigurationSection worlds = config.getConfigurationSection("scan.worlds");
        for (String worldName : worlds.getKeys(false)) {
            SCAN_WORLD_RADIUS.put(worldName, worlds.getInt(worldName));
        }

        SPEED.load(config.getConfigurationSection("abilities.speed"), logger);
        JUMP.load(config.getConfigurationSection("abilities.jump"), logger);
        HEALTH.load(config.getConfigurationSection("abilities.health"), logger);

        if (DEBUG_CONFIG) {
            logger.info("Configuration:");
            logger.info("DEBUG_EVENTS: " + DEBUG_EVENTS);
            logger.info("DEBUG_SAVES: " + DEBUG_SAVES);
            logger.info("DEBUG_SCANS: " + DEBUG_SCANS);
            logger.info("DEBUG_FINDS: " + DEBUG_FINDS);
            logger.info("DATABASE_IMPLEMENTATION: " + DATABASE_IMPLEMENTATION);
            logger.info("EJECT_ON_LOGOFF: " + EJECT_ON_LOGOFF);
            logger.info("ALLOW_PVP: " + ALLOW_PVP);
            logger.info("SPEED_LIMIT: " + SPEED_LIMIT);
            logger.info("DEHYDRATION_DISTANCE: " + DEHYDRATION_DISTANCE);
            logger.info("BUCKET_HYDRATION: " + BUCKET_HYDRATION);
            logger.info("ABANDONED_DAYS: " + ABANDONED_DAYS);
            logger.info("SCAN_PERIOD_SECONDS: " + SCAN_PERIOD_SECONDS);
            logger.info("SCAN_TIME_LIMIT_MICROS: " + SCAN_TIME_LIMIT_MICROS);
            logger.info("Scanned worlds border radius: ");
            for (String worldName : SCAN_WORLD_RADIUS.keySet()) {
                logger.info(worldName + " = " + SCAN_WORLD_RADIUS.get(worldName));
            }

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