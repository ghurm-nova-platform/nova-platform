package ai.nova.platform.identity.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.identity.dto.IdentityDtos.GroupView;
import ai.nova.platform.identity.dto.IdentityDtos.LoginHistoryView;
import ai.nova.platform.identity.dto.IdentityDtos.PermissionView;
import ai.nova.platform.identity.dto.IdentityDtos.RoleView;
import ai.nova.platform.identity.dto.IdentityDtos.UserView;
import ai.nova.platform.web.error.ApiException;

@Service
public class IdentityExportService {

    public record ExportPayload(byte[] content, String contentType, String filename) {
    }

    private final IdentityUserService identityUserService;
    private final GroupService groupService;
    private final RoleService roleService;
    private final PermissionService permissionService;
    private final IdentityService identityService;
    private final ObjectMapper objectMapper;

    public IdentityExportService(
            IdentityUserService identityUserService,
            GroupService groupService,
            RoleService roleService,
            PermissionService permissionService,
            IdentityService identityService,
            ObjectMapper objectMapper) {
        this.identityUserService = identityUserService;
        this.groupService = groupService;
        this.roleService = roleService;
        this.permissionService = permissionService;
        this.identityService = identityService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ExportPayload export(UUID organizationId, String resource, String format) {
        String normalizedFormat = format == null ? "csv" : format.trim().toLowerCase(Locale.ROOT);
        List<List<Object>> rows = rowsForResource(organizationId, resource);
        return switch (normalizedFormat) {
            case "csv" -> csvPayload(rows, resource);
            case "json" -> jsonPayload(rows, resource);
            case "xlsx" -> xlsxPayload(rows, resource);
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "IDENTITY_EXPORT_FORMAT", "Unsupported export format: " + format);
        };
    }

    private List<List<Object>> rowsForResource(UUID organizationId, String resource) {
        return switch (resource.toLowerCase(Locale.ROOT)) {
            case "users" -> userRows(identityUserService.listUsers(organizationId));
            case "groups" -> groupRows(groupService.listGroups(organizationId));
            case "roles" -> roleRows(roleService.listRoles(organizationId));
            case "permissions" -> permissionRows(permissionService.listPermissions(organizationId));
            case "login-history" -> loginHistoryRows(
                    identityService.loginHistory(organizationId));
            default -> throw new ApiException(
                    HttpStatus.BAD_REQUEST, "IDENTITY_EXPORT_RESOURCE", "Unsupported export resource: " + resource);
        };
    }

    private List<List<Object>> userRows(List<UserView> users) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "email", "displayName", "enabled", "mfaEnabled", "locked"));
        for (UserView user : users) {
            rows.add(List.of(
                    user.id(), user.email(), user.displayName(), user.enabled(), user.mfaEnabled(), user.locked()));
        }
        return rows;
    }

    private List<List<Object>> groupRows(List<GroupView> groups) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "name", "externalId", "description"));
        for (GroupView group : groups) {
            rows.add(List.of(group.id(), group.name(), group.externalId(), group.description()));
        }
        return rows;
    }

    private List<List<Object>> roleRows(List<RoleView> roles) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "code", "name", "description"));
        for (RoleView role : roles) {
            rows.add(List.of(role.id(), role.code(), role.name(), role.description()));
        }
        return rows;
    }

    private List<List<Object>> permissionRows(List<PermissionView> permissions) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "code", "name", "description"));
        for (PermissionView permission : permissions) {
            rows.add(List.of(permission.id(), permission.code(), permission.name(), permission.description()));
        }
        return rows;
    }

    private List<List<Object>> loginHistoryRows(List<LoginHistoryView> history) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("id", "identityUserId", "result", "ipAddress", "createdAt", "failureReason"));
        for (LoginHistoryView entry : history) {
            rows.add(List.of(
                    entry.id(),
                    entry.identityUserId(),
                    entry.result(),
                    entry.ipAddress(),
                    entry.createdAt(),
                    entry.failureReason()));
        }
        return rows;
    }

    private ExportPayload csvPayload(List<List<Object>> rows, String resource) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            for (List<Object> row : rows) {
                writer.write(String.join(",", escapeCsv(row)));
                writer.write("\n");
            }
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDENTITY_EXPORT_FAILED", "CSV export failed");
        }
        return new ExportPayload(out.toByteArray(), "text/csv", "identity-" + resource + ".csv");
    }

    private ExportPayload jsonPayload(List<List<Object>> rows, String resource) {
        try {
            List<String> headers = rows.isEmpty()
                    ? List.of()
                    : rows.getFirst().stream().map(String::valueOf).toList();
            List<java.util.Map<String, Object>> records = new ArrayList<>();
            for (int i = 1; i < rows.size(); i++) {
                List<Object> row = rows.get(i);
                java.util.Map<String, Object> record = new java.util.LinkedHashMap<>();
                for (int j = 0; j < headers.size() && j < row.size(); j++) {
                    record.put(headers.get(j), row.get(j));
                }
                records.add(record);
            }
            byte[] content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(records);
            return new ExportPayload(content, "application/json", "identity-" + resource + ".json");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDENTITY_EXPORT_FAILED", "JSON export failed");
        }
    }

    private ExportPayload xlsxPayload(List<List<Object>> rows, String resource) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String sheetName = WorkbookUtil.createSafeSheetName(resource);
            Sheet sheet = workbook.createSheet(sheetName);
            for (int i = 0; i < rows.size(); i++) {
                Row row = sheet.createRow(i);
                List<Object> values = rows.get(i);
                for (int j = 0; j < values.size(); j++) {
                    Cell cell = row.createCell(j);
                    Object value = values.get(j);
                    cell.setCellValue(value == null ? "" : String.valueOf(value));
                }
            }
            workbook.write(out);
            return new ExportPayload(
                    out.toByteArray(),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "identity-" + resource + ".xlsx");
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "IDENTITY_EXPORT_FAILED", "XLSX export failed");
        }
    }

    private List<String> escapeCsv(List<Object> row) {
        return row.stream()
                .map(value -> {
                    String text = value == null ? "" : String.valueOf(value);
                    if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
                        return "\"" + text.replace("\"", "\"\"") + "\"";
                    }
                    return text;
                })
                .toList();
    }
}
