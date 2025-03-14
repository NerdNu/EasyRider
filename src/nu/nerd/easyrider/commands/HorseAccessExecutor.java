package nu.nerd.easyrider.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import nu.nerd.easyrider.Util;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.PlayerState;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Handles the /horse-access command.
 */
public class HorseAccessExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseAccessExecutor() {
        super("horse-access", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("help")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player sendingPlayer = (Player) sender;
        if (args.length == 0 || args[0].startsWith("+") || args[0].startsWith("-")) {
            parseAccess(sendingPlayer, null, args);
        } else {
            // Treat the first arg as the partial UUID of the affected horse.
            List<SavedHorse> horses = EasyRider.DB.getOwnedHorses(sendingPlayer);
            List<SavedHorse> found = Util.findHorses(sendingPlayer, args[0]);
            if (found.size() == 0) {
                sender.sendMessage(ChatColor.RED + "The partial UUID " + args[0] + " does not match any animals that you own.");
            } else if (found.size() > 1) {
                sender.sendMessage(ChatColor.RED + "The partial UUID " + args[0] + " matches multiple animals that you own.");
            } else {
                parseAccess(sendingPlayer, found.get(0), Arrays.copyOfRange(args, 1, args.length));
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------
    /**
     * Parse arguments as a list of player names, each prefixed with '+' or '-'
     * and initiate access display or modification.
     * 
     * @param sendingPlayer the player sending the command.
     * @param savedHorse the affected horse, if specified in the first command
     *        arg, or null if the player must click on a horse.
     * @param args the prefixed player name arguments.
     */
    protected void parseAccess(Player sendingPlayer, SavedHorse savedHorse, String[] args) {
        if (savedHorse == null) {
            Entity vehicle = sendingPlayer.getVehicle();
            if (vehicle instanceof AbstractHorse) {
                AbstractHorse horse = (AbstractHorse) vehicle;
                savedHorse = EasyRider.DB.findHorse(horse);
            }
        }

        boolean addPublicHorse = false;
        boolean removePublicHorse = false;
        TreeSet<OfflinePlayer> added = new TreeSet<OfflinePlayer>((a, b) -> a.getName().compareTo(b.getName()));
        TreeSet<OfflinePlayer> removed = new TreeSet<OfflinePlayer>((a, b) -> a.getName().compareTo(b.getName()));
        for (String arg : args) {
            if (arg.equalsIgnoreCase("+public")) {
                addPublicHorse=true;
            } else if (arg.equalsIgnoreCase("-public")) {
                removePublicHorse=true;
            } else if (arg.startsWith("+") || arg.startsWith("-")) {
                String playerName = arg.substring(1);
                if (!Pattern.matches("\\w{1,16}", playerName)) {
                    sendingPlayer.sendMessage(ChatColor.RED + playerName + " is not a valid player name.");
                    return;
                }

                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (player == null) {
                    sendingPlayer.sendMessage(ChatColor.RED + playerName + " is not a valid player name.");
                    return;
                } else if (player.equals(sendingPlayer)) {
                    sendingPlayer.sendMessage(ChatColor.RED +
                                              "You can't alter your own permissions on an animal that you own.\n" +
                                              "Use /horse-free to release a horse.");
                } else if (!player.isOnline() && !player.hasPlayedBefore()) {
                    sendingPlayer.sendMessage(ChatColor.RED + "Error: " + player.getName() + " has not yet played on this server.");
                    return;
                } else {
                    if (arg.charAt(0) == '+') {
                        added.add(player);
                        removed.remove(player);
                    } else {
                        removed.add(player);
                        added.remove(player);
                    }
                }
            } else {
                sendingPlayer.sendMessage(ChatColor.RED + "Player names must begin with a '+' or '-'.");
                return;
            }
        }

	int publicHorse;
        if (addPublicHorse && !removePublicHorse) {
            publicHorse = 1;
        } else if (removePublicHorse) {
            publicHorse = 0;
        } else {
            publicHorse = -1;
        }

        if (savedHorse == null) {
            sendingPlayer.sendMessage(ChatColor.GOLD + "Right click on a horse, llama, or camel that you own.");
            PlayerState state = EasyRider.PLUGIN.getState(sendingPlayer);
            state.setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    doAccess(sendingPlayer, savedHorse, added, removed, publicHorse);
                }
            });
        } else {
            doAccess(sendingPlayer, savedHorse, added, removed, publicHorse);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Show or modify access to the specified horse.
     * 
     * @param sendingPlayr the player sending the command.
     * @param savedHorse the database state of the horse.
     * @param added a list of players to be granted access.
     * @param remove a list of players whose access is revoked.
     */
    protected void doAccess(Player sendingPlayer, SavedHorse savedHorse,
                            Set<OfflinePlayer> added, Set<OfflinePlayer> removed, int publicHorse) {
        sendingPlayer.sendMessage(ChatColor.GOLD + "Unique ID: " + ChatColor.WHITE + savedHorse.getUuid().toString());
        if (savedHorse.getDisplayName().length() != 0) {
            sendingPlayer.sendMessage(ChatColor.GOLD + "Name: " + ChatColor.YELLOW + savedHorse.getDisplayName());
        }
        OfflinePlayer owner = savedHorse.getOwner();
        sendingPlayer.sendMessage(ChatColor.GOLD + "Owner: " + ChatColor.GRAY + (owner == null ? "<nobody>" : owner.getName()));

        PlayerState state = EasyRider.PLUGIN.getState(sendingPlayer);
        if (savedHorse.getOwnerUuid() == null) {
            sendingPlayer.sendMessage(ChatColor.RED + "Nobody owns that animal.");
            return;
        }

        boolean canAlterACL = sendingPlayer.getUniqueId().equals(savedHorse.getOwnerUuid()) ||
                              state.isBypassEnabled();
        if (!canAlterACL) {
            sendingPlayer.sendMessage(ChatColor.RED + "You cannot alter the access list of animals you don't own.");
            return;
        }

        if (added.size() > 0) {
            savedHorse.addPermittedPlayers(added);
            sendingPlayer.sendMessage(ChatColor.GOLD + "Added: " +
                                      ChatColor.GRAY + String.join(" ", added.stream().map(p -> p.getName()).collect(Collectors.toList())));
        }
        if (removed.size() > 0) {
            savedHorse.removePermittedPlayers(removed);
            sendingPlayer.sendMessage(ChatColor.GOLD + "Removed: " +
                                      ChatColor.GRAY + String.join(" ", removed.stream().map(p -> p.getName()).collect(Collectors.toList())));
        }
        if (publicHorse == -1) {
            //do nothing
        } else if (publicHorse == 0) {
            savedHorse.setPublicHorse(false);
        } else {
            savedHorse.setPublicHorse(true);
        }

	if (savedHorse.getPublicHorse()) {
            sendingPlayer.sendMessage(ChatColor.GOLD + savedHorse.getMessageName() + " is " +
                                      ChatColor.RED + "PUBLIC" + ChatColor.GOLD + ". Know the risks!");
        } else {
            sendingPlayer.sendMessage(ChatColor.GOLD + "Access List: " +
                                      ChatColor.GRAY + String.join(" ", savedHorse.getAccessList()));
        }
    }
} // class HorseAccessExecutor
