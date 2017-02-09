package nu.nerd.easyrider.db;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        Logger logger = EasyRider.PLUGIN.getLogger();
        Path dataDir = null;
        try {
            dataDir = EasyRider.PLUGIN.getDataFolder().toPath();
        } catch (Exception ex) {
            logger.severe("Could not get data folder: " + ex.getMessage());
            return;
        }

        Path backupsDir = null;
        try {
            backupsDir = dataDir.resolve("backups");
            Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxr-x");
            Files.createDirectories(backupsDir, PosixFilePermissions.asFileAttribute(perms));
        } catch (Exception ex) {
            logger.severe("Could not create database backups directory: " +
                          ex.getMessage());
            return;
        }

        Path databaseFile = getDBFile();
        String baseName = databaseFile.getFileName().toString();
        Calendar date = Calendar.getInstance();
        try {
            String formattedDate = new SimpleDateFormat(".yyyy-MM-dd-HH").format(date.getTime());
            Path backupFile = backupsDir.resolve(baseName + formattedDate);
            if (!Files.exists(backupFile)) {
                Files.copy(databaseFile, backupFile, StandardCopyOption.COPY_ATTRIBUTES);
            }
        } catch (Exception ex) {
            logger.severe("Error backing up database: " + ex.getMessage());
            return;
        }

        // Group backup files into one set per day based on filename.
        final int datedFilenameLength = baseName.length() + 11;
        TreeMap<String, TreeSet<File>> backupFilesByDate = new TreeMap<String, TreeSet<File>>();
        Pattern p = Pattern.compile(baseName + "\\.\\d{4}-\\d{2}-\\d{2}-\\d{2}");
        for (File file : backupsDir.toFile().listFiles()) {
            String fileName = file.getName();
            Matcher m = p.matcher(fileName);
            if (m.matches()) {
                String fileNameDatePrefix = fileName.substring(0, datedFilenameLength);
                TreeSet<File> filesForDate = backupFilesByDate.getOrDefault(fileNameDatePrefix, new TreeSet<File>());
                filesForDate.add(file);
                backupFilesByDate.put(fileNameDatePrefix, filesForDate);
            }
        }

        // Exclude the last 7 days of backups from culling (including today).
        SimpleDateFormat dateFormatter = new SimpleDateFormat(".yyyy-MM-dd");
        for (int i = 0; i < 7; ++i) {
            String preservedFileBase = baseName + dateFormatter.format(date.getTime());
            if (EasyRider.CONFIG.DEBUG_PURGES) {
                TreeSet<File> preservedFiles = backupFilesByDate.get(preservedFileBase);
                int preservedFileCount = preservedFiles != null ? preservedFiles.size() : 0;
                logger.info("Preserving all backups beginning with " +
                            preservedFileBase + ": " + preservedFileCount);
            }
            backupFilesByDate.remove(preservedFileBase);
            date.add(Calendar.DAY_OF_YEAR, -1);
        }

        // Preserve the first file in the set for each day; delete the rest.
        for (Entry<String, TreeSet<File>> entry : backupFilesByDate.entrySet()) {
            TreeSet<File> filesForDay = entry.getValue();
            if (EasyRider.CONFIG.DEBUG_PURGES) {
                logger.info("Keeping old backup : " + filesForDay.first().getName());
            }
            filesForDay.remove(filesForDay.first());

            for (File purgedFile : filesForDay) {
                if (EasyRider.CONFIG.DEBUG_PURGES) {
                    logger.info("Removing old backup: " + purgedFile.getName());
                }
                purgedFile.delete();
            }
        }
    }
} // class HorseDBImplWithFile