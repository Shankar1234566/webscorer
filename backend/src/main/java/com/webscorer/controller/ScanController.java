package com.webscorer.controller;

import com.webscorer.dto.CreateScanRequest;
import com.webscorer.dto.FindingResponse;
import com.webscorer.dto.ScanDetailResponse;
import com.webscorer.dto.ScanSummaryResponse;
import com.webscorer.entity.Scan;
import com.webscorer.service.ReportService;
import com.webscorer.service.WebsiteScanService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final WebsiteScanService scanService;
    private final ReportService reportService;

    public ScanController(WebsiteScanService scanService, ReportService reportService) {
        this.scanService = scanService;
        this.reportService = reportService;
    }

    /** Start a scan. Returns as soon as the scan is queued (201 + Location). */
    @PostMapping("/scans")
    public ResponseEntity<ScanSummaryResponse> createScan(@Valid @RequestBody CreateScanRequest request) {
        ScanSummaryResponse scan = scanService.createScan(request.url());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header(HttpHeaders.LOCATION, "/api/scans/" + scan.id())
                .body(scan);
    }

    /** Recent scans, newest first - drives the history page. */
    @GetMapping("/scans")
    public List<ScanSummaryResponse> listScans() {
        return scanService.listScans();
    }

    /** Status (including the live stage while running) plus the full report. */
    @GetMapping("/scans/{id}")
    public ScanDetailResponse getScan(@PathVariable Long id) {
        return scanService.getDetail(id);
    }

    @GetMapping("/scans/{id}/findings")
    public List<FindingResponse> getFindings(@PathVariable Long id) {
        return scanService.findings(id);
    }

    /** Deletes a scan together with its findings, scores and chat history. */
    @org.springframework.web.bind.annotation.DeleteMapping("/scans/{id}")
    public ResponseEntity<Void> deleteScan(@PathVariable Long id) {
        scanService.deleteScan(id);
        return ResponseEntity.noContent().build();
    }

    /** CSV download of the report. */
    @GetMapping("/scans/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id) {
        Scan scan = scanService.requireScan(id);
        String csv = reportService.toCsv(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(reportService.csvFilename(scan), StandardCharsets.UTF_8)
                .build());
        return new ResponseEntity<>(csv.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }
}
