package nu.nerd.easyrider.commands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * CommandExecutor implementation for the /horse-top command.
 *
 * When the maximum possible level is exceeded by training, colour the level
 * text red.
 */
public class HorseTopExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseTopExecutor() {
        super("horse-top", "health", "jump", "speed", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        String abilityName;
        int page;

        switch (args.length) {
        case 0:
            return false;

        case 1:
            if (args[0].equalsIgnoreCase("help")) {
                return false;
            } else {
                abilityName = args[0];
                page = 1;
            }
            break;

        case 2:
            // First arg is ability name, second arg is page #.
            abilityName = args[0];
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    sender.sendMessage(ChatColor.RED + "The page number must be at least 1.");
                    return true;
                }

            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + args[1] + " isn't a valid page number.");
                return true;
            }
            break;

        default:
            return false;
        } // switch

        final Ability ability = EasyRider.CONFIG.getAbility(abilityName);
        if (ability == null) {
            sender.sendMessage(ChatColor.RED + "That is not a valid ability name.");
            return true;
        }

        startSortTask(sender, ability, page);
        return true;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Start an asynchronous task to sort all horses by the specified ability
     * and list the specified page of results back to the command sender.
     *
     * @param sender the command sender.
     * @param ability the ability to sort horses into descending order.
     * @param page the 1-based page number to list.
     */
    protected void startSortTask(CommandSender sender, final Ability ability, final int page) {
        Bukkit.getScheduler().runTaskAsynchronously(EasyRider.PLUGIN, new Runnable() {
            @Override
            public void run() {
                Comparator<SavedHorse> comparator = new Comparator<SavedHorse>() {
                    @Override
                    public int compare(SavedHorse h1, SavedHorse h2) {
                        double h1Level = ability.getFractionalLevel(h1);
                        double h2Level = ability.getFractionalLevel(h2);
                        if (h1Level < h2Level) {
                            return 1;
                        } else if (h2Level < h1Level) {
                            return -1;
                        } else {
                            return h1.getUuid().compareTo(h2.getUuid());
                        }
                    }
                };

                ArrayList<SavedHorse> trainableHorses = EasyRider.DB.cloneAllHorses().stream()
                .filter(h -> h.isTrainable())
                .collect(Collectors.toCollection(ArrayList::new));
                trainableHorses.sort(comparator);

                // Set a non-negative index for the command senders best horse.
                final int ownBestIndex = findBestHorse(sender, trainableHorses);
                Bukkit.getScheduler().runTask(EasyRider.PLUGIN, new Runnable() {
                    @Override
                    public void run() {
                        showPage(sender, ability, trainableHorses, page, ownBestIndex);
                    }
                });
            }
        });
    } // startSortTask

    // ------------------------------------------------------------------------
    /**
     * Find the array index of the command sender's best horse.
     *
     * @param sender the command sender.
     * @param savedHorses all horses sorted in ascending order by some ability.
     * @return the index of the first horse whose owner matches the UUID of the
     *         sender, or -1 of the sender is not a player, or has no matching
     *         horse.
     */
    protected int findBestHorse(CommandSender sender, ArrayList<SavedHorse> savedHorses) {
        int bestIndex = -1;
        if (sender instanceof Player) {
            Player owner = (Player) sender;
            for (int i = 0; i < savedHorses.size(); ++i) {
                SavedHorse savedHorse = savedHorses.get(i);
                if (owner.getUniqueId().equals(savedHorse.getOwnerUuid())) {
                    bestIndex = i;
                    break;
                }
            }
        }
        return bestIndex;
    }

    // ------------------------------------------------------------------------
    /**
     * Show the specified page of results of horses sorted by ability.
     *
     * @param sender the command sender who receives messages.
     * @param ability the ability by which the horses are ranked.
     * @param savedHorses an array of all horses in ascending order by that
     *        ability.
     * @param page the 1-based page number to show.
     * @param bestIndex the array index of the sender's best horse, or -1 for
     *        none.
     */
    protected void showPage(CommandSender sender, Ability ability,
                            ArrayList<SavedHorse> savedHorses,
                            final int page, final int bestIndex) {
        final int PAGE_SIZE = 10;
        final int start = (page - 1) * PAGE_SIZE;
        final int end = Math.min(savedHorses.size(), start + PAGE_SIZE);
        final int pageCount = (savedHorses.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (start >= savedHorses.size()) {
            if (pageCount == 0) {
                sender.sendMessage(ChatColor.GOLD + "Nobody has trained any horses.");
            } else {
                sender.sendMessage(ChatColor.RED + "The specified page number (" + page + ") exceeds the number of pages (" + pageCount + ").");
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "All horses ranked by " + ability.getDisplayName() + ", page " + page + " of " + pageCount + ":");
            for (int i = start; i < end; ++i) {
                SavedHorse savedHorse = savedHorses.get(i);
                OfflinePlayer owner = savedHorse.getOwner();
                String ownerName = (owner != null) ? owner.getName() : "<no owner>";
                double fractionalLevel = ability.getFractionalLevel(savedHorse);
                ChatColor levelColour = (fractionalLevel >= ability.getMaxLevel()) ? ChatColor.RED : ChatColor.YELLOW;
                sender.sendMessage(ChatColor.GOLD + "#" + (i + 1) + " " +
                                   levelColour + String.format("%5.3f ", fractionalLevel) +
                                   ChatColor.WHITE + ownerName + " " +
                                   ChatColor.GRAY + savedHorse.getAppearance() + " " +
                                   ChatColor.WHITE + Util.limitString(savedHorse.getUuid().toString(), 7) + " " +
                                   ChatColor.YELLOW + Util.limitString(savedHorse.getDisplayName(), 16));
            }
            if (bestIndex >= 0) {
                sender.sendMessage(ChatColor.GOLD + "Your best horse in this ability is ranked #" + (bestIndex + 1) + ".");
            }
        }
    } // showPage
} // class HorseTopExecutor