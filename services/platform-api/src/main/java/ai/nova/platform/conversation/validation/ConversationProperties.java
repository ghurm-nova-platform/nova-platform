package ai.nova.platform.conversation.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.conversation")
public class ConversationProperties {

    private int maxContextMessages = 20;
    private int maxContextCharacters = 30000;
    private int maxMessageCharacters = 10000;
    private int maxTitleLength = 255;
    private boolean storeSystemMessage = false;

    public int getMaxContextMessages() {
        return maxContextMessages;
    }

    public void setMaxContextMessages(int maxContextMessages) {
        this.maxContextMessages = maxContextMessages;
    }

    public int getMaxContextCharacters() {
        return maxContextCharacters;
    }

    public void setMaxContextCharacters(int maxContextCharacters) {
        this.maxContextCharacters = maxContextCharacters;
    }

    public int getMaxMessageCharacters() {
        return maxMessageCharacters;
    }

    public void setMaxMessageCharacters(int maxMessageCharacters) {
        this.maxMessageCharacters = maxMessageCharacters;
    }

    public int getMaxTitleLength() {
        return maxTitleLength;
    }

    public void setMaxTitleLength(int maxTitleLength) {
        this.maxTitleLength = maxTitleLength;
    }

    public boolean isStoreSystemMessage() {
        return storeSystemMessage;
    }

    public void setStoreSystemMessage(boolean storeSystemMessage) {
        this.storeSystemMessage = storeSystemMessage;
    }
}
