package ai.nova.platform.knowledge.engine.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.CreateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.ExportPayload;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.ImportDocumentRequest;
import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.ContentFormat;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.Visibility;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class KnowledgeImportExportService {

    private static final Pattern ADR_TITLE = Pattern.compile("(?i)^#?\\s*adr[-\\s]?\\d+");
    private static final float PDF_FONT_SIZE = 10f;
    private static final float PDF_LEADING = 14f;
    private static final float PDF_MARGIN = 50f;

    private final KnowledgeService knowledgeService;
    private final ObjectMapper objectMapper;

    public KnowledgeImportExportService(KnowledgeService knowledgeService, ObjectMapper objectMapper) {
        this.knowledgeService = knowledgeService;
        this.objectMapper = objectMapper;
    }

    public DocumentDetail importDocument(ImportDocumentRequest request, AuthenticatedUser user) {
        ContentFormat format = resolveImportFormat(request);
        KnowledgeType knowledgeType = resolveKnowledgeType(request, format);
        Category category = request.category() == null ? Category.General : request.category();
        Visibility visibility = request.visibility() == null ? Visibility.ORGANIZATION : request.visibility();
        String title = request.title();
        String summary = request.summary();
        String content = request.content();
        if (title == null || title.isBlank()) {
            title = deriveTitle(content);
        }
        if (summary == null || summary.isBlank()) {
            summary = deriveSummary(content);
        }
        return knowledgeService.create(
                new CreateDocumentRequest(
                        request.projectId(),
                        title,
                        summary,
                        content,
                        format,
                        knowledgeType,
                        category,
                        visibility,
                        request.tags()),
                user);
    }

    public ExportPayload exportDocument(DocumentDetail document, String format) {
        String normalized = format == null ? "markdown" : format.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "markdown", "md" -> exportMarkdown(document);
            case "json" -> exportJson(document);
            case "pdf" -> exportPdf(document);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "KNOWLEDGE_EXPORT_FORMAT", "Unsupported export format: " + format);
        };
    }

    private ExportPayload exportMarkdown(DocumentDetail document) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ").append(document.title()).append("\n\n");
        if (document.summary() != null && !document.summary().isBlank()) {
            builder.append(document.summary()).append("\n\n");
        }
        builder.append("---\n\n");
        builder.append(document.content() == null ? "" : document.content());
        byte[] bytes = builder.toString().getBytes(StandardCharsets.UTF_8);
        return new ExportPayload(bytes, "text/markdown", sanitizeFileName(document.title()) + ".md");
    }

    private ExportPayload exportJson(DocumentDetail document) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", document.id());
        payload.put("title", document.title());
        payload.put("summary", document.summary());
        payload.put("content", document.content());
        payload.put("contentFormat", document.contentFormat());
        payload.put("knowledgeType", document.knowledgeType());
        payload.put("category", document.category());
        payload.put("status", document.status());
        payload.put("visibility", document.visibility());
        payload.put("projectId", document.projectId());
        payload.put("authorId", document.authorId());
        payload.put("version", document.version());
        payload.put("tags", document.tags());
        payload.put("relations", document.relations());
        payload.put("attachments", document.attachments());
        payload.put("createdAt", document.createdAt());
        payload.put("updatedAt", document.updatedAt());
        try {
            byte[] bytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(payload);
            return new ExportPayload(bytes, "application/json", sanitizeFileName(document.title()) + ".json");
        } catch (JsonProcessingException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "KNOWLEDGE_EXPORT_FAILED", "JSON export failed");
        }
    }

    private ExportPayload exportPdf(DocumentDetail document) {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            pdf.addPage(page);
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream stream = new PDPageContentStream(pdf, page)) {
                stream.beginText();
                stream.setFont(font, PDF_FONT_SIZE + 2);
                stream.newLineAtOffset(PDF_MARGIN, page.getMediaBox().getHeight() - PDF_MARGIN);
                stream.showText(truncate(document.title(), 90));
                stream.endText();
                float y = page.getMediaBox().getHeight() - PDF_MARGIN - PDF_LEADING * 2;
                for (String line : wrapLines(buildPdfBody(document), 95)) {
                    if (y < PDF_MARGIN) {
                        break;
                    }
                    stream.beginText();
                    stream.setFont(font, PDF_FONT_SIZE);
                    stream.newLineAtOffset(PDF_MARGIN, y);
                    stream.showText(line);
                    stream.endText();
                    y -= PDF_LEADING;
                }
            }
            pdf.save(out);
            return new ExportPayload(out.toByteArray(), "application/pdf", sanitizeFileName(document.title()) + ".pdf");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "KNOWLEDGE_EXPORT_FAILED", "PDF export failed");
        }
    }

    private String buildPdfBody(DocumentDetail document) {
        StringBuilder builder = new StringBuilder();
        if (document.summary() != null) {
            builder.append(document.summary()).append("\n\n");
        }
        builder.append(document.content() == null ? "" : document.content());
        return builder.toString();
    }

    private ContentFormat resolveImportFormat(ImportDocumentRequest request) {
        if (request.contentFormat() != null) {
            return request.contentFormat();
        }
        String importFormat = request.importFormat() == null ? "" : request.importFormat().trim().toLowerCase(Locale.ROOT);
        return switch (importFormat) {
            case "adr", "markdown", "md", "readme", "runbook" -> ContentFormat.MARKDOWN;
            case "plain", "txt", "plain_text" -> ContentFormat.PLAIN_TEXT;
            case "html" -> ContentFormat.HTML;
            case "json" -> ContentFormat.JSON;
            case "yaml", "yml" -> ContentFormat.YAML;
            case "sql" -> ContentFormat.SQL;
            case "xml" -> ContentFormat.XML;
            case "code" -> ContentFormat.CODE;
            default -> ContentFormat.MARKDOWN;
        };
    }

    private KnowledgeType resolveKnowledgeType(ImportDocumentRequest request, ContentFormat format) {
        if (request.knowledgeType() != null) {
            return request.knowledgeType();
        }
        String importFormat = request.importFormat() == null ? "" : request.importFormat().trim().toLowerCase(Locale.ROOT);
        if ("adr".equals(importFormat) || ADR_TITLE.matcher(request.title() == null ? "" : request.title()).find()) {
            return KnowledgeType.ADR;
        }
        if ("runbook".equals(importFormat)) {
            return KnowledgeType.RUNBOOK;
        }
        if ("readme".equals(importFormat)) {
            return KnowledgeType.DOCUMENTATION;
        }
        return format == ContentFormat.CODE ? KnowledgeType.CODE : KnowledgeType.DOCUMENTATION;
    }

    private String deriveTitle(String content) {
        if (content == null || content.isBlank()) {
            return "Imported Document";
        }
        String[] lines = content.split("\\R", 3);
        String first = lines[0].trim();
        if (first.startsWith("#")) {
            return first.replaceFirst("^#+\\s*", "").trim();
        }
        return truncate(first, 120);
    }

    private String deriveSummary(String content) {
        if (content == null || content.isBlank()) {
            return null;
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return truncate(normalized, 500);
    }

    private String sanitizeFileName(String title) {
        String safe = title == null ? "document" : title.replaceAll("[^a-zA-Z0-9._-]+", "-");
        return safe.isBlank() ? "document" : safe;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private List<String> wrapLines(String text, int maxChars) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] rawLines = text.split("\\R");
        for (String raw : rawLines) {
            String remaining = raw;
            while (remaining.length() > maxChars) {
                lines.add(remaining.substring(0, maxChars));
                remaining = remaining.substring(maxChars);
            }
            lines.add(remaining);
        }
        return lines;
    }
}
