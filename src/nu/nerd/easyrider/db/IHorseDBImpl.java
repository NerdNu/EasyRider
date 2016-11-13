package nu.nerd.easyrider.db;

import java.util.Collection;

// ----------------------------------------------------------------------------
/**
 * The underlying storage implementation for {@link HorseDB}.
 */
public interface IHorseDBImpl {
    // ------------------------------------------------------------------------
    /**
     * Return the type identifier of this implementation.
     *
     * @return the type identifier.
     */
    public String getType();

    // ------------------------------------------------------------------------
    /**
     * Back up the database if that is supported.
     */
    public void backup();

    // ------------------------------------------------------------------------
    /**
     * Save all horses in the collection to the database.
     *
     * Horses will be inserted if {@link SavedHorse.isNew()}; otherwise updated.
     *
     * @param collection the horses.
     */
    public void saveAll(Collection<SavedHorse> collection);

    // ------------------------------------------------------------------------
    /**
     * Load all horses as a collection.
     *
     * @return a Collection<SavedHorse> of the entire database's contents.
     */
    public Collection<SavedHorse> loadAll();

    // ------------------------------------------------------------------------
    /**
     * Delete all horses in the collection from the database.
     *
     * @param collection the horses.
     */
    public void delete(Collection<SavedHorse> collection);
} // interface IHorseDBImpl