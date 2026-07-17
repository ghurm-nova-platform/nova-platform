package ai.nova.platform.knowledge.extractor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.web.error.ApiException;

class PlainTextExtractorTest {

    private final KnowledgeProperties properties = new KnowledgeProperties();
    private final PlainTextExtractor extractor = new PlainTextExtractor(properties);

    @Test
    void normalizesLineEndingsAndRejectsBinary() {
        String text = extractor.extract("hello\r\nworld".getBytes(StandardCharsets.UTF_8), "a.txt", "text/plain");
        assertThat(text).isEqualTo("hello\nworld");

        byte[] binary = new byte[200];
        for (int i = 0; i < binary.length; i++) {
            binary[i] = (byte) (i % 32);
        }
        assertThatThrownBy(() -> extractor.extract(binary, "b.bin", "text/plain"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("DOCUMENT_BINARY");
    }
}
