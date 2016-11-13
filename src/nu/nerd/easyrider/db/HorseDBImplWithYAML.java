package nu.nerd.easyrider.db;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;

import org.bukkit.configuration.file.YamlConfiguration;

import nu.nerd.easyrider.EasyRider;

// --------------------------------------------------------------------------
/**
 * Storage of all horses in a YAML file.
 */
public class HorseDBImplWithYAML extends HorseDBImplWithFile {
    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#getType()
     */
    @Override
    public String getType() {
        return "yaml";
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.HorseDBImplWithFile#getDBFile()
     */
    @Override
    public Path getDBFile() {
        return new File(EasyRider.PLUGIN.getDataFolder(), "horses.yml").toPath();
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#loadAll()
     */
    @Override
    public Collection<SavedHorse> loadAll() {
        ArrayList<SavedHorse> result = new ArrayList<SavedHorse>();
        _config = YamlConfiguration.loadConfiguration(getDBFile().toFile());
        for (String uuid : _config.getKeys(false)) {
            SavedHorse savedHorse = new SavedHorse();
            savedHorse.load(_config.getConfigurationSection(uuid));
            result.add(savedHorse);
        }
        return result;
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#saveAll(java.util.Collection)
     */
    @Override
    public void saveAll(Collection<SavedHorse> collection) {
        for (SavedHorse savedHorse : collection) {
            savedHorse.save(_config);
        }
        writeToDisk();
    }

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#delete(java.util.Collection)
     */
    @Override
    public void delete(Collection<SavedHorse> collection) {
        for (SavedHorse savedHorse : collection) {
            _config.set(savedHorse.getUuid().toString(), null);
        }
        writeToDisk();
    }

    // --------------------------------------------------------------------------
    /**
     * Write the YAML file to disk.
     */
    protected void writeToDisk() {
        try {
            _config.save(getDBFile().toFile());
        } catch (IOException ex) {
            EasyRider.PLUGIN.getLogger().severe("Unable to write YAML database: " + getDBFile().toString());
        }
    }

    // --------------------------------------------------------------------------
    /**
     * The YAML file containing the horses.
     */
    protected YamlConfiguration _config = new YamlConfiguration();

} // class HorseDBImplWithYAML