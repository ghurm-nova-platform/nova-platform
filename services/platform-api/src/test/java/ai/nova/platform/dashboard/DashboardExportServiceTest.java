package ai.nova.platform.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import ai.nova.platform.audit.dto.AuditDtos.AuditEvent;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.dashboard.dto.DashboardDtos.ApprovalsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.AuditSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CiSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.CostSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardKpis;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardMeta;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.DeploymentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.EnvironmentsSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.OverviewSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.PipelineSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleaseSnapshot;
import ai.nova.platform.dashboard.dto.DashboardDtos.ReleasesSection;
import ai.nova.platform.dashboard.dto.DashboardDtos.RollbacksSection;
import ai.nova.platform.dashboard.service.DashboardExportService;

class DashboardExportServiceTest {

    private final DashboardExportService exportService = new DashboardExportService();

    @Test
    void exportsRealXlsxWorkbookAndNeutralizesFormulas() throws Exception {
        DashboardSnapshot snapshot = sampleSnapshot("=SUM(A1:A2)", "@danger", 5);

        DashboardExportService.ExportPayload payload = exportService.export(snapshot, "xlsx", "releases");

        assertThat(payload.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(payload.filename()).isEqualTo("dashboard-releases.xlsx");
        assertThat(new String(payload.content(), java.nio.charset.StandardCharsets.UTF_8)).doesNotContain("id,name,version");
        assertThat(payload.content()[0]).isEqualTo((byte) 'P');
        assertThat(payload.content()[1]).isEqualTo((byte) 'K');

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(payload.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
            var sheet = workbook.getSheetAt(0);
            assertThat(sheet.getSheetName()).isEqualTo("releases");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("id");
            assertThat(sheet.getRow(0).getCell(1).getStringCellValue()).isEqualTo("name");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("'=SUM(A1:A2)");
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).isEqualTo("1.0.0");
        }
    }

    @Test
    void exportsValidPdfThatOpensAndContainsExpectedEnglishText() throws Exception {
        DashboardSnapshot snapshot = sampleSnapshot("Release Alpha", "Audit Label", 1);

        DashboardExportService.ExportPayload payload = exportService.export(snapshot, "pdf", "overview");

        assertThat(payload.contentType()).isEqualTo("application/pdf");
        assertThat(payload.filename()).isEqualTo("dashboard-overview.pdf");
        assertThat(new String(payload.content(), 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1)).startsWith("%PDF-");

        try (PDDocument document = Loader.loadPDF(payload.content())) {
            assertThat(document.getNumberOfPages()).isGreaterThanOrEqualTo(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Nova Enterprise Dashboard Export");
            assertThat(text).contains("Section: overview");
            assertThat(text).contains("Organization:");
        }
    }

    @Test
    void exportsMultiPagePdfForLongContent() throws Exception {
        DashboardSnapshot snapshot = sampleSnapshot("Release Alpha", "Audit Label", 120);

        DashboardExportService.ExportPayload payload = exportService.export(snapshot, "pdf", "audit");

        try (PDDocument document = Loader.loadPDF(payload.content())) {
            assertThat(document.getNumberOfPages()).isGreaterThan(1);
            String text = new PDFTextStripper().getText(document);
            assertThat(text).contains("Section: audit");
            assertThat(text).contains("Audit Label 0");
            assertThat(text).contains("Audit Label 50");
        }
    }

    private static DashboardSnapshot sampleSnapshot(String releaseName, String auditLabel, int auditCount) {
        Instant now = Instant.parse("2026-07-21T09:00:00Z");
        List<AuditEvent> events = new ArrayList<>();
        for (int i = 0; i < auditCount; i++) {
            events.add(new AuditEvent(
                    UUID.randomUUID(),
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                    UUID.fromString("11111111-1111-1111-1111-111111111112"),
                    UUID.fromString("11111111-1111-1111-1111-111111111113"),
                    "User",
                    null,
                    AuditEntityType.RELEASE,
                    UUID.randomUUID(),
                    auditLabel + " " + i,
                    AuditAction.ACCESS,
                    AuditResult.SUCCESS,
                    AuditSeverity.MEDIUM,
                    AuditSource.PORTAL,
                    "corr-" + i,
                    "req-" + i,
                    "127.0.0.1",
                    "junit",
                    Map.of("label", auditLabel + " " + i),
                    now.plusSeconds(i)));
        }
        return new DashboardSnapshot(
                new DashboardMeta(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        UUID.fromString("11111111-1111-1111-1111-111111111112"),
                        now,
                        now.plusSeconds(30),
                        30,
                        false),
                new OverviewSection(
                        1,
                        2,
                        3,
                        4,
                        5,
                        6,
                        7,
                        8,
                        auditCount,
                        9,
                        1,
                        2,
                        new DashboardKpis(90.0, 80.0, 70.0, 60.0, 50.0, 40.0, 100L, 200L, 300L, 400L, 500L, 600L)),
                new PipelineSection(List.of(), 0),
                new DeploymentsSection(List.of(), 0, 0, 0),
                new ReleasesSection(
                        1,
                        1,
                        0,
                        0,
                        0,
                        1,
                        List.of(new ReleaseSnapshot(
                                UUID.randomUUID(),
                                UUID.fromString("11111111-1111-1111-1111-111111111112"),
                                releaseName,
                                "1.0.0",
                                "PUBLISHED",
                                now,
                                now))),
                new EnvironmentsSection(List.of(), List.of()),
                new AuditSection(events, auditCount),
                new ApprovalsSection(0, 0, 0, 0, List.of()),
                new CiSection(List.of(), 0, 0, 0, 0),
                new RollbacksSection(0, 0, 0, 0.0, 0, List.of()),
                new CostSection(0.0, List.of(), 0.0, "placeholder only"));
    }
}
