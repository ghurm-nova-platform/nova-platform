package ai.nova.platform.tool.executor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class ToolExecutorRegistry {

    private final Map<String, ToolExecutor> executorsByKey;

    public ToolExecutorRegistry(List<ToolExecutor> executors) {
        Map<String, ToolExecutor> map = new LinkedHashMap<>();
        for (ToolExecutor executor : executors) {
            String key = executor.executorKey();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException("Tool executor returned blank key: " + executor.getClass().getName());
            }
            if (map.containsKey(key)) {
                throw new IllegalStateException("Duplicate tool executor key: " + key);
            }
            map.put(key, executor);
        }
        this.executorsByKey = Collections.unmodifiableMap(map);
    }

    public Optional<ToolExecutor> find(String executorKey) {
        return Optional.ofNullable(executorsByKey.get(executorKey));
    }

    public ToolExecutor require(String executorKey) {
        return find(executorKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown executor key: " + executorKey));
    }

    public Set<String> allowedKeys() {
        return executorsByKey.keySet();
    }

    public boolean isRegistered(String executorKey) {
        return executorsByKey.containsKey(executorKey);
    }
}
