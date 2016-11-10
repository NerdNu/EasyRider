package nu.nerd.easyrider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import nu.nerd.easyrider.commands.EasyRiderExecutor;
import nu.nerd.easyrider.commands.ExecutorBase;
import nu.nerd.easyrider.commands.HorseDebugExecutor;
import nu.nerd.easyrider.commands.HorseLevelsExecutor;
import nu.nerd.easyrider.commands.HorseSetLevelExecutor;
import nu.nerd.easyrider.commands.HorseTopExecutor;
import nu.nerd.easyrider.commands.HorseUpgradesExecutor;
import nu.nerd.easyrider.db.HorseDB;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Plugin, command handling and event handler class.
 */
public class EasyRider extends JavaPlugin implements Listener {
    // ------------------------------------------------------------------------
    /**
     * Configuration wrapper instance.
     */
    public static Configuration CONFIG = new Configuration();

    /**
     * This plugin, accessible as, effectively, a singleton.
     */
    public static EasyRider PLUGIN;

    /**
     * Horse database and cache.
     */
    public static HorseDB DB = new HorseDB();

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        CONFIG.reload();

        File playersFile = new File(getDataFolder(), PLAYERS_FILE);
        _playerConfig = YamlConfiguration.loadConfiguration(playersFile);

        getServer().getPluginManager().registerEvents(this, this);

        addCommandExecutor(new EasyRiderExecutor());
        addCommandExecutor(new HorseDebugExecutor());
        addCommandExecutor(new HorseSetLevelExecutor());
        addCommandExecutor(new HorseLevelsExecutor());
        addCommandExecutor(new HorseUpgradesExecutor());
        addCommandExecutor(new HorseTopExecutor());

        DB.backup();
        DB.load();
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        for (PlayerState state : _state.values()) {
            state.save(_playerConfig);
        }

        try {
            _playerConfig.save(new File(getDataFolder(), PLAYERS_FILE));
        } catch (IOException ex) {
            getLogger().warning("Unable to save player data: " + ex.getMessage());
        }

        DB.save();
    }

    // ------------------------------------------------------------------------
    /**
     * On join, allocate each player a {@link PlayerState} instance.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        _state.put(player.getName(), new PlayerState(player, _playerConfig));
    }

    // ------------------------------------------------------------------------
    /**
     * On quit, forget the {@link PlayerState}.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        PlayerState state = _state.remove(event.getPlayer().getName());
        state.save(_playerConfig);
    }

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking on horses.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity.getType() == EntityType.HORSE) {
            Horse horse = (Horse) entity;
            Player player = event.getPlayer();

            if (horse.getVariant() == Variant.SKELETON_HORSE || horse.getVariant() == Variant.UNDEAD_HORSE) {
                // If not owned, undead horses belong to whoever clicks first.
                if (horse.getOwner() == null) {
                    horse.setTamed(true);
                    horse.setOwner(event.getPlayer());
                    if (CONFIG.DEBUG_EVENTS) {
                        debug(horse, horse.getVariant() + " owner set to " + player.getName());
                    }
                }
            }

            SavedHorse savedHorse = DB.findHorse(horse);
            if (savedHorse == null) {
                savedHorse = DB.addHorse(horse);

                CONFIG.SPEED.setLevel(savedHorse, 1);
                CONFIG.SPEED.setEffort(savedHorse, 0);
                CONFIG.SPEED.updateAttributes(savedHorse, horse);
                CONFIG.JUMP.setLevel(savedHorse, 1);
                CONFIG.JUMP.setEffort(savedHorse, 0);
                CONFIG.JUMP.updateAttributes(savedHorse, horse);
                CONFIG.HEALTH.setLevel(savedHorse, 1);
                CONFIG.HEALTH.setEffort(savedHorse, 0);
                CONFIG.HEALTH.updateAttributes(savedHorse, horse);

                if (CONFIG.DEBUG_EVENTS) {
                    debug(horse, horse.getVariant() + " initialised to level 1 by " + player.getName());
                }
            } else {
                if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                    debug(horse, " Level S" + savedHorse.getSpeedLevel() +
                                 "/J" + savedHorse.getJumpLevel() +
                                 "/H" + savedHorse.getHealthLevel() +
                                 " clicked by " + player.getName());
                }
            }

            PlayerState playerState = getState(player);
            Location horseLoc = horse.getLocation();
            if (playerState.hasPendingInteraction()) {
                playerState.handlePendingInteraction(event, savedHorse);

                // Even though the event is cancelled, the player will end up
                // facing the same direction as the horse, so make the horse
                // face where the player should face.
                horseLoc.setYaw(player.getLocation().getYaw());
                horse.teleport(horseLoc);
                event.setCancelled(true);
            } else {
                // Handle health training. only if the event was not cancelled.
                ItemStack foodItem = player.getEquipment().getItemInMainHand();
                int nuggets = getNuggetValue(foodItem);
                if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                    getLogger().info("Nugget value: " + nuggets);
                }
                if (nuggets > 0) {
                    CONFIG.HEALTH.setEffort(savedHorse, CONFIG.HEALTH.getEffort(savedHorse) + nuggets);
                    if (CONFIG.HEALTH.hasLevelIncreased(savedHorse, horse)) {
                        notifyLevelUp(player, savedHorse, horse, CONFIG.HEALTH);
                    }

                    // Undead and skeletal horse types don't normally eat.
                    // Simulate eating.
                    // Golden carrots are apparently always edible. Apples are
                    // usually not. Force apples to be taken. Untamed horses
                    // also seem to eat golden apples without limit.
                    // Location horseLoc = horse.getLocation();
                    // boolean takeItem = horse.getVariant() ==
                    // Variant.SKELETON_HORSE ||
                    // horse.getVariant() == Variant.UNDEAD_HORSE ||
                    // !horse.isTamed() ||
                    // nuggets > 8;
                    // if (takeItem) {

                    horseLoc.getWorld().playSound(horseLoc, Sound.ENTITY_HORSE_EAT, 3.0f, 1.0f);
                    // Eat the whole stack, since we don't get the event
                    // to see whether the horse eats an item or not.
                    foodItem.setAmount(0);
                    player.getEquipment().setItemInMainHand(foodItem);
                    // }
                }
            }

            // Update stored owner, which may have changed.
            savedHorse.setOwner(horse.getOwner());
        }
    } // onPlayerInteractEntity

    // ------------------------------------------------------------------------
    /**
     * When a horse moves, train up speed if it is on the ground, and jump if it
     * is moving horizontally through the air.
     *
     * Horses *are* Vehicles, but they don't fire a VehicleMoveEvent. Detect
     * player movement when riding a horse using PlayerMoveEvent.
     *
     * Horses swimming in liquid are not counted as "on the ground", but it is
     * not an exploitable way of levelling up jump because the player is ejected
     * as soon as the horse sinks.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Horse)) {
            return;
        }

        Horse horse = (Horse) vehicle;
        if (horse.isInsideVehicle()) {
            // The horse cannot be trained by moving it around in a vehicle.
            return;
        }

        SavedHorse savedHorse = DB.findHorse(horse);
        if (savedHorse == null) {
            getLogger().warning("onVehicleMove(): Missing database entry for horse " + horse.getUniqueId());
            savedHorse = DB.addHorse(horse);
        }

        // Update stored owner, which may have changed.
        savedHorse.setOwner(horse.getOwner());

        // Compute distance moved and update speed or jump depending on whether
        // the horse was on the ground.
        PlayerState playerState = getState(player);
        double tickDistance = playerState.getTickHorizontalDistance();
        if (tickDistance > 0) {
            // Sanity check: if the distance is so large as to be unattainable
            // in one tick, then don't apply the distance to the horse and log
            // in console. Ratio to max speed determined empirically.
            double maxSpeed = CONFIG.SPEED.getValue(savedHorse.getSpeedLevel() + 1);
            if (tickDistance > CONFIG.SPEED_LIMIT * maxSpeed) {
                getLogger().warning(horse.getOwner().getName() + "'s horse " + horse.getUniqueId() +
                                    " moved impossibly fast for its level; ratio: " + (tickDistance / maxSpeed));
                player.sendMessage(ChatColor.RED + "It might move faster with a jetpack, but that doesn't mean the horse is getting stronger!");
            } else {
                Ability ability = (horse.isOnGround()) ? CONFIG.SPEED : CONFIG.JUMP;
                ability.setEffort(savedHorse, ability.getEffort(savedHorse) + tickDistance);
                if (ability.hasLevelIncreased(savedHorse, horse)) {
                    notifyLevelUp(player, savedHorse, horse, ability);
                }
            }
        }

        // Update stored location to compute distance in the next tick.
        playerState.updateRiddenHorse();

        if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
            debug(horse, "supported: " + horse.isOnGround());
        }
    } // onPlayerMove

    // ------------------------------------------------------------------------
    /**
     * When a player mounts a horse, clear the recorded location of the horse in
     * the previous tick.
     *
     * Note that players can switch from horse to horse without dismounting,
     * which would mess up distance ridden calculations if we simply stored the
     * destination horse's location.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof Horse)) {
            return;
        }
        Horse horse = (Horse) event.getVehicle();
        Entity passenger = event.getEntered();
        if (passenger instanceof Player) {
            Player player = (Player) passenger;
            SavedHorse savedHorse = DB.findHorse(horse);
            if (savedHorse == null) {
                getLogger().warning("onVehicleMove(): Missing database entry for horse " + horse.getUniqueId());
                savedHorse = DB.addHorse(horse);
            }

            // Update stored owner, which may have changed.
            savedHorse.setOwner(horse.getOwner());

            getState(player).clearHorseDistance();
            if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                debug(horse, "passenger: " + player.getName());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If a horse teleports and takes a rider with it (is that possible?) clear
     * any distance travelled.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Horse) {
            Horse horse = (Horse) event.getEntity();
            if (horse.getPassenger() instanceof Player) {
                Player player = (Player) horse.getPassenger();
                getState(player).clearHorseDistance();
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Send a debug message to all players with the debug permissions.
     * 
     * @param message the message.
     */
    public void debug(Horse horse, String message) {
        debug(horse.getUniqueId() + ": " + message);
    }

    // ------------------------------------------------------------------------
    /**
     * Send a debug message to all players with the debug permissions.
     * 
     * @param message the message.
     */
    public void debug(String message) {
        Bukkit.broadcast(ChatColor.YELLOW + "[EasyRider] " + message, "easyrider.debug");
    }

    // ------------------------------------------------------------------------
    /**
     * Return the {@link PlayerState} for the specified player.
     *
     * @param player the player.
     * @return the {@link PlayerState} for the specified player.
     */
    public PlayerState getState(Player player) {
        return _state.get(player.getName());
    }

    // ------------------------------------------------------------------------
    /**
     * Register classes that will be stored by Ebeans ORM.
     * 
     * @return the list of classes mapped to database rows.
     */
    @Override
    public ArrayList<Class<?>> getDatabaseClasses() {
        ArrayList<Class<?>> list = new ArrayList<Class<?>>();
        list.add(SavedHorse.class);
        return list;
    }

    // ------------------------------------------------------------------------
    /**
     * Expose database initialisation method.
     */
    @Override
    public void installDDL() {
        super.installDDL();
    }

    // ------------------------------------------------------------------------
    /**
     * Notify the player that the specified horse has changed its level, and
     * play the corresponding sound and particle effects.
     *
     * @param player the player.
     * @param savedHorse the database state of the horse.
     * @param horse the Horse entity.
     * @param ability the affected ability.
     */
    protected void notifyLevelUp(Player player, SavedHorse savedHorse, Horse horse, Ability ability) {
        player.sendMessage(ChatColor.GOLD + "This horse is now Level " + ability.getLevel(savedHorse) +
                           " in " + ability.getDisplayName() + ".");
        Location loc = horse.getLocation().add(0, 1, 0);
        loc.getWorld().spigot().playEffect(loc, Effect.HAPPY_VILLAGER, 0, 0, 2.0f, 1.0f, 2.0f, 0.0f, 100, 32);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 3.0f, 1.0f);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the gold mass of a stack of food items in gold nuggets.
     *
     * @param food an ItemStack containing the food; only one item is counted,
     *        if there are multiple items in the stack.
     * @return the gold mass of a food item in gold nuggets.
     */
    protected int getNuggetValue(ItemStack food) {
        if (food == null) {
            return 0;
        } else if (food.getType() == Material.GOLDEN_CARROT) {
            return food.getAmount() * 8;
        } else if (food.getType() == Material.GOLDEN_APPLE) {
            return food.getAmount() * ((food.getDurability() == 0) ? 8 * 9 : 8 * 9 * 9);
        } else {
            return 0;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Add the specified CommandExecutor and set it as its own TabCompleter.
     * 
     * @param executor the CommandExecutor.
     */
    protected void addCommandExecutor(ExecutorBase executor) {
        PluginCommand command = getCommand(executor.getName());
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    // ------------------------------------------------------------------------
    /**
     * Name of players file.
     */
    private static final String PLAYERS_FILE = "players.yml";

    /**
     * Configuration file for per-player settings.
     */
    protected YamlConfiguration _playerConfig;

    /**
     * Map from Player name to {@link PlayerState} instance.
     *
     * A Player's PlayerState exists only for the duration of a login.
     */
    protected HashMap<String, PlayerState> _state = new HashMap<String, PlayerState>();

} // class EasyRider