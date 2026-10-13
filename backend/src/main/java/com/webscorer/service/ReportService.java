package com.webscorer.service;

import com.webscorer.domain.ScanStatus;
import com.webscorer.dto.CategoryScoreResponse;
import com.webscorer.dto.FindingResponse;
import com.webscorer.dto.ScanDetailResponse;
import com.webscorer.entity.Scan;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Renders a scan as a CSV download. Deliberately plain so it opens anywhere;
 * a PDF export would need an extra rendering dependency.
 */
@Service
public class ReportService {

    private static final DateTimeFormatter TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'").withZone(java.time.ZoneOffset.UTC);

    private final WebsiteScanService scanService;

    public ReportService(WebsiteScanService scanService) {
        this.scanService = scanService;
    }

    public String csvFilename(Scan scan) {
        String host = scan.getUrl().replaceAll("[^A-Za-z0-9.-]", "_");
        if (host.length() > 40) {
            host = host.substring(0, 40);
        }
        return "webscorer-" + host + "-" + scan.getId() + ".csv";
    }

    public String toCsv(Long scanId) {
        Scan scan = scanService.requireScan(scanId);
        ScanDetailResponse detail = scanService.getDetail(scanId);
        StringBuilder csv = new StringBuilder();

        csv.append("WebScorer AI report\n");
        csv.append(row("URL", scan.getUrl()));
        csv.append(row("Final URL", nullToEmpty(scan.getFinalUrl())));
        csv.append(row("Scanned at", scan.getCreatedAt() == null ? "" : TIMESTAMP.format(scan.getCreatedAt())));
        csv.append(row("Status", scan.getStatus().name()));
        csv.append(row("HTTP status", scan.getHttpStatus() == null ? "" : String.valueOf(scan.getHttpStatus())));
        csv.append(row("Duration (ms)", scan.getDurationMs() == null ? "" : String.valueOf(scan.getDurationMs())));
        csv.append(row("Overall score", scan.getOverallScore() == null ? "Not enough data" : scan.getOverallScore() + "/100"));
        csv.append(row("Checks passed", String.valueOf(zeroIfNull(scan.getChecksPassed()))));
        csv.append(row("Checks failed", String.valueOf(zeroIfNull(scan.getChecksFailed()))));
        csv.append(row("Checks not run", String.valueOf(zeroIfNull(scan.getChecksNotRun()))));
        if (scan.getErrorMessage() != null) {
            csv.append(row("Error", scan.getErrorMessage()));
        }
        csv.append('\n');

        csv.append("Category scores\n");
        csv.append("category,score,passed,applicable,not_run,defined\n");
        for (CategoryScoreResponse score : detail.categoryScores()) {
            csv.append(csvLine(score.category().name(),
                    score.score() == null ? "not enough data" : String.valueOf(score.score()),
                    String.valueOf(score.checksPassed()),
                    String.valueOf(score.checksApplicable()),
                    String.valueOf(score.checksNotRun()),
                    String.valueOf(score.checksDefined())));
        }
        csv.append('\n');

        csv.append("Findings\n");
        csv.append("category,status,severity,check_id,title,evidence,explanation,recommendation\n");
        List<FindingResponse> findings = detail.findings();
        if (findings.isEmpty()) {
            csv.append(csvLine("-", "-", "-", "-", "No checks were recorded for this scan.", "", "", ""));
        }
        for (FindingResponse finding : findings) {
            csv.append(csvLine(
                    finding.category().name(),
                    finding.status().name(),
                    finding.severity().name(),
                    finding.checkId(),
                    finding.title(),
                    nullToEmpty(finding.evidence()),
                    nullToEmpty(finding.explanation()),
                    nullToEmpty(finding.recommendation())));
        }

        if (scan.getStatus() != ScanStatus.COMPLETED) {
            csv.append("\nNote: this scan is not complete, so the report contains partial data only.\n");
        }
        return csv.toString();
    }

    private String row(String label, String value) {
        return csvLine(label, value);
    }

    private static int zeroIfNull(Integer value) {
        return value == null ? 0 : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /** CSV escaping: wrap when needed, double embedded quotes, strip newlines. */
    private String csvLine(String... cells) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            String cell = cells[i] == null ? "" : cells[i].replace("\r", " ").replace("\n", " | ");
            boolean needsQuotes = cell.contains(",") || cell.contains("\"") || cell.contains("|");
            if (needsQuotes) {
                cell = "\"" + cell.replace("\"", "\"\"") + "\"";
            }
            line.append(cell);
            if (i < cells.length - 1) {
                line.append(',');
            }
        }
        return line.append('\n').toString();
    }
}
