/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.service;

import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageBatchRequest;
import com.amazonaws.services.sqs.model.SendMessageBatchRequestEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.messages.batch.SqsBatchGenerator;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@Slf4j
@Service
public class GmcDoctorConnectionSyncService {

  private final DoctorsForDBRepository doctorsForDBRepository;
  private final RecommendationService recommendationService;
  private final SqsBatchGenerator batchGenerator;
  private final AmazonSQSClient sqsClient;
  private final ObjectMapper objectMapper;
  @Value("${app.rabbit.reval.exchange}")
  private String exchange;
  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;

  public GmcDoctorConnectionSyncService(
      DoctorsForDBRepository doctorsForDBRepository,
      RecommendationService recommendationService, SqsBatchGenerator batchGenerator,
      AmazonSQSClient sqsClient, ObjectMapper objectMapper) {
    this.sqsClient = sqsClient;
    this.doctorsForDBRepository = doctorsForDBRepository;
    this.recommendationService = recommendationService;
    this.batchGenerator = batchGenerator;
    this.objectMapper = objectMapper;
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.recommendation.syncstart}", ackMode = "NONE")
  @SchedulerLock(name = "IndexRebuildGetGmcJob")
  public void receiveMessage(final String gmcSyncStart) throws JsonProcessingException {
    log.info("Message from integration service to start gmc sync {}", gmcSyncStart);

    if (gmcSyncStart != null && gmcSyncStart.equals("gmcSyncStart")) {
      sendToSqsQueue(fetchDoctorData());
    }
  }

  private List<DoctorsForDB> fetchDoctorData() {
    List<DoctorsForDB> allGmcDoctors = doctorsForDBRepository.findAll();
    log.info("Total doctors fetched from the db: {}", allGmcDoctors.stream().count());
    return allGmcDoctors;
  }

  private void sendToSqsQueue(final List<DoctorsForDB> gmcDoctors) throws JsonProcessingException {

    var batchedDoctors = Lists.partition(gmcDoctors, 10);
    batchedDoctors.stream().forEach(batch -> {
      var messages = batchGenerator.generateBatchMessagesFromList(batch);
      SendMessageBatchRequest request = new SendMessageBatchRequest();
      request.withEntries(messages);
      request.withQueueUrl(sqsEndPoint);
      sqsClient.sendMessageBatch(request);
    });

    log.info("GMC doctors have been published to the SQS queue ");

    final var syncEnd = IndexSyncMessage.builder()
        .syncEnd(true)
        .build();
    SendMessageBatchRequestEntry syncEndMessage = new SendMessageBatchRequestEntry();
    syncEndMessage.setId("1");
    syncEndMessage.setMessageBody(objectMapper.writeValueAsString(syncEnd));
    SendMessageBatchRequest request = new SendMessageBatchRequest();
    request.withEntries(syncEndMessage);
    request.withQueueUrl(sqsEndPoint);
    sqsClient.sendMessageBatch(request);
  }
}
