package com.webscorer.config;

import com.webscorer.service.WebsiteScanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On boot, any scan left in a running state by a restart is marked failed with
 * an honest message instead of staying "in progress" forever.
 */
@Component
public class StaleScanCleanup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StaleScanCleanup.class);

    private final WebsiteScanService scanService;

    public StaleScanCleanup(WebsiteScanService scanService) {
        this.scanService = scanService;
    }

    @Override
    public void run(ApplicationArguments args) {
        scanService.failInterruptedScans();
        log.info("Startup check complete - interrupted scans were marked as failed.");
    }
}
