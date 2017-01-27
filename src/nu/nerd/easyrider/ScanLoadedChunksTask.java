package nu.nerd.easyrider;

import java.util.function.BooleanSupplier;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;

import nu.nerd.easyrider.db.SavedHorse;

// ----------------------------------------------------------------------------
/**
 * A task that scans all horses in all currently loaded chunks in a specified
 * world.
 */
public class ScanLoadedChunksTask implements BooleanSupplier {
    // ------------------------------------------------------------------------

    public ScanLoadedChunksTask(World world) {
        _world = world;
    }

    // ------------------------------------------------------------------------
    /**
     * @see java.util.function.BooleanSupplier#getAsBoolean()
     */
    @Override
    public boolean getAsBoolean() {
        long start = System.nanoTime();
        long elapsed;
        if (_chunks == null) {
            _chunks = _world.getLoadedChunks();
            if (EasyRider.CONFIG.DEBUG_SCANS) {
                elapsed = System.nanoTime() - start;
                EasyRider.PLUGIN.getLogger().info("Get " + _world.getName() +
                                                  " loaded chunks: " + elapsed * 0.001 + " microseconds.");
            }
            return true;
        }

        int startIndex = _index;
        while (_index < _chunks.length) {
            Chunk chunk = _chunks[_index++];
            if (chunk.isLoaded()) {
                for (Entity entity : chunk.getEntities()) {
                    if (entity instanceof Horse) {
                        Horse horse = (Horse) entity;
                        SavedHorse savedHorse = EasyRider.DB.findHorse(horse);
                        if (savedHorse != null) {
                            EasyRider.DB.observe(savedHorse, horse);
                            if (savedHorse.isAbandoned()) {
                                EasyRider.DB.freeHorse(savedHorse, horse);
                            }
                        }
                    }
                }
            }

            elapsed = System.nanoTime() - start;
            if (elapsed > EasyRider.CONFIG.SCAN_TIME_LIMIT_MICROS * 1000) {
                if (EasyRider.CONFIG.DEBUG_SCANS) {
                    EasyRider.PLUGIN.getLogger().info("Processed " + (_index - startIndex) +
                                                      " chunks in " + elapsed * 0.001 + " microseconds.");
                }
                return true;
            }
        }

        if (EasyRider.CONFIG.DEBUG_SCANS) {
            EasyRider.PLUGIN.getLogger().info("Scan of " + _world.getName() + " complete.");
        }
        return false;
    }

    // ------------------------------------------------------------------------
    /**
     * The World whose chunks are scanned.
     */
    protected World _world;

    /**
     * Array of loaded chunks at the start of the scan.
     */
    protected Chunk[] _chunks;

    /**
     * Index of next chunk to process in _chunks.
     */
    protected int _index;
} // class ScanLoadedChunksTask