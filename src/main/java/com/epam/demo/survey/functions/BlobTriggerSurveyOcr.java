package com.epam.demo.survey.functions;

import com.azure.ai.documentintelligence.DocumentIntelligenceClient;
import com.azure.ai.documentintelligence.DocumentIntelligenceClientBuilder;
import com.azure.ai.documentintelligence.models.AnalyzeDocumentRequest;
import com.azure.ai.documentintelligence.models.AnalyzeResult;
import com.azure.ai.documentintelligence.models.AnalyzeResultOperation;
import com.azure.ai.documentintelligence.models.DocumentAnalysisFeature;
import com.azure.ai.documentintelligence.models.DocumentKeyValueElement;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.polling.SyncPoller;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Azure Functions with Azure Blob trigger.
 */
public class BlobTriggerSurveyOcr {
    private static final String QUEUE_ENDPOINT = System.getenv("AZURE_QUEUE_ENDPOINT");
    private static final String QUEUE_SAS_TOKEN = System.getenv("AZURE_QUEUE_SAS_TOKEN");
    private static final String QUEUE_NAME = System.getenv("AZURE_QUEUE_NAME");
    private static final String DOC_INTELLIGENCE_ENDPOINT = System.getenv("AZURE_DOC_INTELLIGENCE_ENDPOINT");
    private static final String DOC_INTELLIGENCE_KEY = System.getenv("AZURE_DOC_INTELLIGENCE_KEY");

    private static final String OCR_MODEL = "prebuilt-layout";

    /**
     * This function will be invoked when a new or updated blob is detected at the specified path. The blob contents are provided as input to this function.
     */
    @FunctionName("BlobTriggerSurveyOcrJava")
    @StorageAccount("AZURE_STORAGE")
    public void run(
        @BlobTrigger(name = "file", path = "ocr-blobs/{name}", dataType = "binary") byte[] content,
        @BindingName("name") String filename,
        final ExecutionContext context
    ) {
        context.getLogger().info("Java Blob trigger function processed a request for" + filename + "\n  Size: " + content.length + " Bytes");

        // Create a Document Intelligence client
        DocumentIntelligenceClient client = new DocumentIntelligenceClientBuilder()
                .endpoint(DOC_INTELLIGENCE_ENDPOINT)
                .credential(new AzureKeyCredential(DOC_INTELLIGENCE_KEY))
                .buildClient();

        SyncPoller<AnalyzeResultOperation, AnalyzeResult> analyzeLayoutResultPoller =
                client.beginAnalyzeDocument(OCR_MODEL, null,
                        null,
                        null,
                        Collections.singletonList(DocumentAnalysisFeature.KEY_VALUE_PAIRS),
                        null,
                        null,
                        null,
                        new AnalyzeDocumentRequest().setBase64Source(content));

        // Analyze the document
        AnalyzeResult analyzeLayoutResult = analyzeLayoutResultPoller.getFinalResult();

        Map<String, Object> keyValuePairs = new HashMap<>();

        analyzeLayoutResult.getKeyValuePairs().forEach(
                keyValuePair -> {
                    DocumentKeyValueElement key = keyValuePair.getKey();
                    DocumentKeyValueElement value = keyValuePair.getValue();
                    if (key != null && value != null) {
                        String keyContent = key.getContent();
                        String propertyValue = value.getContent();
                        if (":selected:".equals(propertyValue)) {
                            keyValuePairs.put(keyContent, true);
                        } else if (":unselected:".equals(propertyValue)) {
                            keyValuePairs.put(keyContent, false);
                        } else {
                            keyValuePairs.put(keyContent, propertyValue);
                        }
                    }
                }
        );

        StringBuilder message = new StringBuilder();

        analyzeLayoutResult.getParagraphs().forEach(
                paragraph -> {
                    String textContent = paragraph.getContent();
                    if (textContent != null && textContent.length() > 40) {
                        int separatorIndex = textContent.indexOf("Message:");
                        if(separatorIndex > 0) {
                            message.insert(0, textContent.substring(separatorIndex + 8).trim());
                        } else if (!textContent.contains(":")) {
                            message.append(" ").append(textContent.trim());
                        }
                    }
                }
        );

        if (!message.isEmpty()) {
            keyValuePairs.put("Message", message.toString());
        }

        // Convert to JSON
        try {
            String jsonData = new ObjectMapper().writeValueAsString(keyValuePairs);

            // Send to Azure Queue
            QueueClient queueClient = new QueueClientBuilder()
                    .endpoint(QUEUE_ENDPOINT)
                    .sasToken(QUEUE_SAS_TOKEN)
                    .queueName(QUEUE_NAME)
                    .buildClient();

            queueClient.sendMessage(jsonData);
            context.getLogger().info("Message sent to queue: " + jsonData);
        } catch (JsonProcessingException e) {
            context.getLogger().severe("Failed to process file and send to queue: " + e.getMessage());
        }
    }
}
