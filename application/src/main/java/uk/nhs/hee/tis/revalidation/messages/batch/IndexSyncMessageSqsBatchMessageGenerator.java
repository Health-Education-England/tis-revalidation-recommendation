package uk.nhs.hee.tis.revalidation.messages.batch;

import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.service.RecommendationService;

public class IndexSyncMessageSqsBatchMessageGenerator implements
    BatchMessageGenerator<List<SendMessageBatchRequestEntry>, IndexSyncMessage> {

  private RecommendationService recommendationService;
  private ObjectMapper objectMapper;

  public IndexSyncMessageSqsBatchMessageGenerator(RecommendationService recommendationService,
      ObjectMapper objectMapper) {
    this.recommendationService = recommendationService;
    this.objectMapper = objectMapper;
  }

  @Override
  public List<SendMessageBatchRequestEntry> generateBatchMessages(
      List<IndexSyncMessage> payloads) {
    List<SendMessageBatchRequestEntry> batchMessages = new ArrayList<>();
    payloads.stream().forEach(payload -> {

    });
    var batchMessage = new SendMessageBatchRequestEntry();
    batchMessage.setId();
    try {
      batchMessage.setMessageBody(objectMapper.writeValueAsString(message));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    batchMessages.add(batchMessage);


    return batchMessages;
}
}
