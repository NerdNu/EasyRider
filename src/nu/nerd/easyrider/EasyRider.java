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
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.HorseJumpEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import nu.nerd.easyrider.commands.EasyRiderExecutor;
import nu.nerd.easyrider.commands.ExecutorBase;
import nu.nerd.easyrider.commands.HorseAccessExecutor;
import nu.nerd.easyrider.commands.HorseBypassExecutor;
import nu.nerd.easyrider.commands.HorseDebugExecutor;
import nu.nerd.easyrider.commands.HorseFreeExecutor;
import nu.nerd.easyrider.commands.HorseGPSExecutor;
import nu.nerd.easyrider.commands.HorseLevelsExecutor;
import nu.nerd.easyrider.commands.HorseOwnedExecutor;
import nu.nerd.easyrider.commands.HorseSetLevelExecutor;
import nu.nerd.easyrider.commands.HorseSpeedLimitExecutor;
import nu.nerd.easyrider.commands.HorseSwapExecutor;
import nu.nerd.easyrider.commands.HorseTPExecutor;
import nu.nerd.easyrider.commands.HorseTPHereExecutor;
import nu.nerd.easyrider.commands.HorseTameExecutor;
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
    public static HorseDB DB;

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {
        PLUGIN = this;

        saveDefaultConfig();
        CONFIG.reload();

        DB = new HorseDB(CONFIG.DATABASE_IMPLEMENTATION);
        DB.backup();
        DB.load();

        File playersFile = new File(getDataFolder(), PLAYERS_FILE);
        _playerConfig = YamlConfiguration.loadConfiguration(playersFile);

        addCommandExecutor(new EasyRiderExecutor());
        addCommandExecutor(new HorseDebugExecutor());
        addCommandExecutor(new HorseSetLevelExecutor());
        addCommandExecutor(new HorseSwapExecutor());
        addCommandExecutor(new HorseTPExecutor());
        addCommandExecutor(new HorseTPHereExecutor());
        addCommandExecutor(new HorseBypassExecutor());
        addCommandExecutor(new HorseTameExecutor());
        addCommandExecutor(new HorseFreeExecutor());
        addCommandExecutor(new HorseLevelsExecutor());
        addCommandExecutor(new HorseUpgradesExecutor());
        addCommandExecutor(new HorseTopExecutor());
        addCommandExecutor(new HorseSpeedLimitExecutor());
        addCommandExecutor(new HorseGPSExecutor());
        addCommandExecutor(new HorseAccessExecutor());
        addCommandExecutor(new HorseOwnedExecutor());

        getServer().getPluginManager().registerEvents(this, this);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                ++_tickCounter;
            }
        }, 1, 1);

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
            SynchronousTimeLimitedTask _scanTask = new SynchronousTimeLimitedTask();

            @Override
            public void run() {
                if (_scanTask.isFinished()) {
                    if (CONFIG.DEBUG_SCANS) {
                        getLogger().info("Commencing new scan of loaded chunks.");
                    }
                    for (String worldName : CONFIG.SCAN_WORLD_RADIUS.keySet()) {
                        World world = Bukkit.getWorld(worldName);
                        if (world == null) {
                            getLogger().warning("Configured world " + worldName + " does not exist to be scanned.");
                        } else {
                            _scanTask.addStep(new ScanLoadedChunksTask(world));
                        }
                    }
                    Bukkit.getScheduler().scheduleSyncDelayedTask(EasyRider.PLUGIN, _scanTask);
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(EasyRider.PLUGIN, this, 20 * CONFIG.SCAN_PERIOD_SECONDS);
            }

        }, 20 * CONFIG.SCAN_PERIOD_SECONDS);
    } // onEnable

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
        DB.purgeAllRemovedHorses();
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
     *
     * If configured, automatically eject the player from its horse.
     */
    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (CONFIG.EJECT_ON_LOGOFF && player.getVehicle() instanceof Horse) {
            ((Horse) player.getVehicle()).eject();
        }

        PlayerState state = _state.remove(player.getName());
        state.save(_playerConfig);
    }

    // ------------------------------------------------------------------------
    /**
     * When a horse spawns, set its stats to defaults.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            CONFIG.SPEED.setAttribute(horse, 1);
            CONFIG.JUMP.setAttribute(horse, 1);
            CONFIG.HEALTH.setAttribute(horse, 1);
            if (CONFIG.DEBUG_EVENTS) {
                debug(horse, " spawned, reason: " + event.getSpawnReason());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Log taming of horses, play the lock sound and message the owner when they
     * tame (lock) a horse.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        Entity entity = event.getEntity();
        Player owner = (Player) event.getOwner();
        if (entity instanceof Horse && owner != null) {
            owner.playSound(owner.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, 1f, 1f);
            owner.sendMessage(ChatColor.GOLD + "This horse has been locked.");
            getLogger().info(owner.getName() + " tamed " + ((Horse) entity).getVariant().toString() +
                             entity.getUniqueId().toString());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Protect owned horses from damage when they don't have a player riding
     * them.
     *
     * Check for abandoned horses and remove the database entry. Riderless
     * horses taking damage from the void are teleported to their owner's bed
     * spawn location, or the world's spawn if a bed spawn is not set.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            if (horse.getOwner() != null && !(horse.getPassenger() instanceof Player)) {
                SavedHorse savedHorse = DB.findOrAddHorse(horse);
                DB.observe(savedHorse, horse);
                if (savedHorse.isAbandoned()) {
                    horse.setOwner(null);
                    DB.removeHorse(savedHorse);
                } else {
                    event.setCancelled(true);
                    if (event.getCause() == DamageCause.VOID) {
                        OfflinePlayer owner = (OfflinePlayer) horse.getOwner();
                        Location safeLocation = owner.getBedSpawnLocation();
                        if (safeLocation == null) {
                            safeLocation = horse.getWorld().getSpawnLocation();
                        }
                        if (safeLocation != null) {
                            horse.teleport(safeLocation);
                            savedHorse.setLocation(safeLocation);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If PvP is allowed, when an owned horse is being ridden it is vulnerable
     * to attacks by players, including both direct attacks and projectiles.
     *
     * That includes inadvertent attacks by the owner or rider of the horse.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Horse && !CONFIG.ALLOW_PVP) {
            Horse horse = (Horse) event.getEntity();
            if (horse.getOwner() != null && horse.getPassenger() instanceof Player) {
                Entity damager = event.getDamager();
                if (damager instanceof Player) {
                    event.setCancelled(true);
                } else if (damager instanceof Projectile) {
                    if (((Projectile) damager).getShooter() instanceof Player) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * When a trained horse dies, remove it from the database and log in the
     * console.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof Horse) {
            Horse horse = (Horse) entity;
            SavedHorse savedHorse = DB.findHorse(horse);
            if (savedHorse != null) {
                DB.removeHorse(savedHorse);

                String passenger = "";
                if (horse.getPassenger() != null) {
                    passenger = horse.getPassenger() instanceof Player ? ((Player) horse.getPassenger()).getName()
                                                                       : horse.getPassenger().toString();
                }
                String deathCause = (horse.getKiller() == null ? "the environment" : horse.getKiller().getName());

                StringBuilder message = new StringBuilder();
                message.append("Horse died ");
                message.append(horse.getUniqueId().toString());
                message.append(": ");

                AnimalTamer owner = horse.getOwner();
                if (owner instanceof Player) {
                    message.append("Owner: ").append(owner.getName());

                    // Tell the owner if someone else was riding.
                    if (!owner.equals(horse.getPassenger())) {
                        StringBuilder horseDescription = new StringBuilder();
                        horseDescription.append(horse.getCustomName() != null ? horse.getCustomName() : horse.getVariant().toString());
                        horseDescription.append(" (");
                        horseDescription.append(Util.limitString(horse.getUniqueId().toString(), 12));
                        horseDescription.append(") ");
                        ((Player) owner).sendMessage(ChatColor.RED + horseDescription.toString() + " has died due to " + deathCause +
                                                     (passenger.isEmpty() ? "." : " while being ridden by " + passenger + "."));
                    }
                } else {
                    message.append("No owner");
                }

                message.append(". Appearance: ").append(savedHorse.getAppearance());
                message.append(". Speed: ").append(CONFIG.SPEED.getFractionalLevel(savedHorse));
                message.append(" (").append(savedHorse.getDistanceTravelled()).append(")");
                message.append(". Health: ").append(CONFIG.HEALTH.getFractionalLevel(savedHorse));
                message.append(" (").append(savedHorse.getNuggetsEaten()).append(")");
                message.append(". Jump: ").append(CONFIG.JUMP.getFractionalLevel(savedHorse));
                message.append(" (").append(savedHorse.getDistanceJumped()).append(")");
                message.append(". Equipment: ").append(HorseEquipment.description(savedHorse.getEquipment()));
                message.append(".");

                getLogger().info(message.toString());
            }
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking on horses.
     *
     * For a player to be able to mount a skeletal or undead horse, the horse
     * must be tame. Those horses spawn tame, but after a /horse-free, their
     * tameness must be restored on the next interaction.
     *
     * Given that those horses will be kept tame full time, there is no point in
     * delaying locking them until the player mounts. This is because cancelling
     * the interaction (as happens when running a command) does not cancel the
     * mount. If a player runs /hinfo or /horse-levels and right clicks on an
     * undead horse, he will mount it, even though the command may cancel the
     * interaction event.
     *
     * The only possible alternative would be to require that players run a
     * command to tame these horses. But I that's undesirably counterintuitive.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!(entity instanceof Horse)) {
            return;
        }
        Horse horse = (Horse) entity;
        Player player = event.getPlayer();
        PlayerState playerState = getState(player);

        SavedHorse savedHorse = DB.findOrAddHorse(horse);
        if (player.equals(horse.getOwner())) {
            savedHorse.setLastAccessed(System.currentTimeMillis());
        } else {
            // Horses that are not interacted with by their owner for a
            // long time are spontaneously untamed. Since the horse is
            // being interacted with, the DB entry is retained.
            if (savedHorse.isAbandoned()) {
                horse.setOwner(null);
                if (CONFIG.DEBUG_EVENTS) {
                    debug(horse, "abandoned");
                }
            }
        }

        // Normalisation of trap horses: if they spawned tame, untame.
        if (horse.getOwner() == null && horse.isTamed()) {
            horse.setTamed(false);
            horse.setDomestication(1);
        }

        // Do pending trainable attribute updates resulting from /horse-swap.
        if (savedHorse.hasOutdatedAttributes()) {
            savedHorse.updateAllAttributes(horse);
        }

        EasyRider.DB.observe(savedHorse, horse);

        if (playerState.hasPendingInteraction()) {
            playerState.handlePendingInteraction(event, savedHorse);
            event.setCancelled(true);
        } else {
            // Allow players to feed golden carrots to living horses that they
            // cannot access for breeding purposes. Other foods don't work due
            // to vanilla limitations. May be broken in 1.11 because of:
            // https://bugs.mojang.com/browse/MC-93824
            // TODO: check in 1.11
            ItemStack item = player.getEquipment().getItemInMainHand();
            if (horse.getVariant() != Variant.SKELETON_HORSE && horse.getVariant() != Variant.UNDEAD_HORSE &&
                item != null && item.getType() == Material.GOLDEN_CARROT) {
                handleFeeding(horse, savedHorse, player);
            } else {
                // Prevent riding, leashing etc. of owned, locked horses.
                if (horse.getOwner() != null && !savedHorse.canBeAccessedBy(player) && !playerState.isBypassEnabled()) {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.GOLD + "You don't have access to this horse.");
                    if (player.hasPermission("easyrider.bypass")) {
                        player.sendMessage(ChatColor.GOLD + "Run " + ChatColor.YELLOW + "/horse-bypass" +
                                           ChatColor.GOLD + " to toggle the access bypass.");
                    }
                } else {
                    handleFeeding(horse, savedHorse, player);
                }
            }

            // Simulate taming of undead/skeletal horses. Vanilla code does most
            // of the work. We just need to put the player on an untamed horse.
            if (horse.getVariant() == Variant.SKELETON_HORSE || horse.getVariant() == Variant.UNDEAD_HORSE) {
                if (!horse.isTamed()) {
                    horse.setPassenger(player);
                }
            }
        }

        if (event.isCancelled()) {
            // Even though the event is cancelled, the player will end up
            // facing the same direction as the horse, so make the horse
            // face where the player should face.
            Location horseLoc = horse.getLocation();
            horseLoc.setYaw(player.getLocation().getYaw());
            horse.teleport(horseLoc);
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

        SavedHorse savedHorse = DB.findOrAddHorse(horse);
        DB.observe(savedHorse, horse);

        // Compute distance moved and update speed or jump depending on whether
        // the horse was on the ground.
        PlayerState playerState = getState(player);
        double tickDistance = playerState.getTickHorizontalDistance();
        if (tickDistance > 0 && !savedHorse.isDehydrated()) {
            // Sanity check: if the distance is so large as to be unattainable
            // in one tick, then don't apply the distance to the horse and log
            // in console. Ratio to max speed determined empirically.
            double maxSpeed = CONFIG.SPEED.getValue(savedHorse.getSpeedLevel() + 1);
            if (tickDistance > CONFIG.SPEED_LIMIT * maxSpeed) {
                getLogger().warning(horse.getOwner().getName() + "'s horse " + horse.getUniqueId() +
                                    " moved impossibly fast for its level; ratio: " + (tickDistance / maxSpeed));
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
        savedHorse.onRidden(_tickCounter, horse);

        // If the horse is owned and permission to ride the horse has been
        // retracted, eject the rider.
        if (horse.getOwner() != null && !savedHorse.canBeAccessedBy(player) && !playerState.isBypassEnabled()) {
            player.sendMessage(ChatColor.GOLD + "You no longer have permission to ride this horse.");
            horse.eject();
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

            SavedHorse savedHorse = DB.findOrAddHorse(horse);
            DB.observe(savedHorse, horse);

            getState(player).clearHorseDistance();

            // Rehydrate if remounting in water.
            if (findDrinkableBlock(horse.getLocation())) {
                savedHorse.setHydration(1.0);
                EasyRider.CONFIG.SPEED.updateAttribute(savedHorse, horse);
                player.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() +
                                   " drinks until it is no longer thirsty!");
                Location loc = horse.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, 2.0f, 1.0f);
            }

            if (horse.getOwner() != null && !player.equals(horse.getOwner())) {
                player.sendMessage(ChatColor.GOLD + "You are now riding " + horse.getOwner().getName() + "'s horse.");
            }

            if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                debug(horse, "Vehicle enter: " + player.getName());
            }
        }
    } // onVehicleEnter

    // ------------------------------------------------------------------------
    /**
     * Update observed horse state on vehicle exit.
     *
     * Include debug logging of vehicle exits.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getVehicle() instanceof Horse && event.getExited() instanceof Player) {
            Horse horse = (Horse) event.getVehicle();
            Player player = (Player) event.getExited();
            SavedHorse savedHorse = DB.findOrAddHorse(horse);
            DB.observe(savedHorse, horse);

            // Reset horse speed to that dictated by its level. It may have been
            // limited by a player-specific maximum speed.
            EasyRider.CONFIG.SPEED.updateAttribute(savedHorse, horse);

            if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                debug(horse, "Vehicle exit: " + player.getName());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Debugging of horse jumps.
     */
    @EventHandler(ignoreCancelled = true)
    public void onHorseJump(HorseJumpEvent event) {
        Horse horse = event.getEntity();
        Entity passenger = horse.getPassenger();
        if (!(passenger instanceof Player)) {
            return;
        }

        Player player = (Player) passenger;
        SavedHorse savedHorse = DB.findOrAddHorse(horse);

        if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
            debug(horse, "Horse jump: " + player.getName());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * On chunk unload, update database state of horses.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Horse) {
                Horse horse = (Horse) entity;
                SavedHorse savedHorse = DB.findHorse(horse);
                if (savedHorse != null) {
                    DB.observe(savedHorse, horse);
                }
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
     * Riderless horses travelling from the end are teleported to their owner's
     * bed spawn.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent event) {
        if (event.getEntity() instanceof Horse) {
            Horse horse = (Horse) event.getEntity();
            if (!(horse.getPassenger() instanceof Player)) {
                AnimalTamer owner = horse.getOwner();
                if (owner instanceof Player && event.getFrom().getWorld().getEnvironment() == Environment.THE_END) {
                    Location bedSpawnLoc = ((Player) owner).getBedSpawnLocation();
                    if (bedSpawnLoc != null) {
                        event.setCancelled(true);
                        horse.teleport(bedSpawnLoc);
                        DB.findOrAddHorse(horse).setLocation(bedSpawnLoc);
                    }
                }
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
        player.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() +
                           " is now Level " + ability.getLevel(savedHorse) +
                           " in " + ability.getDisplayName() + ".");
        Location loc = horse.getLocation().add(0, 1, 0);
        loc.getWorld().spigot().playEffect(loc, Effect.HAPPY_VILLAGER, 0, 0, 2.0f, 1.0f, 2.0f, 0.0f, 100, 32);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 2.0f, 1.0f);
    }

    // --------------------------------------------------------------------------
    /**
     * Handle feeding and watering of horses.
     *
     * @param horse the fed horse.
     * @param savedHorse the database state of the horse.
     * @param player the player feeding the horse.
     */
    protected void handleFeeding(Horse horse, SavedHorse savedHorse, Player player) {
        // Handle health training only if the event was not cancelled.
        ItemStack foodItem = player.getEquipment().getItemInMainHand();
        int nuggetValue = getNuggetValue(foodItem);
        if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
            getLogger().info("Nugget value: " + nuggetValue);
        }

        if (nuggetValue > 0) {
            // For undead horses, they take the food right away.
            if (horse.getVariant() == Variant.SKELETON_HORSE || horse.getVariant() == Variant.UNDEAD_HORSE) {
                foodItem.setAmount(foodItem.getAmount() - 1);
                player.getEquipment().setItemInMainHand(foodItem);
                consumeGoldenFood(savedHorse, horse, nuggetValue, player);

                // And let's simulate healing with golden food too.
                // Golden apples (both types) heal (10); carrots heal 4.
                int foodValue = (foodItem.getType() == Material.GOLDEN_APPLE) ? 10 : 4;
                horse.setHealth(Math.min(horse.getMaxHealth(), horse.getHealth() + foodValue));

            } else {
                // For other types of horses, detect whether the food
                // was consumed by running a task in the next tick.
                Bukkit.getScheduler().runTaskLater(this, new GoldConsumerTask(
                    player, horse, foodItem, nuggetValue, player.getInventory().getHeldItemSlot()), 0);
            }
        } else if (foodItem != null && foodItem.getType() == Material.WATER_BUCKET) {
            // Handle rehydration.
            if (!savedHorse.isFullyHydrated()) {
                player.getEquipment().setItemInMainHand(new ItemStack(Material.BUCKET, 1));
                savedHorse.setHydration(savedHorse.getHydration() + EasyRider.CONFIG.BUCKET_HYDRATION);
                Location loc = horse.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, 2.0f, 1.0f);
            }

            if (savedHorse.isFullyHydrated()) {
                player.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() + " is no longer thirsty.");
            } else {
                player.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() + " is still thirsty.");
            }
        }
    } // handleFeeding

    // ------------------------------------------------------------------------
    /**
     * Return the gold mass of one item only in a stack of food items, in units
     * of gold nuggets.
     *
     * @param food an ItemStack containing the food; only one item is counted,
     *        if there are multiple items in the stack.
     * @return the gold mass of a single food item in gold nuggets.
     */
    protected int getNuggetValue(ItemStack food) {
        if (food == null) {
            return 0;
        } else if (food.getType() == Material.GOLDEN_CARROT) {
            return 8;
        } else if (food.getType() == Material.GOLDEN_APPLE) {
            return (food.getDurability() == 0) ? 8 * 9 : 8 * 9 * 9;
        } else {
            return 0;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Increase a saved horse's health training effort according to the mass of
     * gold consumed, in gold nuggets.
     *
     * Play the eat sound effect and level the horse up if appropriate.
     *
     * Send the player a rate-limited notification message and sound if the
     * horse is already at its maximum health level and is being over-trained.
     *
     * @param savedHorse the database state of the horse.
     * @param horse the Horse entity.
     */
    protected void consumeGoldenFood(SavedHorse savedHorse, Horse horse, int nuggetValue, Player player) {
        CONFIG.HEALTH.setEffort(savedHorse, CONFIG.HEALTH.getEffort(savedHorse) + nuggetValue);
        if (CONFIG.HEALTH.hasLevelIncreased(savedHorse, horse)) {
            notifyLevelUp(player, savedHorse, horse, CONFIG.HEALTH);
        }

        Location loc = horse.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_EAT, 2.0f, 1.0f);

        if (CONFIG.HEALTH.getFractionalLevel(savedHorse) > CONFIG.HEALTH.getMaxLevel()) {
            savedHorse.onOverfed(player, horse);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if there is a potable water block in the 7 x 7 square centred
     * on the horse, either level with the horse's lower half or at ground level
     * (one block below).
     *
     * @param loc the horse's location
     * @return true if the horse can drink a block at feet level or ground level
     *         within 3 blocks of its location.
     */
    protected boolean findDrinkableBlock(Location loc) {
        return findDrinkableSquare(loc) ||
               findDrinkableSquare(loc.clone().add(0, -1, 0));
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if there is potable water in the 7 x 7 square centered on the
     * specified location.
     *
     * @param loc the horse's location
     * @return true if the horse can drink a block within 3 blocks of the
     *         location at the same Y level.
     */
    protected boolean findDrinkableSquare(Location loc) {
        Block feetBlock = loc.getBlock();
        if (feetBlock == null) {
            return false;
        }

        for (int x = -3; x <= 3; ++x) {
            for (int z = -3; z <= 3; ++z) {
                Block rel = feetBlock.getRelative(x, 0, z);
                if (rel != null && isDrinkable(rel)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if a block is a drinkable water source for a horse (filled
     * cauldron or water block).
     *
     * @param block the block, which must not be null.
     * @return true if a horse can drink the block.
     */
    @SuppressWarnings("deprecation")
    protected boolean isDrinkable(Block block) {
        return (block.getType() == Material.CAULDRON && block.getData() != 0) ||
               block.getType() == Material.WATER ||
               block.getType() == Material.STATIONARY_WATER;
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

    /**
     * Counter updated monotonically every tick.
     */
    int _tickCounter;

} // class EasyRider