package ai.nova.platform.knowledge.chunking;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.web.error.ApiException;

@Component
public class ParagraphAwareTextChunker {

    private static final Pattern PARAGRAPH_SPLIT = Pattern.compile("\\n\\s*\\n+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[.!?])\\s+");

    private final KnowledgeProperties properties;

    public ParagraphAwareTextChunker(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public List<TextChunk> chunk(String text, int chunkSize, int chunkOverlap) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be positive");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new IllegalArgumentException("chunkOverlap must be >= 0 and < chunkSize");
        }

        List<Segment> segments = buildSegments(text);
        List<TextChunk> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int currentStart = -1;
        int absoluteOffset = 0;

        for (Segment segment : segments) {
            String piece = segment.text();
            if (piece.isEmpty()) {
                absoluteOffset = segment.end();
                continue;
            }
            if (piece.length() > chunkSize) {
                flush(current, currentStart, chunks);
                current.setLength(0);
                currentStart = -1;
                addOversized(piece, segment.start(), chunkSize, chunkOverlap, chunks);
                absoluteOffset = segment.end();
                continue;
            }
            if (current.length() == 0) {
                current.append(piece);
                currentStart = segment.start();
            } else if (current.length() + 1 + piece.length() <= chunkSize) {
                current.append('\n').append(piece);
            } else {
                flush(current, currentStart, chunks);
                String overlapSeed = overlapText(current.toString(), chunkOverlap);
                current.setLength(0);
                if (!overlapSeed.isEmpty()) {
                    current.append(overlapSeed);
                    currentStart = Math.max(0, segment.start() - overlapSeed.length());
                    if (current.length() + 1 + piece.length() <= chunkSize) {
                        current.append('\n').append(piece);
                    } else {
                        flush(current, currentStart, chunks);
                        current.setLength(0);
                        current.append(piece);
                        currentStart = segment.start();
                    }
                } else {
                    current.append(piece);
                    currentStart = segment.start();
                }
            }
            absoluteOffset = segment.end();
        }
        flush(current, currentStart, chunks);

        if (chunks.size() > properties.getMaxChunksPerDocument()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_TOO_MANY_CHUNKS",
                    "Document exceeds maximum chunks per document");
        }
        return List.copyOf(chunks);
    }

    private void addOversized(
            String piece, int start, int chunkSize, int chunkOverlap, List<TextChunk> chunks) {
        List<String> sentences = splitSentences(piece);
        StringBuilder current = new StringBuilder();
        int currentStart = start;
        int cursor = start;
        for (String sentence : sentences) {
            if (sentence.length() > chunkSize) {
                flush(current, currentStart, chunks);
                current.setLength(0);
                int local = 0;
                while (local < sentence.length()) {
                    int end = Math.min(local + chunkSize, sentence.length());
                    String slice = sentence.substring(local, end);
                    int absStart = cursor + local;
                    chunks.add(toChunk(chunks.size(), slice, absStart, absStart + slice.length()));
                    if (end >= sentence.length()) {
                        break;
                    }
                    local = Math.max(end - chunkOverlap, local + 1);
                }
                cursor += sentence.length();
                currentStart = cursor;
                continue;
            }
            if (current.length() == 0) {
                current.append(sentence);
                currentStart = cursor;
            } else if (current.length() + 1 + sentence.length() <= chunkSize) {
                current.append(' ').append(sentence);
            } else {
                flush(current, currentStart, chunks);
                String overlapSeed = overlapText(current.toString(), chunkOverlap);
                current.setLength(0);
                if (!overlapSeed.isEmpty()) {
                    current.append(overlapSeed);
                    if (current.length() + 1 + sentence.length() <= chunkSize) {
                        current.append(' ').append(sentence);
                    } else {
                        flush(current, currentStart, chunks);
                        current.setLength(0);
                        current.append(sentence);
                        currentStart = cursor;
                    }
                } else {
                    current.append(sentence);
                    currentStart = cursor;
                }
            }
            cursor += sentence.length();
        }
        flush(current, currentStart, chunks);
    }

    private List<Segment> buildSegments(String text) {
        String[] parts = PARAGRAPH_SPLIT.split(text);
        List<Segment> segments = new ArrayList<>();
        int searchFrom = 0;
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            int idx = text.indexOf(part, searchFrom);
            if (idx < 0) {
                idx = searchFrom;
            }
            segments.add(new Segment(part.trim(), idx, idx + part.length()));
            searchFrom = idx + part.length();
        }
        if (segments.isEmpty() && !text.isBlank()) {
            String trimmed = text.trim();
            int idx = text.indexOf(trimmed);
            segments.add(new Segment(trimmed, Math.max(0, idx), Math.max(0, idx) + trimmed.length()));
        }
        return segments;
    }

    private static List<String> splitSentences(String text) {
        String[] parts = SENTENCE_SPLIT.split(text);
        List<String> sentences = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                sentences.add(trimmed);
            }
        }
        if (sentences.isEmpty() && !text.isBlank()) {
            sentences.add(text.trim());
        }
        return sentences;
    }

    private void flush(StringBuilder current, int start, List<TextChunk> chunks) {
        if (current == null || current.length() == 0) {
            return;
        }
        String content = current.toString().trim();
        if (content.isEmpty()) {
            return;
        }
        int end = start + content.length();
        chunks.add(toChunk(chunks.size(), content, Math.max(0, start), Math.max(end, start + 1)));
    }

    private static String overlapText(String text, int overlap) {
        if (overlap <= 0 || text.isEmpty()) {
            return "";
        }
        if (text.length() <= overlap) {
            return text;
        }
        return text.substring(text.length() - overlap);
    }

    private static TextChunk toChunk(int index, String content, int start, int end) {
        return new TextChunk(index, content, start, end, sha256(content));
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    private record Segment(String text, int start, int end) {
    }
}
