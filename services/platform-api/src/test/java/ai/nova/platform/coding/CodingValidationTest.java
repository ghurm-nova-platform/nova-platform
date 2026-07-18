package ai.nova.platform.coding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.coding.config.CodingProperties;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.ParsedCodingOutput;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.CodingArtifactValidator;
import ai.nova.platform.coding.service.CodingJsonParser;
import ai.nova.platform.web.error.ApiException;

class CodingValidationTest {

    private CodingArtifactValidator validator;
    private CodingJsonParser parser;

    @BeforeEach
    void setUp() {
        CodingProperties properties = new CodingProperties();
        validator = new CodingArtifactValidator(properties);
        parser = new CodingJsonParser(new ObjectMapper());
    }

    @Test
    void rejectsMissingArtifacts() {
        assertThatThrownBy(() -> validator.validate(new ParsedCodingOutput("x", List.of())))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_EMPTY_ARTIFACTS");
    }

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> validator.validate(new ParsedCodingOutput(
                        "x",
                        List.of(new GeneratedArtifactDraft(
                                ArtifactType.SOURCE_FILE,
                                ArtifactLanguage.JAVA,
                                "src/A.java",
                                "A.java",
                                "  ")))))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_EMPTY_CONTENT");
    }

    @Test
    void rejectsDuplicatePaths() {
        GeneratedArtifactDraft a = new GeneratedArtifactDraft(
                ArtifactType.SOURCE_FILE, ArtifactLanguage.JAVA, "src/A.java", "A.java", "class A {}");
        GeneratedArtifactDraft b = new GeneratedArtifactDraft(
                ArtifactType.TEST, ArtifactLanguage.JAVA, "src/A.java", "A.java", "class B {}");
        assertThatThrownBy(() -> validator.validate(new ParsedCodingOutput("x", List.of(a, b))))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_DUPLICATE_PATH");
    }

    @Test
    void rejectsInvalidLanguage() {
        assertThatThrownBy(() -> parser.parse("""
                {"summary":"x","artifacts":[{"type":"SOURCE_FILE","language":"COBOL","path":"a.cbl","filename":"a.cbl","content":"x"}]}
                """))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_INVALID_LANGUAGE");
    }

    @Test
    void rejectsInvalidArtifactType() {
        assertThatThrownBy(() -> parser.parse("""
                {"summary":"x","artifacts":[{"type":"BINARY","language":"JAVA","path":"a.java","filename":"a.java","content":"x"}]}
                """))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_INVALID_ARTIFACT_TYPE");
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(() -> validator.validate(new ParsedCodingOutput(
                        "x",
                        List.of(new GeneratedArtifactDraft(
                                ArtifactType.SOURCE_FILE,
                                ArtifactLanguage.JAVA,
                                "../secrets/A.java",
                                "A.java",
                                "class A {}")))))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_PATH_TRAVERSAL");
    }

    @Test
    void rejectsAbsolutePaths() {
        assertThatThrownBy(() -> validator.validate(new ParsedCodingOutput(
                        "x",
                        List.of(new GeneratedArtifactDraft(
                                ArtifactType.SOURCE_FILE,
                                ArtifactLanguage.JAVA,
                                "/tmp/A.java",
                                "A.java",
                                "class A {}")))))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_ABSOLUTE_PATH");
    }

    @Test
    void rejectsBinaryContent() {
        assertThatThrownBy(() -> validator.validate(new ParsedCodingOutput(
                        "x",
                        List.of(new GeneratedArtifactDraft(
                                ArtifactType.SOURCE_FILE,
                                ArtifactLanguage.JAVA,
                                "src/A.java",
                                "A.java",
                                "class A {\0}")))))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_BINARY_CONTENT");
    }

    @Test
    void acceptsValidArtifacts() {
        ParsedCodingOutput output = parser.parse("""
                {
                  "summary":"Implemented login page",
                  "artifacts":[
                    {"type":"SOURCE_FILE","language":"JAVA","path":"src/main/java/LoginService.java","filename":"LoginService.java","content":"class LoginService {}"},
                    {"type":"TEST","language":"JAVA","path":"src/test/java/LoginServiceTest.java","filename":"LoginServiceTest.java","content":"class LoginServiceTest {}"}
                  ]
                }
                """);
        validator.validate(output);
        assertThat(output.artifacts()).hasSize(2);
        assertThat(output.summary()).isEqualTo("Implemented login page");
    }
}
