package nu.nerd.easyrider;

import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * A task that runs immediately after a player has clicked on a horse with a
 * golden food item, in order to detect if the horse consumed the food.
 *
 * There is no way to detect that consumption using the Bukkit API, so instead
 * we take a snapshot of the golden food ItemStack during the player's
 * interaction with the horse, and compare that to what is in the same inventory
 * slot at the next task run time.
 */
public final class GoldConsumerTask implements Runnable {
    // ------------------------------------------------------------------------
    /**
     * Capture a clone of the held food item before Mojang code decrements the
     * amount.
     *
     * @param player the player.
     * @parma horse the horse.
     * @param beforeFoodItem the consumed golden food item before the amount is
     *        changed.
     * @param nuggetValue the computed value of one food item in gold nuggets.
     * @param heldItemSlot the players main hand item slot.
     */
    public GoldConsumerTask(Player player, AbstractHorse horse, ItemStack beforeFoodItem,
                            int nuggetValue, int heldItemSlot) {
        _player = player;
        _horse = horse;
        _beforeFoodItem = beforeFoodItem.clone();
        _nuggetValue = nuggetValue;
        _heldItemSlot = heldItemSlot;
    }

    // ------------------------------------------------------------------------
    /**
     * @see java.lang.Runnable#run()
     *
     *      Check the same item slot is selected and that the item stack is
     *      diminished by one.
     */
    @Override
    public void run() {
        // Check whether the player logged.
        if (EasyRider.PLUGIN.getState(_player) == null) {
            return;
        }

        // Check that the horse is alive.
        SavedHorse savedHorse = EasyRider.DB.findHorse(_horse);
        if (savedHorse == null) {
            return;
        }

        if (_player.getInventory().getHeldItemSlot() == _heldItemSlot) {
            ItemStack afterFoodItem = _player.getInventory().getItem(_heldItemSlot);
            if ((afterFoodItem == null && _beforeFoodItem.getAmount() == 1)
                ||
                (afterFoodItem.getType() == _beforeFoodItem.getType() &&
                 afterFoodItem.getDurability() == _beforeFoodItem.getDurability() &&
                 afterFoodItem.getAmount() == _beforeFoodItem.getAmount() - 1)) {

                EasyRider.PLUGIN.consumeGoldenFood(savedHorse, _horse, _nuggetValue, _player);
            }
        }
    }

    // ------------------------------------------------------------------------
    /**
     * The player.
     */
    private final Player _player;

    /**
     * The horse.
     */
    private final AbstractHorse _horse;

    /**
     * The food item that was clicked on the horse.
     */
    private final ItemStack _beforeFoodItem;

    /**
     * The computed value of one food item in gold nuggets.
     */
    private final int _nuggetValue;

    /**
     * The player's held item slot at the time of the interaction.
     */
    private final int _heldItemSlot;
} // class GoldConsumerTask