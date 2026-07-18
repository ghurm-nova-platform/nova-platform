package ai.nova.platform.modelgateway.gateway;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.web.error.ApiException;

@Component
public class ModelGatewayInputValidator {

    private final ModelGatewayProperties properties;

    public ModelGatewayInputValidator(ModelGatewayProperties properties) {
        this.properties = properties;
    }

    public void validate(ModelGatewayRequest request) {
        if (request.systemPrompt() != null
                && request.systemPrompt().length() > properties.getMaxSystemCharacters()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INPUT_TOO_LARGE", "System prompt too large");
        }
        if (request.messages() != null && request.messages().size() > properties.getMaxMessages()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INPUT_TOO_LARGE", "Too many messages");
        }
        int total = countInputCharacters(request);
        if (total > properties.getMaxInputCharacters()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INPUT_TOO_LARGE", "Input too large");
        }
        if (request.messages() != null) {
            for (RuntimeMessage message : request.messages()) {
                if (message.content() != null
                        && message.content().length() > properties.getMaxMessageCharacters()) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "INPUT_TOO_LARGE", "Message too large");
                }
            }
        }
    }

    public int countInputCharacters(ModelGatewayRequest request) {
        int total = request.systemPrompt() != null ? request.systemPrompt().length() : 0;
        if (request.messages() != null) {
            for (RuntimeMessage message : request.messages()) {
                if (message.content() != null) {
                    total += message.content().length();
                }
            }
        }
        return total;
    }
}
