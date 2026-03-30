package org.rooms.roombay.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemHealthService implements HealthIndicator {
    
    private final DataSource dataSource;
    private static final Instant START_TIME = Instant.now();
    
    @Override
    public Health health() {
        try {
            // Check database connection
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(2)) {
                    return Health.up()
                        .withDetail("database", "Connected")
                        .build();
                }
            }
            return Health.down()
                .withDetail("database", "Connection failed")
                .build();
        } catch (Exception e) {
            log.error("Health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
    
    /**
     * Get comprehensive system health metrics
     */
    public Map<String, Object> getSystemHealth() {
        log.info("Fetching system health metrics");
        
        Map<String, Object> health = new HashMap<>();
        
        // Application uptime
        Duration uptime = Duration.between(START_TIME, Instant.now());
        health.put("uptime", formatUptime(uptime));
        health.put("uptimeSeconds", uptime.getSeconds());
        health.put("startTime", START_TIME.toString());
        
        // JVM Memory
        health.put("memory", getMemoryMetrics());
        
        // System resources
        health.put("system", getSystemMetrics());
        
        // Database health
        health.put("database", getDatabaseHealth());
        
        // Application status
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        
        return health;
    }
    
    /**
     * Get memory metrics
     */
    private Map<String, Object> getMemoryMetrics() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        
        Map<String, Object> memory = new HashMap<>();
        
        // Heap memory
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long heapCommitted = memoryBean.getHeapMemoryUsage().getCommitted();
        
        memory.put("heapUsed", formatBytes(heapUsed));
        memory.put("heapMax", formatBytes(heapMax));
        memory.put("heapCommitted", formatBytes(heapCommitted));
        memory.put("heapUsagePercent", Math.round((heapUsed * 100.0 / heapMax) * 100.0) / 100.0);
        
        // Non-heap memory
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
        long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();
        
        memory.put("nonHeapUsed", formatBytes(nonHeapUsed));
        memory.put("nonHeapMax", formatBytes(nonHeapMax));
        
        return memory;
    }
    
    /**
     * Get system metrics
     */
    private Map<String, Object> getSystemMetrics() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        
        Map<String, Object> system = new HashMap<>();
        
        system.put("os", osBean.getName());
        system.put("osVersion", osBean.getVersion());
        system.put("architecture", osBean.getArch());
        system.put("availableProcessors", osBean.getAvailableProcessors());
        system.put("systemLoadAverage", Math.round(osBean.getSystemLoadAverage() * 100.0) / 100.0);
        
        // JVM info
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("jvmName", System.getProperty("java.vm.name"));
        
        return system;
    }
    
    /**
     * Get database health status
     */
    private Map<String, Object> getDatabaseHealth() {
        Map<String, Object> dbHealth = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(2);
            dbHealth.put("status", isValid ? "UP" : "DOWN");
            dbHealth.put("isConnected", isValid);
            
            if (isValid) {
                dbHealth.put("productName", connection.getMetaData().getDatabaseProductName());
                dbHealth.put("productVersion", connection.getMetaData().getDatabaseProductVersion());
                dbHealth.put("driverName", connection.getMetaData().getDriverName());
                dbHealth.put("url", connection.getMetaData().getURL());
            }
        } catch (Exception e) {
            log.error("Database health check failed", e);
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
        }
        
        return dbHealth;
    }
    
    /**
     * Get application metrics
     */
    public Map<String, Object> getMetrics() {
        log.info("Fetching application metrics");
        
        Map<String, Object> metrics = new HashMap<>();
        
        // Thread info
        metrics.put("threadsActive", Thread.activeCount());
        
        // Memory
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("used", formatBytes(usedMemory));
        memory.put("free", formatBytes(freeMemory));
        memory.put("total", formatBytes(totalMemory));
        memory.put("max", formatBytes(maxMemory));
        memory.put("usagePercent", Math.round((usedMemory * 100.0 / maxMemory) * 100.0) / 100.0);
        
        metrics.put("memory", memory);
        metrics.put("timestamp", Instant.now().toString());
        
        return metrics;
    }
    
    /**
     * Format uptime duration
     */
    private String formatUptime(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
    
    /**
     * Format bytes to human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
}
