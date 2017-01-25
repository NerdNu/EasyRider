package nu.nerd.easyrider;

import java.util.logging.Logger;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Horse;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * This class represents one of the attributes of a horse that can be trained.
 *
 * The effort, E, that must be expended to train an ability to an integer level,
 * L, is:
 * 
 * <pre>
 * E = K * (B ^ (L - 1) - 1)
 * </pre>
 * 
 * Where:
 * <ul>
 * <li>K = effort scale factor</li>
 * <li>B = effort exponential base</li>
 * </ul>
 *
 * Note that the effort to increment the level increases exponentially as the
 * level increases. However, as discussed below, the ability increases as a
 * linear function of the level. So, training gives diminishing returns as the
 * level increases.
 * 
 * The effort scale factor is chosen arbitrarily. The effort base can be
 * computed by substituting in the effort scale, K, the maximum effort, E_max,
 * and the corresponding maximum level, L_max:
 * 
 * <pre>
 * B = (1 + E_max / K) ^ (1 / (L_max - 1))
 * </pre>
 * 
 * Where the maximum level and corresponding maximum effort are carefully
 * selected.
 * 
 * The current level can be expressed as a function L(E) of the effort expended
 * in training, E, as:
 * 
 * <pre>
 * L(E) = min(L_max, floor(1 + ln(1 + E / K) / ln(B)))
 * </pre>
 * 
 * All of these equations were in fact derived from the initial starting
 * concept:
 * 
 * <pre>
 * L = 1 + log_B(1 + E / K)
 * </pre>
 * 
 * where log_B signifies "logarithm to the base B". The logarithm of 1 is 0, so
 * log_B(1 + E/K) will always be defined and greater than 0, increasing in
 * proportion to effort scaled by K. The lowest level is 1, hence the leading "1
 * +" term.
 * 
 * Note that the current level is presented to the user as an integer, though
 * the training effort leads to a notional real number for the level. Also the
 * level cannot be trained past the maximum, though admins can create horses
 * with ability levels above the maximum.
 * 
 * Attributes such as speed, health (hearts) and jump strength are linearly
 * interpolated according to the level, from 1 to the maximum level, and
 * quantised to the value corresponding to L(E), recalling that L(E) is always
 * rounded down to an integer.
 */
public abstract class Ability {
    // --------------------------------------------------------------------------
    /**
     * Constructor.
     * 
     * @param name the programmatic (single lower case word) name of the
     *        ability.
     * @param displayName the displayable name of the ability.
     * @param attribute the attribute to set to {@link #getValue(int)}.
     */
    public Ability(String name, String displayName, Attribute attribute) {
        _name = name;
        _displayName = displayName;
        _attribute = attribute;
    }

    // --------------------------------------------------------------------------
    /**
     * Load the parameters of this ability from the specified configuration
     * section.
     *
     * @param configurationSection the section.
     * @param logger the Logger to send error messages to.
     */
    public void load(ConfigurationSection section, Logger logger) {
        _maxLevel = section.getInt("max-level");
        if (_maxLevel < 1) {
            logger.severe("Loaded max " + getDisplayName() + " level: " + getMaxLevel());
        }
        _maxEffort = section.getDouble("max-effort");
        if (_maxEffort < 1.0) {
            logger.severe("Loaded max " + getDisplayName() + " effort: " + getMaxEffort());
        }
        _effortScale = section.getDouble("effort-scale");
        if (_effortScale < 0.0) {
            logger.severe("Loaded " + getDisplayName() + " effort scale: " + getEffortScale());
        }
        _minValue = section.getDouble("min-value");
        if (_minValue < 0.0) {
            logger.severe("Loaded " + getDisplayName() + " minimum value: " + getMinValue());
        }
        _maxValue = section.getDouble("max-value");
        if (_maxValue < 0.0) {
            logger.severe("Loaded " + getDisplayName() + " maximum value: " + getMinValue());
        }

        _effortBase = Math.pow(1 + getMaxEffort() / getEffortScale(),
                               1.0 / (getMaxLevel() - 1));
    }

    // ------------------------------------------------------------------------
    /**
     * Return the programmatic (single lower case word) name of this ability.
     * 
     * @return the programmatic (single lower case word) name of this ability.
     */
    public String getName() {
        return _name;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the displayable name of this ability.
     * 
     * @return the displayable name of this ability.
     */
    public String getDisplayName() {
        return _displayName;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Attribute that should be set to {@link #getValue(int)}.
     * 
     * @return the Attribute that should be set to {@link #getValue(int)}.
     */
    public Attribute getAttribute() {
        return _attribute;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum level attainable by expending effort.
     * 
     * @return the maximum level attainable by expending effort.
     */
    public int getMaxLevel() {
        return _maxLevel;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum amount of effort that will be counted towards
     * levelling up.
     * 
     * Once the expended effort exceeds this, the maximum level will have been
     * attained.
     * 
     * @return the maximum amount of effort that will be counted towards
     *         levelling up.
     */
    public double getMaxEffort() {
        return _maxEffort;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the scale factor that converts the effort base raised to the level
     * into required effort.
     * 
     * @return the scale factor that converts the effort base raised to the
     *         level into required effort.
     */
    public double getEffortScale() {
        return _effortScale;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the base of the exponential that converts level into required
     * effort.
     * 
     * @return the base of the exponential that converts level into required
     *         effort.
     */
    public double getEffortBase() {
        return _effortBase;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the minimum amount of effort required to attain the specified
     * level.
     *
     * @param level the fractional level.
     * @return the minimum amount of effort required to attain it.
     */
    public double getEffortForLevel(double level) {
        return _effortScale * (Math.pow(_effortBase, level - 1) - 1);
    }

    // ------------------------------------------------------------------------
    /**
     * Return the uncapped level corresponding to effort, including any
     * fractional completion.
     *
     * @param effort the effort.
     * @return the uncapped, unquantised level; the value will not be capped at
     *         the maximum level attainable through effort.
     */
    public double getLevelForEffort(double effort) {
        return 1 + Math.log(1 + effort / getEffortScale()) / Math.log(getEffortBase());
    }

    // ------------------------------------------------------------------------
    /**
     * Return the specified horse's level uncapped level in this Ability
     * corresponding to training effort, including any fractional completion.
     *
     * @param savedHorse the database state of the horse.
     * @return the uncapped, unquantised level; the value will not be capped at
     *         the maximum level attainable through effort.
     */
    public double getFractionalLevel(SavedHorse savedHorse) {
        return getLevelForEffort(getEffort(savedHorse));
    }

    // ------------------------------------------------------------------------
    /**
     * Return the quantised, integer level corresponding to the specified amount
     * of expended effort.
     * 
     * @param effort the effort.
     * @return the integer level attained, capped at the maximum level
     *         attainable through effort.
     */
    public int getQuantisedLevelForEffort(double effort) {
        return Math.min(getMaxLevel(), (int) getLevelForEffort(effort));
    }

    // ------------------------------------------------------------------------
    /**
     * Return the minimum ability value on the internal (Bukkit API) scale.
     * 
     * @return the minimum ability value.
     */
    public double getMinValue() {
        return _minValue;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the maximum ability value on the internal (Bukkit API) scale.
     * 
     * @return the maximum ability value.
     */
    public double getMaxValue() {
        return _maxValue;
    }

    // ------------------------------------------------------------------------
    /**
     * Return the internal attribute value corresponding to a specific level.
     * 
     * @param level the integer level.
     * @return the attribute value.
     */
    public double getValue(int level) {
        double frac = (level - 1) / (double) (_maxLevel - 1);
        return Util.linterp(_minValue, _maxValue, frac);
    }

    // ------------------------------------------------------------------------
    /**
     * Checks whether the current level should increase due to training and, if
     * so, increases the base Attribute accordingly.
     *
     * @param savedHorse the database state of the horse.
     * @param horse the Horse entity whose attribute will be set.
     * @return true if the training effort resulted in a change in level (and
     *         attributes) of the horse.
     */
    public boolean hasLevelIncreased(SavedHorse savedHorse, Horse horse) {
        int trainedLevel = getQuantisedLevelForEffort(getEffort(savedHorse));
        if (trainedLevel > getLevel(savedHorse)) {
            setLevel(savedHorse, trainedLevel);
            updateAttribute(savedHorse, horse);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * Set the attribute of the specified Horse entity to match the current
     * level of this horse in this Ability.
     *
     * @param savedHorse the database state of the horse.
     * @param horse the Horse entity whose attribute will be set.
     */
    public void updateAttribute(SavedHorse savedHorse, Horse horse) {
        setAttribute(horse, getLevel(savedHorse));
    }

    // ------------------------------------------------------------------------
    /**
     * Set the attribute of the specified Horse entity to the value appropriate
     * to the specified level of this Ability.
     *
     * @param horse the Horse entity whose attribute will be set.
     * @param level the level to set.
     */
    public void setAttribute(Horse horse, int level) {
        AttributeInstance horseAttribute = horse.getAttribute(getAttribute());
        horseAttribute.setBaseValue(getValue(level));
    }

    // ------------------------------------------------------------------------
    /**
     * Return the units of attribute values as a displayable String.
     *
     * @return the units of attribute values as a displayable String.
     */
    public abstract String getValueUnits();

    // ------------------------------------------------------------------------
    /**
     * Return the units of effort as a displayable String.
     *
     * @return the units of effort as a displayable String.
     */
    public abstract String getEffortUnits();

    // ------------------------------------------------------------------------
    /**
     * Convert an Attribute value the to a number suitable for display
     *
     * This method exists because the conversion from internal values to
     * displayed values cannot always be implemented trivially (as just a scale
     * factor).
     *
     * @param value the value of the horse's Attribute in this ablility.
     * @return the displayable numeric value of the ability.
     */
    public abstract double toDisplayValue(double value);

    // ------------------------------------------------------------------------
    /**
     * Convert a displayable value to the corresponding Attribute value of a
     * horse.
     *
     * @param displayValue the displayable value of the horse's ablility.
     * @return the corresponding Attribute value of the ability.
     * @throws IllegalStateException if not implemented.
     */
    public abstract double toAttributeValue(double displayValue);

    // ------------------------------------------------------------------------
    /**
     * Return the formatted display value corresponding to this ability on a
     * SavedHorse.
     * 
     * @param savedHorse the database state of the horse.
     * @return the formatted display value.
     */
    public abstract String getFormattedValue(SavedHorse savedHorse);

    // ------------------------------------------------------------------------
    /**
     * Return the formatted effort expended on this ability.
     *
     * @param savedHorse the database state of the horse.
     * @return effort for this ability expended on the specified horse.
     */
    public abstract String getFormattedEffort(SavedHorse savedHorse);

    // ------------------------------------------------------------------------
    /**
     * Set the stored level of the SavedHorse in this Ability.
     * 
     * Horse attributes are not affected.
     *
     * @param savedHorse the database state of the horse.
     * @param level the level.
     */
    public abstract void setLevel(SavedHorse savedHorse, int level);

    // ------------------------------------------------------------------------
    /**
     * Return the level corresponding to this Ability on a SavedHorse.
     *
     * @param savedHorse the database state of the horse.
     * @return the level.
     */
    public abstract int getLevel(SavedHorse savedHorse);

    // ------------------------------------------------------------------------
    /**
     * Set the stored training effort in this ability.
     *
     * The level and corresponding horse attribute are not affected.
     *
     * @param savedHorse the database state of the horse.
     * @param level the level.
     */
    public abstract void setEffort(SavedHorse savedHorse, double effort);

    // ------------------------------------------------------------------------
    /**
     * Return the stored training effort in this ability.
     *
     * @param savedHorse the database state of the horse.
     * @return the stored training effort in this ability.
     */
    public abstract double getEffort(SavedHorse savedHorse);

    // ------------------------------------------------------------------------
    /**
     * The programmatic (single lower case word) name of this ability.
     */
    protected String _name;

    /**
     * The displayable name of this ability.
     */
    protected String _displayName;

    /**
     * The Attribute that should be set to {@link #getValue(int)}.
     */
    protected Attribute _attribute;

    /**
     * The maximum level attainable by expending effort.
     */
    protected int _maxLevel;

    /**
     * The maximum amount of effort that will be counted towards levelling up.
     */
    protected double _maxEffort;

    /**
     * The scale factor that converts the effort base raised to the level into
     * required effort.
     */
    protected double _effortScale;

    /**
     * The base of the exponential that converts level into required effort.
     */
    protected double _effortBase;

    /**
     * The minimum ability value on the internal (Bukkit API) scale.
     */
    protected double _minValue;

    /**
     * The maximum ability value on the internal (Bukkit API) scale.
     */
    protected double _maxValue;

} // class Ability