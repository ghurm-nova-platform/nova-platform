package ai.nova.platform.web.error;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API error response contract for platform-api.
 *
 * <pre>
 * {
 *   "timestamp": "2026-07-16T20:00:00Z",
 *   "status": 404,
 *   "error": "Not Found",
 *   "code": "RESOURCE_NOT_FOUND",
 *   "message": "Human-readable summary",
 *   "path": "/api/v1/example",
 *   "correlationId": "uuid",
 *   "details": [ { "field": "name", "message": "must not be blank" } ]
 * }
 * </pre>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        String correlationId,
        List<FieldErrorDetail> details
) {

    public record FieldErrorDetail(String field, String message) {
    }
}
