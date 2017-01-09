package nu.nerd.easyrider;

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
     * Bit set corresponding to all regular, vanilla equipment.
     */
    public static final int ALL_REGULAR = (SADDLE | IRON_BARDING | GOLD_BARDING | DIAMOND_BARDING | CHEST);

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
        return s.toString();
    }
} // class HorseEquipment