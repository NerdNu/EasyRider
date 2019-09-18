package nu.nerd.easyrider.commands;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import nu.nerd.easyrider.EasyRider;
import nu.nerd.easyrider.IPendingInteraction;
import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Executor for the /horse-set-appearance admin command.
 */
public class HorseSetAppearanceExecutor extends ExecutorBase {
    // ------------------------------------------------------------------------
    /**
     * Default constructor.
     */
    public HorseSetAppearanceExecutor() {
        super("horse-set-appearance");
    }

    // ------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            Bukkit.getScheduler().runTaskLater(EasyRider.PLUGIN, () -> {
                showColoursAndStyles(sender);
            }, 0);
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You must be in game to use this command.");
            return true;
        }
        Player player = (Player) sender;

        if (args.length == 1) {
            Llama.Color newColour;
            try {
                newColour = Llama.Color.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid llama colour: " + args[0]);
                showColoursAndStyles(sender);
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "Right click on a llama to set its colour to: " + ChatColor.WHITE + newColour);
            sender.sendMessage(ChatColor.GOLD + "To cancel, relog.");
            EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    Player player = event.getPlayer();
                    AbstractHorse abstractHorse = (AbstractHorse) event.getRightClicked();
                    if (abstractHorse instanceof Llama) {
                        Llama llama = (Llama) abstractHorse;
                        player.sendMessage(ChatColor.GOLD + "Llama: " + ChatColor.YELLOW + abstractHorse.getUniqueId());
                        player.sendMessage(ChatColor.GOLD + "Old colour: " + ChatColor.YELLOW + llama.getColor());
                        player.sendMessage(ChatColor.GOLD + "New colour: " + ChatColor.YELLOW + newColour);
                        llama.setColor(newColour);
                    } else {
                        player.sendMessage(ChatColor.RED + "That's not a llama! The colour did not change.");
                    }
                }
            });
        } else if (args.length == 2) {
            Horse.Color newColour;
            try {
                newColour = Horse.Color.valueOf(args[0].toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid horse colour: " + args[0]);
                showColoursAndStyles(sender);
                return true;
            }

            Horse.Style newStyle;
            try {
                newStyle = Horse.Style.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(ChatColor.RED + "Invalid horse style: " + args[1]);
                showColoursAndStyles(sender);
                return true;
            }

            sender.sendMessage(ChatColor.GOLD + "Right click on a horse to set its appearance to: " + ChatColor.WHITE + newColour + " " + newStyle);
            sender.sendMessage(ChatColor.GOLD + "To cancel, relog.");
            EasyRider.PLUGIN.getState(player).setPendingInteraction(new IPendingInteraction() {
                @Override
                public void onPlayerInteractEntity(PlayerInteractEntityEvent event, SavedHorse savedHorse) {
                    Player player = event.getPlayer();
                    AbstractHorse abstractHorse = (AbstractHorse) event.getRightClicked();
                    if (abstractHorse instanceof Horse) {
                        Horse horse = (Horse) abstractHorse;
                        player.sendMessage(ChatColor.GOLD + "Horse: " + ChatColor.YELLOW + abstractHorse.getUniqueId());
                        player.sendMessage(ChatColor.GOLD + "Old colour, style: " + ChatColor.YELLOW + horse.getColor() + " " + horse.getStyle());
                        player.sendMessage(ChatColor.GOLD + "New colour, style: " + ChatColor.YELLOW + newColour + " " + newStyle);
                        horse.setColor(newColour);
                        horse.setStyle(newStyle);
                    } else {
                        player.sendMessage(ChatColor.RED + "That's not a horse! The colour did not change.");
                    }
                }
            });
        } else {
            sender.sendMessage(ChatColor.RED + " Invalid arguments. Try /" + getName() + " help.");
        }

        return true;
    } // onCommand

    // ------------------------------------------------------------------------
    /**
     * List all possible llama and horse colours and styles.
     * 
     * @param sender the recipient of messages.
     */
    protected void showColoursAndStyles(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Llama colours: " +
                           ChatColor.YELLOW + Stream.of(Llama.Color.values()).map(Object::toString).sorted().collect(Collectors.joining(" ")));
        sender.sendMessage(ChatColor.GOLD + "Horse colours: " +
                           ChatColor.YELLOW + Stream.of(Horse.Color.values()).map(Object::toString).sorted().collect(Collectors.joining(" ")));
        sender.sendMessage(ChatColor.GOLD + "Horse styles: " +
                           ChatColor.YELLOW + Stream.of(Horse.Style.values()).map(Object::toString).sorted().collect(Collectors.joining(" ")));
    }
} // class HorseSetNameExecutor