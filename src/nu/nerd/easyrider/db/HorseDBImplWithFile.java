package nu.nerd.easyrider.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

import nu.nerd.easyrider.EasyRider;

// ----------------------------------------------------------------------------
/**
 * Abstract base of {@link IHorseDBImpl} implementations that store data in a
 * file that can be easily backed up.
 */
public abstract class HorseDBImplWithFile implements IHorseDBImpl {
    // --------------------------------------------------------------------------
    /**
     * Return the path to the file containing the database.
     *
     * @return the path to the file containing the database.
     */
    public abstract Path getDBFile();

    // --------------------------------------------------------------------------
    /**
     * @see nu.nerd.easyrider.db.IHorseDBImpl#backup()
     *
     *      Make a backup of the database file if a backup has not been made in
     *      the last hour.
     *
     *      The backup file will be called
     *      "backups/<database-file>.yyyy-MM-dd-HH".
     */
    @Override
    public void backup() {
        Path dataDir = null;
        try {
            dataDir = EasyRider.PLUGIN.getDataFolder().toPath();
        } catch (Exception ex) {
            EasyRider.PLUGIN.getLogger().severe("Could not get data folder: " +
                                                ex.getMessage());
            return;
        }

        Path backupsDir = null;
        try {
            backupsDir = dataDir.resolve("backups");
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxr-x");
            Files.createDirectories(backupsDir, PosixFilePermissions.asFileAttribute(perms));
        } catch (Exception ex) {
            EasyRider.PLUGIN.getLogger().severe("Could not create database backups directory: " +
                                                ex.getMessage());
            return;
        }

        Path databaseFile = getDBFile();
        try {
            String date = new SimpleDateFormat(".yyyy-MM-dd-HH").format(new Date());
            String baseName = databaseFile.getFileName().toString();
            Path backupFile = backupsDir.resolve(baseName + date);
            if (!Files.exists(backupFile)) {
                Files.copy(databaseFile, backupFile, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception ex) {
            EasyRider.PLUGIN.getLogger().severe("Error backing up database: " + ex.getMessage());
            return;
        }
    }
} // class HorseDBImplWithFile