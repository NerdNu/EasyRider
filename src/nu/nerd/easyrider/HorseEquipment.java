package nu.nerd.easyrider;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LlamaInventory;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * Bit constants used to signify horse equipment in the equipment field of
 * {@link SavedHorse}.
 */
public class HorseEquipment {
    /**
     * Bit set to indicate presence of saddle.
     */
    public static final int SADDLE = 0x01;

    /**
     * Bit set to indicate presence of iron barding.
     */
    public static final int IRON_BARDING = 0x02;

    /**
     * Bit set to indicate presence of gold barding.
     */
    public static final int GOLD_BARDING = 0x04;

    /**
     * Bit set to indicate presence of diamond barding.
     */
    public static final int DIAMOND_BARDING = 0x08;

    /**
     * Bit set to indicate presence of chest barding.
     */
    public static final int CHEST = 0x10;

    /**
     * Bit set to indicate the presence of a llama blanket (carpet/decor).
     */
    public static final int BLANKET = 0x20;

    /**
     * Bit set corresponding to a 4-bit field used to encode a llama's decor
     * (blanket) colour.
     */
    public static final int BLANKET_COLOUR = 0x3C0;

    /**
     * Number of bits to shift the blanket bits down to get a blanket colour
     * number (0-15).
     */
    public static final int BLANKET_SHIFT = 6;

    /**
     * Bit set corresponding to all regular, vanilla equipment.
     */
    public static final int ALL_REGULAR = (SADDLE | IRON_BARDING | GOLD_BARDING | DIAMOND_BARDING | CHEST |
                                           BLANKET | BLANKET_COLOUR);

    // ------------------------------------------------------------------------
    /**
     * Return the equipment bits corresponding to the equipment on the specified
     * horse-like entity.
     *
     * @param abstractHorse the horse-like entity.
     * @return the equipment bits corresponding to the equipment on the
     *         specified horse-like entity.
     */
    public static int bits(AbstractHorse abstractHorse) {
        int equip = 0;
        if (abstractHorse instanceof ChestedHorse) {
            if (((ChestedHorse) abstractHorse).isCarryingChest()) {
                equip |= HorseEquipment.CHEST;
            }
        }

        if (abstractHorse instanceof Horse ||
            abstractHorse instanceof SkeletonHorse ||
            abstractHorse instanceof ZombieHorse) {
            HorseInventory inv = (HorseInventory) abstractHorse.getInventory();
            if (inv.getSaddle() != null && inv.getSaddle().getType() == Material.SADDLE) {
                equip |= HorseEquipment.SADDLE;
            }
            if (inv.getArmor() != null) {
                if (inv.getArmor().getType() == Material.IRON_BARDING) {
                    equip |= HorseEquipment.IRON_BARDING;
                } else if (inv.getArmor().getType() == Material.GOLD_BARDING) {
                    equip |= HorseEquipment.GOLD_BARDING;
                } else if (inv.getArmor().getType() == Material.DIAMOND_BARDING) {
                    equip |= HorseEquipment.DIAMOND_BARDING;
                }
            }
        }

        if (abstractHorse instanceof Llama) {
            Llama llama = (Llama) abstractHorse;
            LlamaInventory inv = llama.getInventory();
            ItemStack item = inv.getDecor();
            if (item != null && item.getType() == Material.CARPET) {
                equip |= HorseEquipment.BLANKET | ((item.getDurability() & 0xF) << BLANKET_SHIFT);
            }

        }
        return equip;
    }

    // ------------------------------------------------------------------------
    /**
     * Return a description of the equipment encoded in the specified bitset.
     * 
     * @param bits the set of equipment.
     * @return the description.
     */
    public static String description(int bits) {
        StringBuilder s = new StringBuilder();
        String sep = "";
        if ((bits & SADDLE) != 0) {
            s.append("saddle");
            sep = ", ";
        }
        if ((bits & IRON_BARDING) != 0) {
            s.append(sep).append("iron barding");
            sep = ", ";
        }
        if ((bits & GOLD_BARDING) != 0) {
            s.append(sep).append("gold barding");
            sep = ", ";
        }
        if ((bits & DIAMOND_BARDING) != 0) {
            s.append(sep).append("diamond barding");
            sep = ", ";
        }
        if ((bits & CHEST) != 0) {
            s.append(sep).append("chest");
            sep = ", ";
        }
        if ((bits & BLANKET) != 0) {
            int colourBits = (bits & BLANKET_COLOUR) >> BLANKET_SHIFT;
            @SuppressWarnings("deprecation")
            DyeColor colour = DyeColor.getByWoolData((byte) colourBits);
            s.append(colour.name().replace('_', ' ').toLowerCase()).append(" blanket");
            sep = ", ";
        }
        return s.toString();
    }
} // class HorseEquipment