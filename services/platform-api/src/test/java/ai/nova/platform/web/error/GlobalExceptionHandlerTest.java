package ai.nova.platform.web.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import ai.nova.platform.web.correlation.CorrelationIdFilter;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @BeforeEach
    void setUp() {
        MDC.put(CorrelationIdFilter.CORRELATION_ID_MDC_KEY, "corr-test-1");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void notFoundProducesStandardErrorContract() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/missing");

        ResponseEntity<ApiErrorResponse> response = handler.handleNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/missing"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.status()).isEqualTo(404);
        assertThat(body.error()).isEqualTo("Not Found");
        assertThat(body.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(body.message()).isEqualTo("The requested resource was not found");
        assertThat(body.path()).isEqualTo("/api/v1/missing");
        assertThat(body.correlationId()).isEqualTo("corr-test-1");
        assertThat(body.timestamp()).isNotNull();
        assertThat(body.details()).isNull();
    }

    @Test
    void unexpectedExceptionHidesInternalDetails() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/boom");

        ResponseEntity<ApiErrorResponse> response = handler.handleUnexpected(
                new RuntimeException("secret stack detail"),
                request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.message()).doesNotContain("secret");
    }
}
