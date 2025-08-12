package net.runelite.client.plugins.microbot.util.cache.util;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.RuneLite;
import net.runelite.client.plugins.microbot.util.cache.Rs2GroundItemCache;
import net.runelite.client.plugins.microbot.util.cache.Rs2NpcCache;
import net.runelite.client.plugins.microbot.util.cache.Rs2ObjectCache;
import net.runelite.client.plugins.microbot.util.cache.Rs2QuestCache;
import net.runelite.client.plugins.microbot.util.cache.Rs2SkillCache;
import net.runelite.client.plugins.microbot.util.cache.Rs2VarPlayerCache;
import net.runelite.client.plugins.microbot.util.cache.Rs2VarbitCache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for logging cache states and dumping to files.
 * Provides reflection-based utilities for VarbitID and VarPlayerID name resolution.
 * 
 * @author Vox
 * @version 1.0
 */
@Slf4j
public class Rs2CacheLoggingUtils {
    
    private static final String CACHE_LOG_FOLDER = "cache";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    // Cache for VarbitID field mappings to avoid repeated reflection
    private static final Map<Integer, String> varbitIdCache = new ConcurrentHashMap<>();
    private static final Map<Integer, String> varPlayerIdCache = new ConcurrentHashMap<>();
    private static boolean varbitCacheInitialized = false;
    private static boolean varPlayerCacheInitialized = false;
    
    /**
     * Gets the RuneLite user directory for cache logs.
     * 
     * @return Path to the cache log directory
     */
    public static Path getCacheLogDirectory() {
        Path runeliteDir = RuneLite.RUNELITE_DIR.toPath();
        Path microbotPluginsDir = runeliteDir.resolve("microbot-plugins");
        Path cacheDir = microbotPluginsDir.resolve(CACHE_LOG_FOLDER);
        
        try {
            Files.createDirectories(cacheDir);
        } catch (IOException e) {
            log.warn("Failed to create cache log directory: {}", cacheDir, e);
            // Fall back to a temp directory
            return Paths.get(System.getProperty("java.io.tmpdir"), "microbot-cache");
        }
        
        return cacheDir;
    }
    
    /**
     * Gets a timestamp-based filename for cache dumps.
     * 
     * @param cacheType The cache type (e.g., "npc", "object")
     * @return Filename with timestamp
     */
    public static String getCacheLogFilename(String cacheType) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return String.format("%s_cache_%s.log", cacheType, timestamp);
    }
    
    /**
     * Writes content to a cache log file.
     * 
     * @param cacheType The cache type identifier
     * @param content The content to write
     * @param includeTimestamp Whether to include a timestamp in the filename
     */
    public static void writeCacheLogFile(String cacheType, String content, boolean includeTimestamp) {
        try {
            Path logDir = getCacheLogDirectory().resolve(cacheType);
            Files.createDirectories(logDir);
            String filename = includeTimestamp ? getCacheLogFilename(cacheType) : cacheType + "_cache.log";
            Path logFile = logDir.resolve(filename);
            
            try (BufferedWriter writer = Files.newBufferedWriter(logFile)) {
                writer.write(content);
                writer.flush();
            }
            
            log.info("Cache log written to: {}", logFile.toAbsolutePath());
            
        } catch (IOException e) {
            log.error("Failed to write cache log file for {}: {}", cacheType, e.getMessage(), e);
        }
    }
    
    /**
     * Gets the field name for a VarbitID value using reflection.
     * 
     * @param varbitId The varbit ID value
     * @return The field name if found, or the ID as string if not found
     */
    public static String getVarbitFieldName(int varbitId) {
        initializeVarbitCache();
        return varbitIdCache.getOrDefault(varbitId, String.valueOf(varbitId));
    }
    
    /**
     * Gets the field name for a VarPlayerID value using reflection.
     * 
     * @param varPlayerId The var player ID value
     * @return The field name if found, or the ID as string if not found
     */
    public static String getVarPlayerFieldName(int varPlayerId) {
        initializeVarPlayerCache();
        return varPlayerIdCache.getOrDefault(varPlayerId, String.valueOf(varPlayerId));
    }
    
    /**
     * Initializes the VarbitID cache using reflection.
     */
    private static synchronized void initializeVarbitCache() {
        if (varbitCacheInitialized) {
            return;
        }
        
        try {
            Field[] fields = VarbitID.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == int.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        int value = field.getInt(null);
                        varbitIdCache.put(value, field.getName());
                    } catch (IllegalAccessException e) {
                        log.debug("Could not access VarbitID field: {}", field.getName());
                    }
                }
            }
            varbitCacheInitialized = true;
            log.debug("Initialized VarbitID cache with {} entries", varbitIdCache.size());
            
        } catch (Exception e) {
            log.warn("Failed to initialize VarbitID cache via reflection", e);
            varbitCacheInitialized = true; // Prevent retries
        }
    }
    
    /**
     * Initializes the VarPlayerID cache using reflection.
     */
    private static synchronized void initializeVarPlayerCache() {
        if (varPlayerCacheInitialized) {
            return;
        }
        
        try {
            Field[] fields = VarPlayerID.class.getDeclaredFields();
            for (Field field : fields) {
                if (field.getType() == int.class && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        int value = field.getInt(null);
                        varPlayerIdCache.put(value, field.getName());
                    } catch (IllegalAccessException e) {
                        log.debug("Could not access VarPlayerID field: {}", field.getName());
                    }
                }
            }
            varPlayerCacheInitialized = true;
            log.debug("Initialized VarPlayerID cache with {} entries", varPlayerIdCache.size());
            
        } catch (Exception e) {
            log.warn("Failed to initialize VarPlayerID cache via reflection", e);
            varPlayerCacheInitialized = true; // Prevent retries
        }
    }

    /**
     * Formats a table header with specified column headers and widths.
     * 
     * @param headers Array of header strings
     * @param columnWidths Array of column widths
     * @return Formatted table header
     */
    public static String formatTableHeader(String[] headers, int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append("╔");
        for (int i = 0; i < columnWidths.length; i++) {
            for (int j = 0; j < columnWidths[i]; j++) {
                sb.append("═");
            }
            if (i < columnWidths.length - 1) {
                sb.append("╦");
            }
        }
        sb.append("╗\n");
        
        sb.append("║");
        for (int i = 0; i < headers.length; i++) {
            String header = truncate(headers[i], columnWidths[i] - 2);
            sb.append(String.format(" %-" + (columnWidths[i] - 2) + "s ", header));
            if (i < headers.length - 1) {
                sb.append("║");
            }
        }
        sb.append("║\n");
        
        sb.append("╠");
        for (int i = 0; i < columnWidths.length; i++) {
            for (int j = 0; j < columnWidths[i]; j++) {
                sb.append("═");
            }
            if (i < columnWidths.length - 1) {
                sb.append("╬");
            }
        }
        sb.append("╣\n");
        
        return sb.toString();
    }

    /**
     * Formats a table row with specified values and column widths.
     * 
     * @param values Array of cell values
     * @param columnWidths Array of column widths
     * @return Formatted table row
     */
    public static String formatTableRow(String[] values, int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append("║");
        for (int i = 0; i < values.length; i++) {
            String value = truncate(values[i], columnWidths[i] - 2);
            sb.append(String.format(" %-" + (columnWidths[i] - 2) + "s ", value));
            if (i < values.length - 1) {
                sb.append("║");
            }
        }
        sb.append("║\n");
        return sb.toString();
    }

    /**
     * Formats a table footer with specified column widths.
     * 
     * @param columnWidths Array of column widths
     * @return Formatted table footer
     */
    public static String formatTableFooter(int[] columnWidths) {
        StringBuilder sb = new StringBuilder();
        sb.append("╚");
        for (int i = 0; i < columnWidths.length; i++) {
            for (int j = 0; j < columnWidths[i]; j++) {
                sb.append("═");
            }
            if (i < columnWidths.length - 1) {
                sb.append("╩");
            }
        }
        sb.append("╝\n");
        return sb.toString();
    }

    /**
     * Truncates a string to the specified maximum length.
     * 
     * @param str The string to truncate
     * @param maxLength Maximum length
     * @return Truncated string
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Safely converts an object to string, handling null values.
     * 
     * @param obj The object to convert
     * @return String representation or "null" if object is null
     */
    public static String safeToString(Object obj) {
        return obj != null ? obj.toString() : "null";
    }

    /**
     * Formats a cache header with type, size and mode information.
     * 
     * @param cacheType The cache type name
     * @param cacheSize The number of entries in the cache
     * @param cacheMode The cache mode
     * @return Formatted header string
     */
    public static String formatCacheHeader(String cacheType, int cacheSize, String cacheMode) {
        return String.format("=== %s Cache State (%d entries, %s mode) ===", 
            cacheType, cacheSize, cacheMode);
    }

    /**
     * Formats a WorldPoint location for display.
     * 
     * @param location The WorldPoint to format
     * @return Formatted location string
     */
    public static String formatLocation(net.runelite.api.coords.WorldPoint location) {
        if (location == null) {
            return "N/A";
        }
        return String.format("(%d,%d,%d)", location.getX(), location.getY(), location.getPlane());
    }

    /**
     * Formats a timestamp in milliseconds to a readable date/time string.
     * 
     * @param timestampMillis Timestamp in milliseconds
     * @return Formatted timestamp string
     */
    public static String formatTimestamp(long timestampMillis) {
        if (timestampMillis <= 0) {
            return "N/A";
        }
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestampMillis / 1000, 0, java.time.ZoneOffset.UTC);
        return dateTime.format(TIMESTAMP_FORMAT);
    }

    /**
     * Formats cache statistics for display.
     * 
     * @param hitRate Cache hit rate as a percentage
     * @param hits Number of cache hits
     * @param misses Number of cache misses
     * @param mode Cache mode
     * @return Formatted statistics string
     */
    public static String formatCacheStatistics(double hitRate, long hits, long misses, String mode) {
        return String.format("Cache Statistics: %.1f%% hit rate (%d hits, %d misses) - Mode: %s", 
            hitRate, hits, misses, mode);
    }

    /**
     * Formats a limit message when cache content is truncated.
     * 
     * @param totalSize The total number of items
     * @param displayedSize The number of items displayed
     * @return Formatted limit message or empty string if no truncation
     */
    public static String formatLimitMessage(int totalSize, int displayedSize) {
        if (totalSize > displayedSize) {
            return String.format("... and %d more entries (showing first %d)", 
                totalSize - displayedSize, displayedSize);
        }
        return "";
    }

    /**
     * Outputs cache log content based on the specified output mode.
     * 
     * @param cacheType The cache type identifier
     * @param content The content to output
     * @param outputMode Where to direct the output
     */
    public static void outputCacheLog(String cacheType, String content, LogOutputMode outputMode) {
        switch (outputMode) {
            case CONSOLE_ONLY:
                log.info(content);
                break;
            case FILE_ONLY:
                writeCacheLogFile(cacheType, content, false);
                break;
            case BOTH:
                log.info(content);
                writeCacheLogFile(cacheType, content, false);
                break;
        }
    }

     /**
     * Logs all cache states to files only.
     * This is useful for generating comprehensive cache dumps for analysis.
     * Uses the existing boolean-based logState methods with dumpToFile=true.
     */
    public static void logAllCachesToFiles() {
        log.info("Starting cache file dump for all Rs2Cache instances...");
        
        try {
            // NPC Cache - uses LogOutputMode.FILE_ONLY
            if (Rs2NpcCache.getInstance() != null) {
                Rs2NpcCache.logState(LogOutputMode.FILE_ONLY);
            }
            
            // Object Cache - uses boolean method
            if (Rs2ObjectCache.getInstance() != null) {
                try {
                    Rs2ObjectCache.logState(LogOutputMode.FILE_ONLY);
                } catch (Exception e) {
                    log.error("Failed to log Object Cache state: {}", e.getMessage(), e);
                }
                
            }
            
            // Ground Item Cache - uses boolean method
            if (Rs2GroundItemCache.getInstance() != null) {
                Rs2GroundItemCache.logState(LogOutputMode.FILE_ONLY);
            }
            
            // Skill Cache - uses boolean method
            if (Rs2SkillCache.getInstance() != null) {
                Rs2SkillCache.logState(LogOutputMode.FILE_ONLY);
            }
            
            // Varbit Cache - uses boolean method
            if (Rs2VarbitCache.getInstance() != null) {
                Rs2VarbitCache.logState(LogOutputMode.FILE_ONLY);
            }
            
            // VarPlayer Cache - uses boolean method
            if (Rs2VarPlayerCache.getInstance() != null) {
                Rs2VarPlayerCache.logState(LogOutputMode.FILE_ONLY);
            }
            
            // Quest Cache - uses boolean method
            if (Rs2QuestCache.getInstance() != null) {
                Rs2QuestCache.logState(LogOutputMode.FILE_ONLY);
            }
            
            log.info("Cache file dump completed successfully. Files written to cache log directory.");
            
        } catch (Exception e) {
            log.error("Error during cache file dump: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Logs all cache states to both console and files.
     * This provides comprehensive output for debugging sessions.
     */
    public static void logAllCachesToConsoleAndFiles() {
        log.info("Starting comprehensive cache dump (console + files)...");
        
        try {
            // NPC Cache - uses LogOutputMode.BOTH
            if (Rs2NpcCache.getInstance() != null) {
                Rs2NpcCache.logState(LogOutputMode.BOTH);
            }
            
            // Object Cache - uses boolean method (console + file)
            if (Rs2ObjectCache.getInstance() != null) {
                Rs2ObjectCache.logState(LogOutputMode.BOTH);
            }
            
            // Ground Item Cache - uses boolean method (console + file)
            if (Rs2GroundItemCache.getInstance() != null) {
                Rs2GroundItemCache.logState(LogOutputMode.BOTH);
            }
            
            // Skill Cache - uses boolean method (console + file)
            if (Rs2SkillCache.getInstance() != null) {
                Rs2SkillCache.logState(LogOutputMode.BOTH);
            }
            
            // Varbit Cache - uses boolean method (console + file)
            if (Rs2VarbitCache.getInstance() != null) {
                Rs2VarbitCache.logState(LogOutputMode.BOTH);
            }
            
            // VarPlayer Cache - uses boolean method (console + file)
            if (Rs2VarPlayerCache.getInstance() != null) {
                Rs2VarPlayerCache.logState(LogOutputMode.BOTH);
            }
            
            // Quest Cache - uses boolean method (console + file)
            if (Rs2QuestCache.getInstance() != null) {
                Rs2QuestCache.logState(LogOutputMode.BOTH);
            }
            
            log.info("Comprehensive cache dump completed successfully.");
            
        } catch (Exception e) {
            log.error("Error during comprehensive cache dump: {}", e.getMessage(), e);
        }
    }
}
