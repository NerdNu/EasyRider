package nu.nerd.easyrider;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Llama;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import nu.nerd.easyrider.commands.EasyRiderExecutor;
import nu.nerd.easyrider.commands.ExecutorBase;
import nu.nerd.easyrider.commands.HorseAccessExecutor;
import nu.nerd.easyrider.commands.HorseBypassExecutor;
import nu.nerd.easyrider.commands.HorseDebugExecutor;
import nu.nerd.easyrider.commands.HorseDisguiseSelfExecutor;
import nu.nerd.easyrider.commands.HorseFreeExecutor;
import nu.nerd.easyrider.commands.HorseGPSExecutor;
import nu.nerd.easyrider.commands.HorseInfoExecutor;
import nu.nerd.easyrider.commands.HorseNeglectExecutor;
import nu.nerd.easyrider.commands.HorseNextExecutor;
import nu.nerd.easyrider.commands.HorseOwnedExecutor;
import nu.nerd.easyrider.commands.HorseSetAppearanceExecutor;
import nu.nerd.easyrider.commands.HorseSetLevelExecutor;
import nu.nerd.easyrider.commands.HorseSetNameExecutor;
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
 *
 * In 1.11, the Spigot API implements the following type hierarchy:
 *
 * <pre>
 * AbstractHorse
 *     ChestedHorse
 *         Donkey
 *         Llama
 *         Mule
 *     Horse
 *     SkeletonHorse
 *     ZombieHorse
 * </pre>
 *
 * There are two things to note here:
 * <ol>
 * <li>Donkeys, mules, and skeletal and zombie horses are no longer <i>horse
 * variants</i> in this API. In fact, they are not <i>Horse</i>s at all, they
 * are AbstractHorses. The AbstractHorse interface might have been better named
 * RideableAnimal.</li>
 * <li>Whereas a Llama is an AbstractHorse, it cannot be steered and EasyRider
 * doesn't implement trainability of its attributes. EasyRider distinguises
 * between trackable (lockable and findable) and trainable AbstractHorses. See
 * {@link Util#isTrackable(Entity)} and {@link Util#isTrainable(Entity)},
 * respectively.</li>
 * </ol>
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
     * Return the provider of the disguise facility, or null if not supported.
     * 
     * @return the provider of the disguise facility, or null if not supported.
     */
    public DisguiseProvider getDisguiseProvider() {
        return _disguiseProvider;
    }

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
        addCommandExecutor(new HorseSetAppearanceExecutor());
        addCommandExecutor(new HorseSetLevelExecutor());
        addCommandExecutor(new HorseSetNameExecutor());
        addCommandExecutor(new HorseSwapExecutor());
        addCommandExecutor(new HorseTPExecutor());
        addCommandExecutor(new HorseTPHereExecutor());
        addCommandExecutor(new HorseBypassExecutor());
        addCommandExecutor(new HorseTameExecutor());
        addCommandExecutor(new HorseFreeExecutor());
        addCommandExecutor(new HorseInfoExecutor());
        addCommandExecutor(new HorseUpgradesExecutor());
        addCommandExecutor(new HorseTopExecutor());
        addCommandExecutor(new HorseSpeedLimitExecutor());
        addCommandExecutor(new HorseGPSExecutor());
        addCommandExecutor(new HorseAccessExecutor());
        addCommandExecutor(new HorseOwnedExecutor());
        addCommandExecutor(new HorseNextExecutor());
        addCommandExecutor(new HorseDisguiseSelfExecutor());
        addCommandExecutor(new HorseNeglectExecutor());

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

        Plugin libsDisguises = Bukkit.getPluginManager().getPlugin("LibsDisguises");
        if (libsDisguises != null && libsDisguises.isEnabled()) {
            _disguiseProvider = new LibsDisguiseProvider();
        } else {
            getLogger().info("Note: LibsDisguises is missing or disabled.");
        }
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
        addState(event.getPlayer());
        SpecialSaddles.refreshSaddleDisguises();
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
        if (CONFIG.EJECT_ON_LOGOFF && Util.isTrackable(player.getVehicle())) {
            ((AbstractHorse) player.getVehicle()).eject();
        }

        PlayerState state = _state.remove(player.getName());
        state.save(_playerConfig);
    }

    // ------------------------------------------------------------------------
    /**
     * When a trainable AbstractHorse spawns, set its stats to defaults, unless
     * it is already known to the database (as is the case with horses going
     * through portals).
     *
     * Don't actually add the newly spawned AbstractHorse to the database until
     * a player interacts.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();
        if (Util.isTrainable(entity)) {
            AbstractHorse abstractHorse = (AbstractHorse) entity;
            SavedHorse savedHorse = DB.findHorse(abstractHorse);
            if (savedHorse == null) {
                CONFIG.SPEED.setAttribute(abstractHorse, 1);
                CONFIG.JUMP.setAttribute(abstractHorse, 1);
                CONFIG.HEALTH.setAttribute(abstractHorse, 1);
                if (CONFIG.DEBUG_EVENTS) {
                    debug(abstractHorse, " spawned, reason: " + event.getSpawnReason());
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Log taming of AbstractHorses, play the lock sound and message the owner
     * when they tame (lock) them.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityTame(EntityTameEvent event) {
        Entity entity = event.getEntity();
        Player owner = (Player) event.getOwner();
        if (Util.isTrackable(entity) && owner != null) {
            AbstractHorse abstractHorse = (AbstractHorse) entity;
            SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
            DB.observe(savedHorse, abstractHorse);

            owner.playSound(owner.getLocation(), Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.NEUTRAL, 1f, 1f);
            owner.sendMessage(ChatColor.GOLD + "This " + Util.entityTypeName(abstractHorse) + " has been locked.");
            getLogger().info(owner.getName() + " tamed " + Util.entityTypeName(abstractHorse) + " " + entity.getUniqueId().toString());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Protect trackable AbstractHorses from damage when they don't have a
     * player riding them.
     *
     * Check for abandoned ones and remove the database entry. Riderless
     * AbstractHorses taking damage from the void are teleported to their
     * owner's bed spawn location, or the world's spawn if a bed spawn is not
     * set.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (Util.isTrackable(entity)) {
            AbstractHorse abstractHorse = (AbstractHorse) entity;
            if (abstractHorse.getOwner() != null && !(Util.getPassenger(abstractHorse) instanceof Player)) {
                SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
                DB.observe(savedHorse, abstractHorse);
                if (savedHorse.isAbandoned()) {
                    abstractHorse.setOwner(null);
                    DB.removeHorse(savedHorse);
                } else {
                    event.setCancelled(true);
                    if (event.getCause() == DamageCause.VOID) {
                        OfflinePlayer owner = (OfflinePlayer) abstractHorse.getOwner();
                        Location safeLocation = owner.getBedSpawnLocation();
                        if (safeLocation == null) {
                            safeLocation = abstractHorse.getWorld().getSpawnLocation();
                        }
                        if (safeLocation != null) {
                            abstractHorse.teleport(safeLocation);
                            savedHorse.setLocation(safeLocation);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * If PvP is allowed, when an owned AbstractHorse is being ridden, it is
     * vulnerable to attacks by players, including both direct attacks and
     * projectiles.
     *
     * That includes inadvertent attacks by the owner or rider of the
     * AbstractHorse.
     */
    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (Util.isTrackable(event.getEntity()) && !CONFIG.ALLOW_PVP) {
            AbstractHorse abstractHorse = (AbstractHorse) event.getEntity();
            if (abstractHorse.getOwner() != null && Util.getPassenger(abstractHorse) instanceof Player) {
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
     * When a trackable AbstractHorse dies, remove it from the database and log
     * in the console.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        Entity entity = event.getEntity();
        if (Util.isTrackable(entity)) {
            AbstractHorse abstractHorse = (AbstractHorse) entity;
            SavedHorse savedHorse = DB.findHorse(abstractHorse);
            if (savedHorse != null) {
                DB.removeHorse(savedHorse);

                String passengerName = "";
                Entity passenger = Util.getPassenger(abstractHorse);
                if (passenger != null) {
                    passengerName = passenger instanceof Player ? ((Player) passenger).getName()
                                                                : passenger.toString();
                }
                String deathCause = (abstractHorse.getKiller() == null ? "the environment" : abstractHorse.getKiller().getName());

                StringBuilder message = new StringBuilder();
                message.append("Horse died ");
                message.append(abstractHorse.getUniqueId().toString());
                message.append(": ");

                AnimalTamer owner = abstractHorse.getOwner();
                if (owner instanceof Player) {
                    message.append("Owner: ").append(owner.getName());

                    // Tell the owner if someone else was riding.
                    if (!owner.equals(passenger)) {
                        StringBuilder horseDescription = new StringBuilder("Your ");
                        horseDescription.append(abstractHorse.getCustomName() != null ? abstractHorse.getCustomName()
                                                                                      : Util.entityTypeName(abstractHorse));
                        horseDescription.append(" (");
                        horseDescription.append(Util.limitString(abstractHorse.getUniqueId().toString(), 12));
                        horseDescription.append(") ");

                        ((Player) owner).sendMessage(ChatColor.RED + horseDescription.toString() + " has died due to " + deathCause +
                                                     (passengerName.isEmpty() ? "." : " while being ridden by " + passengerName + "."));
                    }
                } else {
                    message.append("No owner");
                }

                message.append(". Appearance: ").append(savedHorse.getAppearance());
                message.append(". Equipment: ").append(HorseEquipment.description(savedHorse.getEquipment()));
                if (Util.isTrainable(abstractHorse)) {
                    message.append(". Speed: ").append(CONFIG.SPEED.getFractionalLevel(savedHorse));
                    message.append(" (").append(savedHorse.getDistanceTravelled()).append(")");
                    message.append(". Health: ").append(CONFIG.HEALTH.getFractionalLevel(savedHorse));
                    message.append(" (").append(savedHorse.getNuggetsEaten()).append(")");
                    message.append(". Jump: ").append(CONFIG.JUMP.getFractionalLevel(savedHorse));
                    message.append(" (").append(savedHorse.getDistanceJumped()).append(")");
                } else if (abstractHorse instanceof Llama) {
                    Llama llama = (Llama) abstractHorse;
                    message.append(". Strength: ").append(llama.getStrength());
                    message.append(". Speed: ").append(CONFIG.SPEED.toDisplayValue(CONFIG.SPEED.getAttribute(abstractHorse)));
                    message.append(". Health: ").append(CONFIG.HEALTH.toDisplayValue(CONFIG.HEALTH.getAttribute(abstractHorse)));
                    message.append(". Jump: ").append(CONFIG.JUMP.toDisplayValue(CONFIG.JUMP.getAttribute(abstractHorse)));
                }
                message.append(". Cause: ").append(deathCause);
                message.append(". Passenger: ").append(passengerName.isEmpty() ? "<none>" : passengerName);
                message.append(". Location: ").append(Util.formatLocation(event.getEntity().getLocation()));
                message.append(".");

                getLogger().info(message.toString());
            }
        }
    } // onEntityDeath

    // ------------------------------------------------------------------------
    /**
     * Handle players right clicking on horses.
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (!Util.isTrackable(entity)) {
            return;
        }
        AbstractHorse abstractHorse = (AbstractHorse) entity;
        Player player = event.getPlayer();
        PlayerState playerState = getState(player);
        Location playerLoc = player.getLocation();

        SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
        if (player.equals(abstractHorse.getOwner())) {
            savedHorse.setLastAccessed(System.currentTimeMillis());
        } else {
            // Horses that are not interacted with by their owner for a
            // long time are spontaneously untamed. Since the horse is
            // being interacted with, the DB entry is retained.
            if (savedHorse.isAbandoned()) {
                abstractHorse.setOwner(null);
                if (CONFIG.DEBUG_EVENTS) {
                    debug(abstractHorse, "abandoned");
                }
            }
        }

        // Normalisation of trap horses: if they spawned tame, untame.
        if (abstractHorse.getOwner() == null && abstractHorse.isTamed()) {
            abstractHorse.setTamed(false);
            abstractHorse.setDomestication(1);
        }

        // Do pending trainable attribute updates resulting from /horse-swap.
        // Also fix some horses that got attributes minimised by mistake when
        // they "spawned" by going through a portal.
        if (Util.isTrainable(abstractHorse)) {
            savedHorse.updateAllAttributes(abstractHorse);
        }

        EasyRider.DB.observe(savedHorse, abstractHorse);

        if (playerState.hasPendingInteraction()) {
            playerState.handlePendingInteraction(event, savedHorse);
            event.setCancelled(true);
        } else {
            ItemStack item = player.getEquipment().getItemInMainHand();
            if (Util.isTrainable(abstractHorse)) {
                // Allow players to feed golden carrots to living horses that
                // they cannot access for breeding purposes. Other foods don't
                // work due to vanilla limitations.
                if (!Util.isUndeadHorse(abstractHorse) &&
                    item != null && item.getType() == Material.GOLDEN_CARROT) {
                    handleFeeding(abstractHorse, savedHorse, player);
                } else {
                    // Prevent riding, leashing etc. of owned, locked horses.
                    if (isAccessible(savedHorse, abstractHorse, player, playerState)) {
                        handleFeeding(abstractHorse, savedHorse, player);
                    } else {
                        event.setCancelled(true);
                    }
                }

                // Simulate taming of undead horses. Vanilla code does most of
                // the work. We just need to put the player on an untamed horse.
                if (Util.isUndeadHorse(abstractHorse) && !abstractHorse.isTamed()) {
                    abstractHorse.addPassenger(player);
                }
            } else if (abstractHorse instanceof Llama) {
                // Allow anyone to feed hay blocks to locked llamas.
                boolean canAccess = (item != null && item.getType() == Material.HAY_BLOCK) ||
                                    isAccessible(savedHorse, abstractHorse, player, playerState);
                if (!canAccess) {
                    event.setCancelled(true);
                }
            }
        }

        if (event.isCancelled()) {
            // Even though the event is cancelled, the player will end up
            // facing the same direction as the horse. Restore the player's
            // original look direction.
            if (player.getVehicle() == null) {
                player.teleport(playerLoc);
            }
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
        if (!Util.isTrackable(vehicle)) {
            return;
        }

        AbstractHorse abstractHorse = (AbstractHorse) vehicle;
        if (abstractHorse.isInsideVehicle()) {
            // The horse cannot be trained by moving it around in a vehicle.
            return;
        }
        PlayerState playerState = getState(player);
        SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
        if (Util.isTrainable(abstractHorse)) {
            // NOTE: call onRidden() before observe() for correct dehydration.
            savedHorse.onRidden(_tickCounter, abstractHorse);

            // Compute distance moved and update speed or jump depending on
            // whether the horse was on the ground.
            double tickDistance = playerState.getTickHorizontalDistance();
            if (tickDistance > 0 && !savedHorse.isDehydrated()) {
                // Sanity check: if the distance is so large as to be
                // unattainable in one tick, then don't apply the distance to
                // the horse and log in console. Ratio to max speed determined
                // empirically.
                double maxSpeed = CONFIG.SPEED.getValue(savedHorse.getSpeedLevel() + 1);
                if (tickDistance > CONFIG.SPEED_LIMIT * maxSpeed) {
                    String ownerClause = (abstractHorse.getOwner() != null ? abstractHorse.getOwner().getName() + "'s"
                                                                           : "Unowned");
                    getLogger().warning(ownerClause + " horse " + abstractHorse.getUniqueId() +
                                        " moved impossibly fast for its level; ratio: " + (tickDistance / maxSpeed));
                } else {
                    // Underwater training (of skeleton horses) counts as
                    // speed rather than jump training.
                    boolean underWater = Util.isWaterlogged(abstractHorse.getLocation().getBlock());
                    Ability ability = (abstractHorse.isOnGround() || underWater) ? CONFIG.SPEED
                                                                                 : CONFIG.JUMP;
                    ability.setEffort(savedHorse, ability.getEffort(savedHorse) + tickDistance);
                    if (ability.hasLevelIncreased(savedHorse, abstractHorse)) {
                        notifyLevelUp(player, savedHorse, abstractHorse, ability);
                    }
                }
            }

            // Update stored location to compute distance in the next tick.
            playerState.updateRiddenHorse();
        }

        // Observe the AbstractHorse's new location.
        DB.observe(savedHorse, abstractHorse);

        // If the horse is owned and permission to ride the horse has been
        // retracted, eject the rider.
        if (abstractHorse.getOwner() != null && !savedHorse.canBeAccessedBy(player) && !playerState.isBypassEnabled()) {
            player.sendMessage(ChatColor.GOLD + "You no longer have permission to ride this " + Util.entityTypeName(abstractHorse) + ".");
            abstractHorse.eject();
        }
    } // onPlayerMove

    // ------------------------------------------------------------------------
    /**
     * When feeding golden carrots, it may be possible for the right click to
     * get past the PlayerInteractEntityEvent even when the player doesn't have
     * access. Ensure that inventory access control is enforced. See the doc
     * comment for {@link EasyRider#onVehicleEnter(VehicleEnterEvent)} for more
     * information.
     */
    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractHorse) || !(event.getPlayer() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getPlayer();
        AbstractHorse abstractHorse = (AbstractHorse) holder;
        if (Util.isTrackable(abstractHorse)) {
            SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
            DB.observe(savedHorse, abstractHorse);

            PlayerState playerState = getState(player);
            if (!isAccessible(savedHorse, abstractHorse, player, playerState)) {
                event.setCancelled(true);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * After an inventory click, check what the final result is and disguise the
     * steed if it has a player riding it.
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (getDisguiseProvider() == null) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractHorse) ||
            !(event.getWhoClicked() instanceof Player)) {
            return;
        }

        // Exclude unsupported AbstractHorse subtypes.
        if (holder instanceof Llama) {
            return;
        }

        ItemStack oldSaddle = event.getInventory().getItem(0);
        Bukkit.getScheduler().runTaskLater(this, () -> {
            // Require that the horse has a human passenger before applying
            // a disguise. Note that the player doing the inventory editing
            // is not necessarily the rider.
            AbstractHorse abstractHorse = (AbstractHorse) holder;
            Player rider = null;
            for (Entity passenger : abstractHorse.getPassengers()) {
                if (passenger instanceof Player) {
                    rider = (Player) passenger;
                    break;
                }
            }
            if (rider == null) {
                return;
            }

            // Check that the saddle item has changed to silence unnecessary
            // disguise messages caused by players clicking in the inventory of
            // a disguised horse.
            ItemStack newSaddle = event.getInventory().getItem(0);
            if ((oldSaddle == null && newSaddle != null) ||
                (oldSaddle != null && !oldSaddle.equals(newSaddle))) {
                String encodedDisguise = SpecialSaddles.getSaddleEncodedDisguise(abstractHorse);
                if (encodedDisguise == null) {
                    getDisguiseProvider().removeDisguise(abstractHorse);
                } else {
                    SpecialSaddles.applySaddleDisguise(abstractHorse, rider, encodedDisguise, false, true);
                }
            }
        }, 1);
    }

    // ------------------------------------------------------------------------
    /**
     * When a player closes a horse's inventory, snapshot the contents.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof AbstractHorse)) {
            return;
        }

        AbstractHorse abstractHorse = (AbstractHorse) holder;
        SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
        savedHorse.observeInventory(abstractHorse);
    }

    // ------------------------------------------------------------------------
    /**
     * When a player mounts a horse, clear the recorded location of the horse in
     * the previous tick.
     *
     * Note that players can switch from horse to horse without dismounting,
     * which would mess up distance ridden calculations if we simply stored the
     * destination horse's location.
     *
     * In 1.11, since we allow players to feed horses golden carrots even if
     * they are otherwise inaccessible, and since 1.11 horses will not consume
     * the carrot if already in love mode (which we can't detect with a query
     * method) it is possible for the interaction event to not be cancelled even
     * though the player has no access and for the player to end up riding the
     * horse or accessing it's inventory. Let's stop that here and in the
     * inventory open event.
     */
    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!Util.isTrackable(event.getVehicle())) {
            return;
        }

        AbstractHorse abstractHorse = (AbstractHorse) event.getVehicle();
        Entity passenger = event.getEntered();
        if (passenger instanceof Player) {
            Player player = (Player) passenger;

            // During the Minecraft 1.15.2 era, Spigot reversed the ordering of
            // VehicleEnterEvent relative to PlayerJoinEvent so that the
            // former preceded the latter (players now enter their vehicles
            // before joining the server) if they logged out in a vehicle.
            addState(player);

            SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
            DB.observe(savedHorse, abstractHorse);

            PlayerState playerState = getState(player);
            playerState.clearHorseDistance();

            if (!isAccessible(savedHorse, abstractHorse, player, playerState)) {
                event.setCancelled(true);
                return;
            }

            if (Util.isTrainable(abstractHorse)) {
                EasyRider.CONFIG.SPEED.updateAttribute(savedHorse, abstractHorse);
            }

            handleDrinking(abstractHorse, savedHorse, player);

            if (abstractHorse.getOwner() != null && !player.equals(abstractHorse.getOwner())) {
                player.sendMessage(ChatColor.GOLD + "You are now riding " + abstractHorse.getOwner().getName() + "'s " +
                                   Util.entityTypeName(abstractHorse) + ".");
            }

            String encodedDisguise = SpecialSaddles.getSaddleEncodedDisguise(abstractHorse);
            if (encodedDisguise != null) {
                SpecialSaddles.applySaddleDisguise(abstractHorse, player, encodedDisguise, false, true);
            }

            if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                debug(abstractHorse, "Vehicle enter: " + player.getName());
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
        if (Util.isTrackable(event.getVehicle()) && event.getExited() instanceof Player) {
            AbstractHorse abstractHorse = (AbstractHorse) event.getVehicle();
            Player player = (Player) event.getExited();
            SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);
            DB.observe(savedHorse, abstractHorse);

            if (Util.isTrainable(abstractHorse)) {
                // Reset horse speed to that dictated by its level. It may have
                // been limited by a player-specific maximum speed.
                EasyRider.CONFIG.SPEED.updateAttribute(savedHorse, abstractHorse);
            }

            handleDrinking(abstractHorse, savedHorse, player);

            // Clear disguise on dismount.
            if (getDisguiseProvider() != null) {
                String encodedDisguise = SpecialSaddles.getSaddleEncodedDisguise(abstractHorse);
                if (encodedDisguise != null) {
                    getDisguiseProvider().removeDisguise(abstractHorse);
                }
            }

            if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
                debug(abstractHorse, "Vehicle exit: " + player.getName());
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Debugging of horse jumps.
     */
    @EventHandler(ignoreCancelled = true)
    public void onHorseJump(HorseJumpEvent event) {
        AbstractHorse abstractHorse = event.getEntity();
        Entity passenger = Util.getPassenger(abstractHorse);
        if (!(passenger instanceof Player)) {
            return;
        }

        Player player = (Player) passenger;
        SavedHorse savedHorse = DB.findOrAddHorse(abstractHorse);

        if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
            debug(abstractHorse, "Horse jump: " + player.getName());
        }
    }

    // ------------------------------------------------------------------------
    /**
     * On chunk unload, update database state of horses.
     */
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (Util.isTrackable(entity)) {
                AbstractHorse abstractHorse = (AbstractHorse) entity;
                SavedHorse savedHorse = DB.findHorse(abstractHorse);
                if (savedHorse != null) {
                    DB.observe(savedHorse, abstractHorse);
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
        if (Util.isTrackable(event.getEntity())) {
            AbstractHorse abstractHorse = (AbstractHorse) event.getEntity();
            Entity passenger = Util.getPassenger(abstractHorse);
            if (passenger instanceof Player) {
                Player player = (Player) passenger;
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
        if (Util.isTrackable(event.getEntity())) {
            AbstractHorse abstractHorse = (AbstractHorse) event.getEntity();
            Entity passenger = Util.getPassenger(abstractHorse);
            if (!(passenger instanceof Player)) {
                AnimalTamer owner = abstractHorse.getOwner();
                if (owner instanceof Player && event.getFrom().getWorld().getEnvironment() == Environment.THE_END) {
                    Location bedSpawnLoc = ((Player) owner).getBedSpawnLocation();
                    if (bedSpawnLoc != null) {
                        event.setCancelled(true);
                        abstractHorse.teleport(bedSpawnLoc);
                        DB.findOrAddHorse(abstractHorse).setLocation(bedSpawnLoc);
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
    public void debug(AbstractHorse abstractHorse, String message) {
        debug(abstractHorse.getUniqueId() + ": " + message);
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
     * Add PlayerState for the specified player.
     * 
     * @param player the Player.
     */
    protected void addState(Player player) {
        if (!_state.containsKey(player.getName())) {
            _state.put(player.getName(), new PlayerState(player, _playerConfig));
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Notify the player that the specified horse has changed its level, and
     * play the corresponding sound and particle effects.
     *
     * @param player     the player.
     * @param savedHorse the database state of the horse.
     * @param horse      the AbstractHorse entity.
     * @param ability    the affected ability.
     */
    protected void notifyLevelUp(Player player, SavedHorse savedHorse, AbstractHorse horse, Ability ability) {
        player.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() +
                           " is now Level " + ability.getLevel(savedHorse) +
                           " in " + ability.getDisplayName() + ".");
        Location loc = horse.getLocation().add(0, 1, 0);
        loc.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc, 100, 2.0f, 1.0f, 2.0f);
        loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.NEUTRAL, 2.0f, 1.0f);
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if a given AbstractHorse is accessible by a player.
     *
     * A horse could be accessible because it is owned by the player, or the
     * player is on the access list for the horse, or the player is in bypass
     * mode.
     *
     * @param savedHorse    the database state of the AbstractHorse.
     * @param abstractHorse the horse-like entity.
     * @param player        the player.
     * @param playerState   the player's transient state.
     * @return true if a given AbstractHorse is accessible by a player.
     */
    protected boolean isAccessible(SavedHorse savedHorse, AbstractHorse abstractHorse, Player player, PlayerState playerState) {
        if (abstractHorse.getOwner() != null && !savedHorse.canBeAccessedBy(player) && !playerState.isBypassEnabled()) {
            player.sendMessage(ChatColor.GOLD + "You don't have access to this " + Util.entityTypeName(abstractHorse) + ".");
            player.sendMessage(ChatColor.GOLD + "It belongs to " + ChatColor.YELLOW + abstractHorse.getOwner().getName() + ChatColor.GOLD + ".");
            if (player.hasPermission("easyrider.bypass")) {
                player.sendMessage(ChatColor.GOLD + "Run " + ChatColor.YELLOW + "/horse-bypass" +
                                   ChatColor.GOLD + " to toggle the access bypass.");
            }
            return false;
        } else {
            return true;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Let the horse drink if it is not fully hydrated.
     * 
     * @param abstractHorse the horse.
     * @param savedHorse    the database state of the horse.
     * @param player        the player messaged about the horse's drinking.
     */
    protected void handleDrinking(AbstractHorse abstractHorse, SavedHorse savedHorse, Player player) {
        if (Util.isTrainable(abstractHorse)) {
            // Don't drink if the horse is already *nearly* full hydration.
            // Don't drink due to teleport triggered by command.
            if (!savedHorse.isFullyHydrated() && findDrinkableBlock(abstractHorse.getLocation()) &&
                !isCommandExecuting()) {
                savedHorse.setHydration(1.0);
                player.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() + " drinks until it is no longer thirsty!");
                Location loc = abstractHorse.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, SoundCategory.NEUTRAL, 2.0f, 1.0f);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Check whether a command is currently executing.
     *
     * Some commands teleport the player in order to set the player's look
     * angle, and require that the player be re-mounted in his vehicle,
     * triggering a vehicle entry or exit event. To avoid player exploitation of
     * this behaviour to bypass horse dehydration mechanics, we check whether a
     * command is executing.
     * 
     * For efficiency, we reflectively check the CraftServer.playerCommandState
     * field, taking on average ~0.01 ms, rather than walking the stack trace
     * (~0.2 ms). This will most likely break if we ever switch to a
     * non-CraftBukkit-derived server.
     * 
     * @return true if a command is currently executing.
     */
    protected boolean isCommandExecuting() {
        // long start = System.nanoTime();
        // boolean inCommand = false;
        // for (StackTraceElement e : Thread.currentThread().getStackTrace()) {
        // if (e.getMethodName().equals("handleCommand")) {
        // inCommand = true;
        // break;
        // }
        // }
        // long now = System.nanoTime();
        // double elapsedMillis = (now - start) * 1e-6;
        // getLogger().info("handleCommand: " + inCommand + " elapsed ms: " +
        // elapsedMillis);

        try {
            Field playerCommandState = Bukkit.getServer().getClass().getField("playerCommandState");
            return playerCommandState.getBoolean(Bukkit.getServer());
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException ex) {
            getLogger().severe("This plugin needs to be updated for the current server (EasyRider.isCommandExecuting()).");
            return false;
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Handle feeding and watering of trainable horses.
     *
     * @param abstractHorse the fed horse.
     * @param savedHorse    the database state of the horse.
     * @param player        the player feeding the horse.
     */
    protected void handleFeeding(AbstractHorse abstractHorse, SavedHorse savedHorse, Player player) {
        // Handle health training only if the event was not cancelled.
        ItemStack foodItem = player.getEquipment().getItemInMainHand();
        int nuggetValue = getNuggetValue(foodItem);
        if (CONFIG.DEBUG_EVENTS && savedHorse.isDebug()) {
            getLogger().info("Nugget value: " + nuggetValue);
        }

        if (nuggetValue > 0) {
            // For undead horses, they take the food right away.
            if (Util.isUndeadHorse(abstractHorse)) {
                foodItem.setAmount(foodItem.getAmount() - 1);
                player.getEquipment().setItemInMainHand(foodItem);
                consumeGoldenFood(savedHorse, abstractHorse, nuggetValue, player);

                // And let's simulate healing with golden food too.
                // Golden apples (both types) heal (10); carrots heal 4.
                int foodValue = (foodItem.getType() == Material.GOLDEN_APPLE) ? 10 : 4;
                AttributeInstance maxHealth = abstractHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                abstractHorse.setHealth(Math.min(maxHealth.getValue(), abstractHorse.getHealth() + foodValue));

            } else {
                // For other types of horses, detect whether the food
                // was consumed by running a task in the next tick.
                Bukkit.getScheduler().runTaskLater(this, new GoldConsumerTask(
                    player, abstractHorse, foodItem, nuggetValue, player.getInventory().getHeldItemSlot()), 0);
            }
        } else if (foodItem != null && foodItem.getType() == Material.WATER_BUCKET) {
            // Handle rehydration.
            if (!savedHorse.isFullyHydrated()) {
                player.getEquipment().setItemInMainHand(new ItemStack(Material.BUCKET, 1));
                savedHorse.setHydration(savedHorse.getHydration() + EasyRider.CONFIG.BUCKET_HYDRATION);
                Location loc = abstractHorse.getLocation();
                loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_DRINK, SoundCategory.NEUTRAL, 2.0f, 1.0f);
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
     *             if there are multiple items in the stack.
     * @return the gold mass of a single food item in gold nuggets.
     */
    protected int getNuggetValue(ItemStack food) {
        if (food == null) {
            return 0;
        } else if (food.getType() == Material.GOLDEN_CARROT) {
            return 8;
        } else if (food.getType() == Material.GOLDEN_APPLE) {
            return 8 * 9;
        } else if (food.getType() == Material.ENCHANTED_GOLDEN_APPLE) {
            return 8 * 9 * 9;
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
     * @param horse      the AbstractHorse entity.
     */
    protected void consumeGoldenFood(SavedHorse savedHorse, AbstractHorse horse, int nuggetValue, Player player) {
        CONFIG.HEALTH.setEffort(savedHorse, CONFIG.HEALTH.getEffort(savedHorse) + nuggetValue);
        if (CONFIG.HEALTH.hasLevelIncreased(savedHorse, horse)) {
            notifyLevelUp(player, savedHorse, horse, CONFIG.HEALTH);
        }

        Location loc = horse.getLocation();
        loc.getWorld().playSound(loc, Sound.ENTITY_HORSE_EAT, SoundCategory.NEUTRAL, 2.0f, 1.0f);

        if (CONFIG.HEALTH.getFractionalLevel(savedHorse) > CONFIG.HEALTH.getMaxLevel()) {
            savedHorse.onOverfed(player, horse);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return true if there is a potable water block in the 7 x 7 square centred
     * on the horse, either level with the horse's lower half, or upper half or
     * at ground level (one block below).
     *
     * @param loc the horse's location
     * @return true if the horse can drink a block at feet level, head level or
     *         ground level within 3 blocks of its location.
     */
    protected boolean findDrinkableBlock(Location loc) {
        return findDrinkableSquare(loc) ||
               findDrinkableSquare(loc.clone().add(0, -1, 0)) ||
               findDrinkableSquare(loc.clone().add(0, +1, 0));
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
    protected boolean isDrinkable(Block block) {
        BlockData data = block.getBlockData();
        Waterlogged waterlogged = (data instanceof Waterlogged) ? (Waterlogged) data : null;
        return (waterlogged != null && waterlogged.isWaterlogged()) ||
               (block.getType() == Material.CAULDRON && ((Levelled) data).getLevel() != 0) ||
               block.getType() == Material.WATER;
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
    protected static final String PLAYERS_FILE = "players.yml";

    /**
     * Start of lore string on saddles indicating that the saddle confers a
     * disguise.
     */
    protected static final String DISGUISE_PREFIX = "Disguise:";

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
    protected int _tickCounter;

    /**
     * Provides the disguise facility, or null if disguises are not supported.
     */
    protected DisguiseProvider _disguiseProvider;

} // class EasyRider
