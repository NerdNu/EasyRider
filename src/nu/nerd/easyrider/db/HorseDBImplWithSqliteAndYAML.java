package nu.nerd.easyrider.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import nu.nerd.easyrider.EasyRider;

// ----------------------------------------------------------------------------
/**
 * A database implementation that duplicates its contents to both Sqlite and
 * YAML.
 *
 * Where the two disagree, the contents of the YAML file are considered
 * definitive.
 */
public class HorseDBImplWithSqliteAndYAML implements IHorseDBImpl {
    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#getType()
     */
    @Override
    public String getType() {
        return "sqlite+yaml";
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#backup()
     */
    @Override
    public void backup() {
        _sqlite.backup();
        _yaml.backup();
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#saveAll(java.util.Collection)
     */
    @Override
    public void saveAll(Collection<SavedHorse> collection) {
        _sqlite.saveAll(collection);
        _yaml.saveAll(collection);
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#loadAll()
     */
    @Override
    public Collection<SavedHorse> loadAll() {
        Collection<SavedHorse> fromSqlite = _sqlite.loadAll();
        Collection<SavedHorse> fromYAML = _yaml.loadAll();

        HashMap<UUID, SavedHorse> sqliteMap = new HashMap<UUID, SavedHorse>();
        for (SavedHorse savedHorse : fromSqlite) {
            if (savedHorse.getUuid() == null) {
                EasyRider.PLUGIN.getLogger().severe("Got a horse with a null UUID from Sqlite.");
            }
            sqliteMap.put(savedHorse.getUuid(), savedHorse);
        }

        // Remove all horses found in fromYAML from sqliteMap, looking for
        // extras.
        boolean same = true;
        for (SavedHorse savedHorse : fromYAML) {
            UUID key = savedHorse.getUuid();
            if (sqliteMap.containsKey(key)) {
                sqliteMap.remove(key);
            } else {
                same = false;
                EasyRider.PLUGIN.getLogger().severe("Horse with UUID: " + key.toString() + " did not load from Sqlite.");
            }
        }

        // Check for extra horses that were in Sqlite but not YAML.
        if (!sqliteMap.isEmpty()) {
            same = false;
            for (SavedHorse savedHorse : sqliteMap.values()) {
                EasyRider.PLUGIN.getLogger().severe("Horse with UUID: " + savedHorse.getUuid().toString() + " did not load from YAML.");
            }
        }
        return same ? fromSqlite : fromYAML;
    }

    // ------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#delete(java.util.Collection)
     */
    @Override
    public void delete(Collection<SavedHorse> collection) {
        _sqlite.delete(collection);
        _yaml.delete(collection);
    }

    // ------------------------------------------------------------------------
    /**
     * Sqlite implemnentation.
     */
    IHorseDBImpl _sqlite = new HorseDBImplWithSqlite();

    /**
     * Sqlite implemnentation.
     */
    IHorseDBImpl _yaml = new HorseDBImplWithYAML();

} // class HorseDBImplWithSqliteAndYAML