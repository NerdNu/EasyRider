package nu.nerd.easyrider.db;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.PersistenceException;

import com.avaje.ebean.EbeanServer;

import nu.nerd.easyrider.EasyRider;

// --------------------------------------------------------------------------
/**
 * Storage of all horses using Bukkits Ebeans encapsulation of Sqlite.
 *
 * This code assumes that the server does not have a custom server-wide database
 * configured, so that each plugin has its own <plugin-name>.db file (the
 * default).
 */
public class HorseDBImplWithSqlite extends HorseDBImplWithFile {
    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#getType()
     */
    @Override
    public String getType() {
        return "sqlite";
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.HorseDBImplWithFile#getDBFile()
     */
    @Override
    public Path getDBFile() {
        return new File(EasyRider.PLUGIN.getDataFolder(), "EasyRider.db").toPath();
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#loadAll()
     */
    @Override
    public Collection<SavedHorse> loadAll() {
        ArrayList<SavedHorse> result = new ArrayList<SavedHorse>();
        try {
            getDatabase().find(SavedHorse.class).findRowCount();
            for (SavedHorse savedHorse : getDatabase().find(SavedHorse.class).findList()) {
                result.add(savedHorse);
            }
        } catch (PersistenceException ex) {
            EasyRider.PLUGIN.getLogger().info("First run, initialising database.");
            EasyRider.PLUGIN.installDDL();
        }
        return result;
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#saveAll(java.util.Collection)
     */
    @Override
    public void saveAll(Collection<SavedHorse> collection) {
        getDatabase().beginTransaction();
        try {
            for (SavedHorse horse : collection) {
                if (horse.isNew()) {
                    getDatabase().insert(horse);
                    horse.setClean();
                } else if (horse.isDirty()) {
                    getDatabase().update(horse);
                    horse.setClean();
                }
            }
            getDatabase().commitTransaction();
        } catch (Exception ex) {
            EasyRider.PLUGIN.getLogger().severe("Error saving horses: " + ex.getMessage());

        } finally {
            getDatabase().endTransaction();
        }
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#delete(java.util.Collection)
     */
    @Override
    public void delete(Collection<SavedHorse> collection) {
        getDatabase().beginTransaction();
        try {
            for (SavedHorse savedHorse : collection) {
                getDatabase().delete(savedHorse);
            }
            getDatabase().commitTransaction();
        } catch (Exception ex) {
            EasyRider.PLUGIN.getLogger().severe("Error removing horses: " + ex.getMessage());
        } finally {
            getDatabase().endTransaction();
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Return the Ebeans database.
     *
     * @return the Ebeans database.
     */
    protected EbeanServer getDatabase() {
        return EasyRider.PLUGIN.getDatabase();
    }

} // class HorseDBImplWithSqlite