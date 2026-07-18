package ai.nova.platform.modelgateway.secrets.vault;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretDtos.CreateProviderSecretRequest;
import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretDtos.ProviderSecretResponse;
import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretDtos.RotateProviderSecretRequest;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/provider-secrets")
public class ProviderSecretController {

    private final ProviderSecretService secretService;

    public ProviderSecretController(ProviderSecretService secretService) {
        this.secretService = secretService;
    }

    @GetMapping
    public Page<ProviderSecretResponse> list(
            @RequestParam(required = false) ProviderSecretStatus status,
            @RequestParam(required = false) AiProviderType providerType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return secretService.list(status, providerType, search, pageable, user);
    }

    @GetMapping("/{secretId}")
    public ProviderSecretResponse get(
            @PathVariable UUID secretId, @AuthenticationPrincipal AuthenticatedUser user) {
        return secretService.get(secretId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderSecretResponse create(
            @Valid @RequestBody CreateProviderSecretRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return secretService.create(request, user);
    }

    @PostMapping("/{secretId}/rotate")
    public ProviderSecretResponse rotate(
            @PathVariable UUID secretId,
            @Valid @RequestBody RotateProviderSecretRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return secretService.rotate(secretId, request, user);
    }

    @PostMapping("/{secretId}/revoke")
    public ProviderSecretResponse revoke(
            @PathVariable UUID secretId, @AuthenticationPrincipal AuthenticatedUser user) {
        return secretService.revoke(secretId, user);
    }
}
