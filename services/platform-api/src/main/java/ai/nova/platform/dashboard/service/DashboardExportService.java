package ai.nova.platform.dashboard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalQueueItem;
import ai.nova.platform.dashboard.dto.DashboardDtos.AuditSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiPipelineSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentExecutionSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentItemSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.OverviewSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineStageSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleaseSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbackSnapshot;
import ai.nova.platform.web.error.ApiException;

@Service
public class DashboardExportService {

    private static final int MAX_COLUMN_WIDTH = 80 * 256;
    private static final float FONT_SIZE = 10f;
    private static final float LEADING = 14f;
    private static final float MARGIN = 50f;

    public ExportPayload export(DashboardSnapshot snapshot, String format, String section) {
        String normalizedFormat = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        String normalizedSection = section == null ? "overview" : section.trim().toLowerCase(Locale.ROOT);
        List<List<Object>> rows = rowsForSection(snapshot, normalizedSection);
        return switch (normalizedFormat) {
            case "csv" -> csvPayload(rows, normalizedSection);
            case "xlsx" -> xlsxPayload(rows, normalizedSection);
            case "pdf" -> pdfPayload(snapshot, normalizedSection, rows);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "DASHBOARD_EXPORT_FORMAT", "Unsupported export format: " + format);
        };
    }

    private ExportPayload csvPayload(List<List<Object>> rows, String section) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            for (List<Object> row : rows) {
                writer.write(String.join(",", escapeCsv(row)));
                writer.write("\n");
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHBOARD_EXPORT_FAILED", "CSV export failed");
        }
        return new ExportPayload(out.toByteArray(), "text/csv", "dashboard-" + section + ".csv");
    }

    private ExportPayload xlsxPayload(List<List<Object>> rows, String section) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = WorkbookUtil.createSafeSheetName(section == null || section.isBlank() ? "dashboard" : section);
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                Row row = sheet.createRow(rowIndex);
                List<Object> values = rows.get(rowIndex);
                for (int colIndex = 0; colIndex < values.size(); colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    if (rowIndex == 0) {
                        cell.setCellStyle(headerStyle);
                    }
                    writeCell(cell, values.get(colIndex));
                }
            }

            int columnCount = rows.stream().mapToInt(List::size).max().orElse(0);
            for (int colIndex = 0; colIndex < columnCount; colIndex++) {
                sheet.autoSizeColumn(colIndex);
                if (sheet.getColumnWidth(colIndex) > MAX_COLUMN_WIDTH) {
                    sheet.setColumnWidth(colIndex, MAX_COLUMN_WIDTH);
                }
            }

            workbook.write(out);
            return new ExportPayload(
                    out.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "dashboard-" + section + ".xlsx");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHBOARD_EXPORT_FAILED", "XLSX export failed");
        }
    }

    private ExportPayload pdfPayload(DashboardSnapshot snapshot, String section, List<List<Object>> rows) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            List<String> lines = new ArrayList<>();
            lines.add("Nova Enterprise Dashboard Export");
            lines.add("Generated: " + snapshot.meta().generatedAt());
            lines.add("Section: " + section);
            lines.add("Organization: " + snapshot.meta().organizationId());
            lines.add("");
            for (List<Object> row : rows) {
                List<String> cells = new ArrayList<>();
                for (Object value : row) {
                    cells.add(toPdfText(value));
                }
                lines.add(String.join(" | ", cells));
            }

            float pageHeight = PDRectangle.LETTER.getHeight();
            int maxLinesPerPage = Math.max(1, (int) ((pageHeight - (2 * MARGIN)) / LEADING));
            int index = 0;
            while (index < lines.size()) {
                PDPage page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, FONT_SIZE);
                    content.newLineAtOffset(MARGIN, pageHeight - MARGIN);
                    for (int lineCount = 0; lineCount < maxLinesPerPage && index < lines.size(); lineCount++, index++) {
                        String line = sanitizePdfText(lines.get(index), font);
                        if (lineCount > 0) {
                            content.newLineAtOffset(0, -LEADING);
                        }
                        content.showText(line);
                    }
                    content.endText();
                }
            }

            document.save(out);
            return new ExportPayload(out.toByteArray(), "application/pdf", "dashboard-" + section + ".pdf");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHBOARD_EXPORT_FAILED", "PDF export failed");
        }
    }

    private List<List<Object>> rowsForSection(DashboardSnapshot snapshot, String section) {
        return switch (section) {
            case "overview" -> overviewRows(snapshot.overview());
            case "pipeline" -> pipelineRows(snapshot.pipeline().stages());
            case "deployments" -> deploymentRows(snapshot.deployments().running());
            case "releases" -> releaseRows(snapshot.releases().recent());
            case "environments" -> environmentRows(snapshot.environments().environments());
            case "audit" -> auditRows(snapshot.audit());
            case "approvals" -> approvalRows(snapshot.approvals().queue());
            case "ci" -> ciRows(snapshot.ci().recentPipelines());
            case "rollbacks" -> rollbackRows(snapshot.rollbacks().recent());
            case "cost" -> costRows(snapshot);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "DASHBOARD_EXPORT_SECTION", "Unsupported export section: " + section);
        };
    }

    private List<List<Object>> overviewRows(OverviewSection overview) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("metric", "value"));
        rows.add(List.of("projects", overview.projectCount()));
        rows.add(List.of("agents", overview.agentCount()));
        rows.add(List.of("activeRuns", overview.activeRunCount()));
        rows.add(List.of("releases", overview.releaseCount()));
        rows.add(List.of("deployments", overview.deploymentCount()));
        rows.add(List.of("executions", overview.executionCount()));
        rows.add(List.of("environments", overview.environmentCount()));
        rows.add(List.of("auditEvents", overview.auditEventCount()));
        rows.add(List.of("pendingApprovals", overview.pendingApprovalCount()));
        rows.add(List.of("releaseSuccessRate", overview.kpis().releaseSuccessRate()));
        rows.add(List.of("deploymentSuccessRate", overview.kpis().deploymentSuccessRate()));
        return rows;
    }

    private List<List<Object>> pipelineRows(List<PipelineStageSnapshot> stages) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("stage", "current", "waiting", "failed", "success", "avgDurationMs"));
        for (PipelineStageSnapshot stage : stages) {
            rows.add(List.of(
                    stage.stage().name(),
                    stage.current(),
                    stage.waiting(),
                    stage.failed(),
                    stage.success(),
                    stage.avgDurationMs()));
        }
        return rows;
    }

    private List<List<Object>> deploymentRows(List<DeploymentExecutionSnapshot> items) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "provider", "status", "stage", "step", "durationMs", "progress"));
        for (DeploymentExecutionSnapshot item : items) {
            rows.add(List.of(
                    String.valueOf(item.id()),
                    item.provider(),
                    item.status(),
                    nullToEmpty(item.currentStage()),
                    nullToEmpty(item.currentStep()),
                    item.durationMs() == null ? "" : item.durationMs(),
                    item.progressPercent()));
        }
        return rows;
    }

    private List<List<Object>> releaseRows(List<ReleaseSnapshot> items) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "name", "version", "status", "createdAt", "publishedAt"));
        for (ReleaseSnapshot item : items) {
            rows.add(List.of(
                    String.valueOf(item.id()),
                    item.releaseName(),
                    item.semanticVersion(),
                    item.status(),
                    String.valueOf(item.createdAt()),
                    String.valueOf(item.publishedAt())));
        }
        return rows;
    }

    private List<List<Object>> environmentRows(List<EnvironmentItemSnapshot> items) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("code", "name", "type", "status", "health", "runningExecutions"));
        for (EnvironmentItemSnapshot item : items) {
            rows.add(List.of(
                    item.code(),
                    item.name(),
                    item.environmentType(),
                    item.status(),
                    item.health(),
                    item.runningExecutions()));
        }
        return rows;
    }

    private List<List<Object>> auditRows(AuditSection audit) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "entityType", "action", "result", "severity", "createdAt", "entityLabel"));
        audit.events().forEach(event -> rows.add(List.of(
                String.valueOf(event.id()),
                event.entityType().name(),
                event.action().name(),
                event.result().name(),
                event.severity().name(),
                String.valueOf(event.createdAt()),
                nullToEmpty(event.entityLabel()))));
        return rows;
    }

    private List<List<Object>> approvalRows(List<ApprovalQueueItem> items) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("taskId", "displayName", "status", "expired", "blocked", "waitingSince"));
        for (ApprovalQueueItem item : items) {
            rows.add(List.of(
                    String.valueOf(item.taskId()),
                    item.displayName(),
                    item.status(),
                    item.expired(),
                    item.blocked(),
                    String.valueOf(item.waitingSince())));
        }
        return rows;
    }

    private List<List<Object>> ciRows(List<CiPipelineSnapshot> items) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("taskId", "provider", "repository", "branch", "status", "durationMs", "failed"));
        for (CiPipelineSnapshot item : items) {
            rows.add(List.of(
                    String.valueOf(item.taskId()),
                    item.provider(),
                    item.repository(),
                    item.branch(),
                    item.overallStatus(),
                    item.durationMs() == null ? "" : item.durationMs(),
                    item.failed()));
        }
        return rows;
    }

    private List<List<Object>> rollbackRows(List<RollbackSnapshot> items) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "currentVersion", "targetVersion", "environment", "status", "durationMs"));
        for (RollbackSnapshot item : items) {
            rows.add(List.of(
                    String.valueOf(item.id()),
                    item.currentVersion(),
                    item.targetVersion(),
                    item.environmentCode(),
                    item.status(),
                    item.durationMs() == null ? "" : item.durationMs()));
        }
        return rows;
    }

    private List<List<Object>> costRows(DashboardSnapshot snapshot) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("estimatedTotalCost", snapshot.cost().estimatedTotalCost()));
        rows.add(List.of("futureLlmCostEstimate", snapshot.cost().futureLlmCostEstimate()));
        rows.add(List.of("note", snapshot.cost().note()));
        snapshot.cost().providerUsage().forEach(usage -> rows.add(List.of(
                usage.provider(), usage.estimatedCost(), usage.operationCount())));
        return rows;
    }

    private static List<String> escapeCsv(List<Object> row) {
        List<String> escaped = new ArrayList<>();
        for (Object value : row) {
            String text = sanitizeForSpreadsheet(toCellText(value));
            if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
                escaped.add("\"" + text.replace("\"", "\"\"") + "\"");
            } else {
                escaped.add(text);
            }
        }
        return escaped;
    }

    private static void writeCell(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Boolean bool) {
            cell.setCellValue(bool);
            return;
        }
        if (value instanceof Integer number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value instanceof Long number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        if (value instanceof Double number) {
            cell.setCellValue(number);
            return;
        }
        if (value instanceof Float number) {
            cell.setCellValue(number.doubleValue());
            return;
        }
        String text = sanitizeForSpreadsheet(toCellText(value));
        cell.setCellValue(text);
    }

    private static String sanitizeForSpreadsheet(String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }
        char first = value.charAt(0);
        if (first == '=' || first == '+' || first == '-' || first == '@') {
            return "'" + value;
        }
        return value;
    }

    private static String toCellText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String toPdfText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static String sanitizePdfText(String value, PDType1Font font) throws IOException {
        StringBuilder sanitized = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String s = String.valueOf(value.charAt(i));
            try {
                font.encode(s);
                sanitized.append(s);
            } catch (IllegalArgumentException ex) {
                sanitized.append('?');
            }
        }
        return sanitized.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record ExportPayload(byte[] content, String contentType, String filename) {}
}
