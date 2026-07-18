package ai.nova.platform.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.knowledge")
public class KnowledgeProperties {

    private long maxFileBytes = 10485760;
    private int maxExtractedCharacters = 1000000;
    private int maxChunksPerDocument = 5000;
    private int defaultChunkSize = 1000;
    private int defaultChunkOverlap = 150;
    private int maxDocumentKeyLength = 100;
    private boolean pdfEnabled = false;
    private int pdfMaxPages = 200;
    private int maxVectorCandidates = 10000;
    private int defaultTopK = 5;
    private int maximumTopK = 20;
    private double defaultMinimumScore = 0.0;
    private int maxRetrievedCharacters = 20000;
    private int maxQueryCharacters = 10000;
    private boolean retrievalEnabled = true;

    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long maxFileBytes) { this.maxFileBytes = maxFileBytes; }
    public int getMaxExtractedCharacters() { return maxExtractedCharacters; }
    public void setMaxExtractedCharacters(int maxExtractedCharacters) { this.maxExtractedCharacters = maxExtractedCharacters; }
    public int getMaxChunksPerDocument() { return maxChunksPerDocument; }
    public void setMaxChunksPerDocument(int maxChunksPerDocument) { this.maxChunksPerDocument = maxChunksPerDocument; }
    public int getDefaultChunkSize() { return defaultChunkSize; }
    public void setDefaultChunkSize(int defaultChunkSize) { this.defaultChunkSize = defaultChunkSize; }
    public int getDefaultChunkOverlap() { return defaultChunkOverlap; }
    public void setDefaultChunkOverlap(int defaultChunkOverlap) { this.defaultChunkOverlap = defaultChunkOverlap; }
    public int getMaxDocumentKeyLength() { return maxDocumentKeyLength; }
    public void setMaxDocumentKeyLength(int maxDocumentKeyLength) { this.maxDocumentKeyLength = maxDocumentKeyLength; }
    public boolean isPdfEnabled() { return pdfEnabled; }
    public void setPdfEnabled(boolean pdfEnabled) { this.pdfEnabled = pdfEnabled; }
    public int getPdfMaxPages() { return pdfMaxPages; }
    public void setPdfMaxPages(int pdfMaxPages) { this.pdfMaxPages = pdfMaxPages; }
    public int getMaxVectorCandidates() { return maxVectorCandidates; }
    public void setMaxVectorCandidates(int maxVectorCandidates) { this.maxVectorCandidates = maxVectorCandidates; }
    public int getDefaultTopK() { return defaultTopK; }
    public void setDefaultTopK(int defaultTopK) { this.defaultTopK = defaultTopK; }
    public int getMaximumTopK() { return maximumTopK; }
    public void setMaximumTopK(int maximumTopK) { this.maximumTopK = maximumTopK; }
    public double getDefaultMinimumScore() { return defaultMinimumScore; }
    public void setDefaultMinimumScore(double defaultMinimumScore) { this.defaultMinimumScore = defaultMinimumScore; }
    public int getMaxRetrievedCharacters() { return maxRetrievedCharacters; }
    public void setMaxRetrievedCharacters(int maxRetrievedCharacters) { this.maxRetrievedCharacters = maxRetrievedCharacters; }
    public int getMaxQueryCharacters() { return maxQueryCharacters; }
    public void setMaxQueryCharacters(int maxQueryCharacters) { this.maxQueryCharacters = maxQueryCharacters; }
    public boolean isRetrievalEnabled() { return retrievalEnabled; }
    public void setRetrievalEnabled(boolean retrievalEnabled) { this.retrievalEnabled = retrievalEnabled; }
}
