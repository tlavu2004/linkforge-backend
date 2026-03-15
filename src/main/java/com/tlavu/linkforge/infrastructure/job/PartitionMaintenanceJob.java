package com.tlavu.linkforge.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class PartitionMaintenanceJob {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Executes on application startup and on the 1st of every month at 3:00 AM.
     * Ensures partitions for the next 3 months exist.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 3 1 * ?")
    public void maintainPartitions() {
        log.info("Starting database partition maintenance...");
        
        // We ensure partitions for current month and next 3 months
        LocalDate now = LocalDate.now();
        for (int i = 0; i <= 3; i++) {
            createPartitionForMonth(now.plusMonths(i));
        }
        
        log.info("Partition maintenance completed.");
    }

    @SuppressWarnings("null")
    private void createPartitionForMonth(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy'_m'MM"));
        String tableName = "short_links_y" + yearMonth;
        
        LocalDate firstDay = date.withDayOfMonth(1);
        LocalDate nextMonth = firstDay.plusMonths(1);
        
        String startStr = firstDay.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00+00";
        String endStr = nextMonth.format(DateTimeFormatter.ISO_LOCAL_DATE) + " 00:00:00+00";

        try {
            // Check if partition already exists (PostgreSQL specific check)
            Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename = ?)",
                Boolean.class, tableName.toLowerCase());

            if (Boolean.FALSE.equals(exists)) {
                String sql = String.format(
                    "CREATE TABLE %s PARTITION OF short_links FOR VALUES FROM ('%s') TO ('%s')",
                    tableName, startStr, endStr
                );
                jdbcTemplate.execute(sql);
                log.info("Created partition: {}", tableName);
            } else {
                log.info("Partition already exists: {}", tableName);
            }
        } catch (Exception e) {
            log.error("Failed to create partition: {}", tableName, e);
        }
    }
}
