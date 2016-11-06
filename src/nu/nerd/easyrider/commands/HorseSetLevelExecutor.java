package nu.nerd.easyrider.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

// --------------------------------------------------------------------------
/**
 * Executor for the /horse-set-level admin command.
 */
public class HorseSetLevelExecutor extends ExecutorBase {
    // --------------------------------------------------------------------------
    /**
     */
    public HorseSetLevelExecutor() {
        super("horse-set-level", "health", "jump", "speed", "help");
    }

    // --------------------------------------------------------------------------
    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender,
     *      org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender arg0, Command arg1, String arg2, String[] arg3) {
        // TODO Auto-generated method stub
        return false;
    }

}
// class HorseSetLevelExecutor