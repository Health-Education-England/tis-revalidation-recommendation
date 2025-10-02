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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.collections4.ListUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.batchmanager.SqsAsyncBatchManager;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@Slf4j
@Service
public class GmcDoctorConnectionSyncService {

  private static final int BATCH_SIZE = 100;

  private final SqsAsyncBatchManager batchManager;
  private final DoctorsForDBRepository doctorsForDBRepository;
  private final RecommendationService recommendationService;
  private final ObjectMapper objectMapper;
  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;

  public GmcDoctorConnectionSyncService(SqsAsyncBatchManager batchManager,
      ObjectMapper objectMapper,
      DoctorsForDBRepository doctorsForDBRepository, RecommendationService recommendationService) {
    this.batchManager = batchManager;
    this.objectMapper = objectMapper;
    this.doctorsForDBRepository = doctorsForDBRepository;
    this.recommendationService = recommendationService;
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.recommendation.syncstart}", ackMode = "NONE")
  @SchedulerLock(name = "IndexRebuildGetGmcJob")
  public void receiveMessage(final String gmcSyncStart) {
    log.info("Message from integration service to start gmc sync {}", gmcSyncStart);

    if (gmcSyncStart == null || !gmcSyncStart.equals("gmcSyncStart")) {
      return;
    }

    ListUtils.partition(doctorsForDBRepository.findAll(), BATCH_SIZE).forEach(batch -> {
      List<CompletableFuture<?>> futures = batch.stream()
          .filter(Objects::nonNull)
          .map(doctor -> {
            SendMessageRequest req = convertToMessage(doctor);
            if (req == null) {
              return CompletableFuture.completedFuture(null);
            }

            return batchManager.sendMessage(req)
                .handle((resp, ex) -> {
                  if (ex != null) {
                    log.error("Failed to send doctor {}", doctor.getGmcReferenceNumber(), ex);
                  }
                  return resp;
                });
          }).toList();
      // Wait for the batch of messages to be sent
      CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
      log.info("Batch of {} doctors sent", batch.size());
    });

    log.info("GMC doctors have been published to the SQS queue.");
      final var syncEnd = IndexSyncMessage.builder().syncEnd(true).build();
      try {
        String body = objectMapper.writeValueAsString(syncEnd);
        batchManager.sendMessage(
            SendMessageRequest.builder().queueUrl(sqsEndPoint).messageBody(body).build()
        ).handle((resp, ex) -> {
          if (ex != null) {
            log.error("Failed to send syncEnd message", ex);
          } else {
            log.info("Sent syncEnd message");
          }
          return null;
        });
      } catch (JsonProcessingException e) {
        log.error(
            "Unable to convert 'syncEnd' message. Downstream services need notification.", e);
      }
  }

  private SendMessageRequest convertToMessage(DoctorsForDB doctor) {
    TraineeRecommendationRecordDto recommendation =
        recommendationService.getLatestRecommendation(doctor.getGmcReferenceNumber());

    final var summary = RevalidationSummaryDto.builder()
        .doctor(doctor)
        .gmcOutcome(recommendation.getGmcOutcome())
        .build();
    final var message = IndexSyncMessage.builder()
        .payload(summary)
        .syncEnd(false)
        .build();
    try {
      return SendMessageRequest.builder().queueUrl(sqsEndPoint)
          .messageBody(objectMapper.writeValueAsString(message)).build();
    } catch (JsonProcessingException e) {
      log.error("Unable to construct message for doctor '{}'", doctor.getGmcReferenceNumber(), e);
      return null;
    }
  }

}
