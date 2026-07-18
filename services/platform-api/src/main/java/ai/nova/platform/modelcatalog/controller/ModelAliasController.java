package ai.nova.platform.modelcatalog.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.modelcatalog.service.ModelAliasService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/ai-model-aliases")
public class ModelAliasController {

    private final ModelAliasService aliasService;

    public ModelAliasController(ModelAliasService aliasService) {
        this.aliasService = aliasService;
    }

    @DeleteMapping("/{aliasId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID aliasId, @AuthenticationPrincipal AuthenticatedUser user) {
        aliasService.delete(aliasId, user);
    }
}
