package nu.nerd.easyrider;

import java.util.HashMap;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Donkey;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;
import org.bukkit.entity.Mule;
import org.bukkit.entity.SkeletonHorse;
import org.bukkit.entity.ZombieHorse;
import org.bukkit.inventory.HorseInventory;
import org.bukkit.inventory.Inventory;
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
     * Bit set to indicate presence of iron armour.
     */
    public static final int IRON_HORSE_ARMOUR = 0x02;

    /**
     * Bit set to indicate presence of gold armour.
     */
    public static final int GOLDEN_HORSE_ARMOUR = 0x04;

    /**
     * Bit set to indicate presence of diamond armour.
     */
    public static final int DIAMOND_HORSE_ARMOUR = 0x08;

    /**
     * Bit set to indicate presence of chest.
     */
    public static final int CHEST = 0x10;

    /**
     * Bit set to indicate the presence of a llama pack (carpet/decor).
     */
    public static final int PACK = 0x20;

    /**
     * Bit set corresponding to a 4-bit field used to encode a llama's decor
     * (pack) colour.
     */
    public static final int PACK_COLOUR = 0x3C0;

    /**
     * Number of bits to shift the pack bits down to get a pack colour number
     * (0-15).
     */
    public static final int PACK_SHIFT = 6;

    /**
     * Bit set corresponding to all regular, vanilla equipment.
     */
    public static final int ALL_REGULAR = (SADDLE | IRON_HORSE_ARMOUR | GOLDEN_HORSE_ARMOUR | DIAMOND_HORSE_ARMOUR |
                                           CHEST | PACK | PACK_COLOUR);

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

        if (abstractHorse instanceof Horse) {
            HorseInventory inv = (HorseInventory) abstractHorse.getInventory();
            if (inv.getSaddle() != null && inv.getSaddle().getType() == Material.SADDLE) {
                equip |= HorseEquipment.SADDLE;
            }
            if (inv.getArmor() != null) {
                if (inv.getArmor().getType() == Material.IRON_HORSE_ARMOR) {
                    equip |= HorseEquipment.IRON_HORSE_ARMOUR;
                } else if (inv.getArmor().getType() == Material.GOLDEN_HORSE_ARMOR) {
                    equip |= HorseEquipment.GOLDEN_HORSE_ARMOUR;
                } else if (inv.getArmor().getType() == Material.DIAMOND_HORSE_ARMOR) {
                    equip |= HorseEquipment.DIAMOND_HORSE_ARMOUR;
                }
            }
        } else if (abstractHorse instanceof Donkey ||
                   abstractHorse instanceof Mule) {
            Inventory inv = abstractHorse.getInventory();
            ItemStack saddleItem = inv.getItem(0);
            if (saddleItem != null && saddleItem.getType() == Material.SADDLE) {
                equip |= HorseEquipment.SADDLE;
            }
        } else if (abstractHorse instanceof SkeletonHorse ||
                   abstractHorse instanceof ZombieHorse) {
            Inventory inv = abstractHorse.getInventory();
            if (inv.contains(Material.SADDLE)) {
                equip |= HorseEquipment.SADDLE;
            }
        } else if (abstractHorse instanceof Llama) {
            Llama llama = (Llama) abstractHorse;
            LlamaInventory inv = llama.getInventory();
            ItemStack item = inv.getDecor();
            if (item != null) {
                DyeColor colour = CARPET_COLOURS.get(item.getType());
                if (colour != null) {
                    equip |= HorseEquipment.PACK | ((colour.ordinal() & 0xF) << PACK_SHIFT);
                }
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
        if ((bits & IRON_HORSE_ARMOUR) != 0) {
            s.append(sep).append("iron armour");
            sep = ", ";
        }
        if ((bits & GOLDEN_HORSE_ARMOUR) != 0) {
            s.append(sep).append("gold armour");
            sep = ", ";
        }
        if ((bits & DIAMOND_HORSE_ARMOUR) != 0) {
            s.append(sep).append("diamond armour");
            sep = ", ";
        }
        if ((bits & CHEST) != 0) {
            s.append(sep).append("chest");
            sep = ", ";
        }
        if ((bits & PACK) != 0) {
            int colourBits = (bits & PACK_COLOUR) >> PACK_SHIFT;
            DyeColor colour = DyeColor.values()[colourBits];
            s.append(sep).append(colour.name().replace('_', ' ').toLowerCase()).append(" pack");
            sep = ", ";
        }
        return s.toString();
    }

    // ------------------------------------------------------------------------
    /**
     * Map from 16 carpet materials to DyeColor enum.
     */
    private static final HashMap<Material, DyeColor> CARPET_COLOURS = new HashMap<>();
    static {
        for (DyeColor colour : DyeColor.values()) {
            CARPET_COLOURS.put(Material.valueOf(colour.name() + "_CARPET"), colour);
        }
    }
} // class HorseEquipment