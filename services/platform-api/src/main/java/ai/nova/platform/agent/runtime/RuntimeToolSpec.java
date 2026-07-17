package ai.nova.platform.agent.runtime;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provider-neutral tool specification exposed to the runtime.
 * Does not include executor class names or secrets.
 */
public record RuntimeToolSpec(
        String toolKey,
        String name,
        String description,
        JsonNode inputSchema) {
}
