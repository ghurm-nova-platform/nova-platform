package ai.nova.platform.dashboard.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    public ExportPayload export(DashboardSnapshot snapshot, String format, String section) {
        String normalizedFormat = format == null ? "csv" : format.trim().toLowerCase();
        String normalizedSection = section == null ? "overview" : section.trim().toLowerCase();
        List<String[]> rows = rowsForSection(snapshot, normalizedSection);
        return switch (normalizedFormat) {
            case "csv" -> csvPayload(rows, normalizedSection);
            case "xlsx" -> xlsxPayload(rows, normalizedSection);
            case "pdf" -> pdfPayload(snapshot, normalizedSection, rows);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "DASHBOARD_EXPORT_FORMAT", "Unsupported export format: " + format);
        };
    }

    private ExportPayload csvPayload(List<String[]> rows, String section) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            for (String[] row : rows) {
                writer.write(String.join(",", escape(row)));
                writer.write("\n");
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "DASHBOARD_EXPORT_FAILED", "CSV export failed");
        }
        return new ExportPayload(
                out.toByteArray(),
                "text/csv",
                "dashboard-" + section + ".csv");
    }

    private ExportPayload xlsxPayload(List<String[]> rows, String section) {
        ExportPayload csv = csvPayload(rows, section);
        return new ExportPayload(
                csv.content(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dashboard-" + section + ".xlsx");
    }

    private ExportPayload pdfPayload(DashboardSnapshot snapshot, String section, List<String[]> rows) {
        StringBuilder text = new StringBuilder();
        text.append("Nova Enterprise Dashboard Export\n");
        text.append("Generated: ").append(snapshot.meta().generatedAt()).append('\n');
        text.append("Section: ").append(section).append('\n');
        text.append("Organization: ").append(snapshot.meta().organizationId()).append('\n');
        text.append('\n');
        for (String[] row : rows) {
            text.append(String.join(" | ", row)).append('\n');
        }
        byte[] pdf = SimplePdfWriter.write(text.toString());
        return new ExportPayload(pdf, "application/pdf", "dashboard-" + section + ".pdf");
    }

    private List<String[]> rowsForSection(DashboardSnapshot snapshot, String section) {
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

    private List<String[]> overviewRows(OverviewSection overview) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"metric", "value"});
        rows.add(new String[] {"projects", String.valueOf(overview.projectCount())});
        rows.add(new String[] {"agents", String.valueOf(overview.agentCount())});
        rows.add(new String[] {"activeRuns", String.valueOf(overview.activeRunCount())});
        rows.add(new String[] {"releases", String.valueOf(overview.releaseCount())});
        rows.add(new String[] {"deployments", String.valueOf(overview.deploymentCount())});
        rows.add(new String[] {"executions", String.valueOf(overview.executionCount())});
        rows.add(new String[] {"environments", String.valueOf(overview.environmentCount())});
        rows.add(new String[] {"auditEvents", String.valueOf(overview.auditEventCount())});
        rows.add(new String[] {"pendingApprovals", String.valueOf(overview.pendingApprovalCount())});
        rows.add(new String[] {"releaseSuccessRate", String.valueOf(overview.kpis().releaseSuccessRate())});
        rows.add(new String[] {"deploymentSuccessRate", String.valueOf(overview.kpis().deploymentSuccessRate())});
        return rows;
    }

    private List<String[]> pipelineRows(List<PipelineStageSnapshot> stages) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"stage", "current", "waiting", "failed", "success", "avgDurationMs"});
        for (PipelineStageSnapshot stage : stages) {
            rows.add(new String[] {
                stage.stage().name(),
                String.valueOf(stage.current()),
                String.valueOf(stage.waiting()),
                String.valueOf(stage.failed()),
                String.valueOf(stage.success()),
                String.valueOf(stage.avgDurationMs())
            });
        }
        return rows;
    }

    private List<String[]> deploymentRows(List<DeploymentExecutionSnapshot> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"id", "provider", "status", "stage", "step", "durationMs", "progress"});
        for (DeploymentExecutionSnapshot item : items) {
            rows.add(new String[] {
                String.valueOf(item.id()),
                item.provider(),
                item.status(),
                nullToEmpty(item.currentStage()),
                nullToEmpty(item.currentStep()),
                String.valueOf(item.durationMs()),
                String.valueOf(item.progressPercent())
            });
        }
        return rows;
    }

    private List<String[]> releaseRows(List<ReleaseSnapshot> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"id", "name", "version", "status", "createdAt", "publishedAt"});
        for (ReleaseSnapshot item : items) {
            rows.add(new String[] {
                String.valueOf(item.id()),
                item.releaseName(),
                item.semanticVersion(),
                item.status(),
                String.valueOf(item.createdAt()),
                String.valueOf(item.publishedAt())
            });
        }
        return rows;
    }

    private List<String[]> environmentRows(List<EnvironmentItemSnapshot> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"code", "name", "type", "status", "health", "runningExecutions"});
        for (EnvironmentItemSnapshot item : items) {
            rows.add(new String[] {
                item.code(),
                item.name(),
                item.environmentType(),
                item.status(),
                item.health(),
                String.valueOf(item.runningExecutions())
            });
        }
        return rows;
    }

    private List<String[]> auditRows(AuditSection audit) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"id", "entityType", "action", "result", "severity", "createdAt"});
        audit.events().forEach(event -> rows.add(new String[] {
            String.valueOf(event.id()),
            event.entityType().name(),
            event.action().name(),
            event.result().name(),
            event.severity().name(),
            String.valueOf(event.createdAt())
        }));
        return rows;
    }

    private List<String[]> approvalRows(List<ApprovalQueueItem> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"taskId", "displayName", "status", "expired", "blocked", "waitingSince"});
        for (ApprovalQueueItem item : items) {
            rows.add(new String[] {
                String.valueOf(item.taskId()),
                item.displayName(),
                item.status(),
                String.valueOf(item.expired()),
                String.valueOf(item.blocked()),
                String.valueOf(item.waitingSince())
            });
        }
        return rows;
    }

    private List<String[]> ciRows(List<CiPipelineSnapshot> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"taskId", "provider", "repository", "branch", "status", "durationMs", "failed"});
        for (CiPipelineSnapshot item : items) {
            rows.add(new String[] {
                String.valueOf(item.taskId()),
                item.provider(),
                item.repository(),
                item.branch(),
                item.overallStatus(),
                String.valueOf(item.durationMs()),
                String.valueOf(item.failed())
            });
        }
        return rows;
    }

    private List<String[]> rollbackRows(List<RollbackSnapshot> items) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"id", "currentVersion", "targetVersion", "environment", "status", "durationMs"});
        for (RollbackSnapshot item : items) {
            rows.add(new String[] {
                String.valueOf(item.id()),
                item.currentVersion(),
                item.targetVersion(),
                item.environmentCode(),
                item.status(),
                String.valueOf(item.durationMs())
            });
        }
        return rows;
    }

    private List<String[]> costRows(DashboardSnapshot snapshot) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {"estimatedTotalCost", String.valueOf(snapshot.cost().estimatedTotalCost())});
        rows.add(new String[] {"futureLlmCostEstimate", String.valueOf(snapshot.cost().futureLlmCostEstimate())});
        rows.add(new String[] {"note", snapshot.cost().note()});
        snapshot.cost().providerUsage().forEach(usage -> rows.add(new String[] {
            usage.provider(), String.valueOf(usage.estimatedCost()), String.valueOf(usage.operationCount())
        }));
        return rows;
    }

    private static String[] escape(String[] row) {
        String[] escaped = new String[row.length];
        for (int i = 0; i < row.length; i++) {
            String value = row[i] == null ? "" : row[i];
            if (value.contains(",") || value.contains("\"")) {
                escaped[i] = "\"" + value.replace("\"", "\"\"") + "\"";
            } else {
                escaped[i] = value;
            }
        }
        return escaped;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record ExportPayload(byte[] content, String contentType, String filename) {}

    static final class SimplePdfWriter {
        private SimplePdfWriter() {
        }

        static byte[] write(String text) {
            List<String> lines = List.of(text.split("\n", -1));
            StringBuilder content = new StringBuilder();
            content.append("BT /F1 10 Tf 50 780 Td ");
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) {
                    content.append("0 -12 Td ");
                }
                content.append("(").append(escapePdf(lines.get(i))).append(") Tj ");
            }
            content.append("ET");
            byte[] streamBytes = content.toString().getBytes(StandardCharsets.US_ASCII);
            String header = """
                    %PDF-1.4
                    1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj
                    2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj
                    3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>endobj
                    4 0 obj<< /Length %d >>stream
                    """.formatted(streamBytes.length);
            String font = """
                    endstream
                    endobj
                    5 0 obj<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>endobj
                    xref
                    0 6
                    0000000000 65535 f
                    0000000009 00000 n
                    0000000058 00000 n
                    0000000115 00000 n
                    0000000240 00000 n
                    0000000%03d 00000 n
                    trailer<< /Size 6 /Root 1 0 R >>
                    startxref
                    %d
                    %%EOF
                    """.formatted(300 + streamBytes.length, 300 + streamBytes.length);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.writeBytes(header.getBytes(StandardCharsets.US_ASCII));
            out.writeBytes(streamBytes);
            out.writeBytes(font.getBytes(StandardCharsets.US_ASCII));
            return out.toByteArray();
        }

        private static String escapePdf(String value) {
            return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        }
    }
}
