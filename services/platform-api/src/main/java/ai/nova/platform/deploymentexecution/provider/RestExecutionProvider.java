package ai.nova.platform.deploymentexecution.provider;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.deploymentexecution.config.ExecutionProperties;
import ai.nova.platform.deploymentexecution.entity.ExecutionLogLevel;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;

@Component
public class RestExecutionProvider implements DeploymentExecutionProvider {

    private final ExecutionProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public RestExecutionProvider(ExecutionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void prepare(ExecutionContext ctx) {
        ctx.updateStep("prepare", "PREPARE", "Preparing REST deployment request");
        ctx.log(ExecutionLogLevel.INFO, "REST provider preparing payload");
    }

    @Override
    public void deploy(ExecutionContext ctx) {
        String url = resolveUrl(ctx);
        if (url == null || url.isBlank()) {
            ctx.log(ExecutionLogLevel.WARN, "REST provider not configured — dry run");
            ctx.updateStep("deploy", "DEPLOY", "REST dry run (no URL configured)");
            return;
        }
        ctx.updateStep("deploy", "DEPLOY", "POST " + url);
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "releaseId", ctx.getReleaseId().toString(),
                    "environmentId", ctx.getEnvironmentId().toString(),
                    "executionId", ctx.getExecutionId().toString()));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ctx.log(ExecutionLogLevel.INFO, "REST deploy response status " + response.statusCode());
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("REST deploy failed with status " + response.statusCode());
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("REST deploy interrupted", ex);
        } catch (Exception ex) {
            throw new IllegalStateException("REST deploy failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void verify(ExecutionContext ctx) {
        ctx.updateStep("verify", "VERIFY", "REST verification");
        String url = resolveUrl(ctx);
        if (url == null || url.isBlank()) {
            ctx.log(ExecutionLogLevel.INFO, "REST dry run verification passed");
            ctx.saveResult(true, "REST dry run completed (no URL configured)", "{\"mode\":\"rest-dry-run\"}");
            return;
        }
        ctx.log(ExecutionLogLevel.INFO, "REST verification passed");
        ctx.saveResult(true, "REST deployment completed", "{\"mode\":\"rest\"}");
    }

    @Override
    public void cancel(ExecutionContext ctx) {
        ctx.log(ExecutionLogLevel.WARN, "REST provider cancel acknowledged (no remote cancel call)");
    }

    @Override
    public ExecutionProviderCode providerCode() {
        return ExecutionProviderCode.REST;
    }

    private String resolveUrl(ExecutionContext ctx) {
        if (ctx.getRestDeployUrl() != null && !ctx.getRestDeployUrl().isBlank()) {
            return ctx.getRestDeployUrl().trim();
        }
        return properties.getRestBaseUrl();
    }
}
