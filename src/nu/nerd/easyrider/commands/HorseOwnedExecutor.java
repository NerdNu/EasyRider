package nu.nerd.easyrider.commands;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import nu.nerd.easyrider.Ability;
import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.HorseEquipment;
import nu.nerd.easyrider.Util;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Executor for the /horse-owned command.
 * 
 * This command would be called /horse-list, if CobraCorral had not already
 * taken that name.
 */
public class HorseOwnedExecutor extends ExecutorBase {

    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseOwnedExecutor() {
        super("horse-owned", "help");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }

        Player sendingPlayer = (Player) sender;
        OfflinePlayer owner = null;
        String ownerArg = null;
        int page = 1;

        switch (args.length) {
        case 0:
            owner = sendingPlayer;
            break;

        case 1:
            if (args[0].equalsIgnoreCase("help")) {
                return false;
            }

            // Try to parse the arg as a page number, first, then as owner.
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    sender.sendMessage(ChatColor.RED + "The page number must be at least 1.");
                    return true;
                }
            } catch (NumberFormatException ex) {
                ownerArg = args[0];
            }
            break;

        case 2:
            // First arg is player name, second arg is page #.
            ownerArg = args[0];
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

        if (owner == null) {
            if (ownerArg == null || ownerArg.equalsIgnoreCase(sendingPlayer.getName())) {
                owner = sendingPlayer;
            } else {
                // Owner is specified by name. Check permissions for this.
                if (!sender.hasPermission("easyrider.owned-player")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to list other players' horses.");
                    return true;
                }

                owner = Bukkit.getOfflinePlayer(ownerArg);
                if (owner == null) {
                    sender.sendMessage(ChatColor.RED + "Unknown player: " + ownerArg);
                    return true;
                }
            }
        }

        listHorses(sender, owner, EasyRider.DB.getOwnedHorses(owner), page);
        return true;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * Show the specified page of information about the horses.
     *
     * @param sender the command sender that receives the results.
     * @param owner the owner of the horses.
     * @param savedHorses the horses, sorted by UUID.
     * @param page the 1-based page to show.
     */
    protected void listHorses(CommandSender sender, OfflinePlayer owner,
                              ArrayList<SavedHorse> savedHorses, final int page) {
        final int PAGE_SIZE = 4;
        final int start = (page - 1) * PAGE_SIZE;
        final int end = Math.min(savedHorses.size(), start + PAGE_SIZE);
        final int pageCount = (savedHorses.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        if (start >= savedHorses.size()) {
            if (pageCount == 0) {
                sender.sendMessage(ChatColor.GOLD + owner.getName() + " does not own any horses.");
            } else {
                sender.sendMessage(ChatColor.RED + "The specified page number (" + page + ") exceeds the number of pages (" + pageCount + ").");
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "All horses owned by " + owner.getName() + ", page " + page + " of " + pageCount + ":");
            for (int i = start; i < end; ++i) {
                SavedHorse savedHorse = savedHorses.get(i);
                sender.sendMessage(ChatColor.GOLD + "#" + (i + 1) + " " +
                                   ChatColor.GRAY + Util.limitString(savedHorse.getUuid().toString(), 7) + " " +
                                   ChatColor.WHITE + savedHorse.getAppearance() + " " +
                                   ChatColor.YELLOW + Util.limitString(HorseEquipment.description(savedHorse.getEquipment()), 25));
                if (savedHorse.getDisplayName().length() > 0) {
                    sender.sendMessage(ChatColor.GOLD + "    Name: " +
                                       ChatColor.YELLOW + savedHorse.getDisplayName());
                }
                String seenDate;
                if (savedHorse.getLastObserved() == 0) {
                    seenDate = "never";
                } else {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(savedHorse.getLastObserved());
                    SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
                    format.setCalendar(cal);
                    seenDate = format.format(cal.getTime());
                }
                sender.sendMessage(ChatColor.GOLD + "    Last seen: " +
                                   ChatColor.WHITE + seenDate);
                Location loc = savedHorse.getLocation();
                String formattedLoc = (loc == null) ? "" : Util.formatLocation(loc);
                sender.sendMessage(ChatColor.GOLD + "    Location: " +
                                   ChatColor.WHITE + formattedLoc);
                sender.sendMessage(ChatColor.GOLD + "    Sp: " +
                                   formattedAbility(EasyRider.CONFIG.SPEED, savedHorse) +
                                   ChatColor.GOLD + " He: " +
                                   formattedAbility(EasyRider.CONFIG.HEALTH, savedHorse) +
                                   ChatColor.GOLD + " Ju: " +
                                   formattedAbility(EasyRider.CONFIG.JUMP, savedHorse));
            }
        }
    } // listHorses

    // ------------------------------------------------------------------------
    /**
     * Format the level and corresponding Horse Entity attribute value for a
     * specified Ability as a String.
     *
     * @param ability the Ability.
     * @param savedHorse the horse whose level and corresponding Attribute value
     *        are returned.
     * @return the formatted ability level and attribute value.
     */
    protected String formattedAbility(Ability ability, SavedHorse savedHorse) {
        double fractionalLevel = ability.getLevelForEffort(ability.getEffort(savedHorse));
        String formattedValue = ability.getFormattedValue(savedHorse);
        return new StringBuilder()
        .append((fractionalLevel >= ability.getMaxLevel()) ? ChatColor.RED : ChatColor.YELLOW)
        .append(String.format("%5.3f", fractionalLevel))
        .append(ChatColor.GRAY).append(" (").append(formattedValue).append(')')
        .toString();
    }
} // class HorseOwnedExecutor