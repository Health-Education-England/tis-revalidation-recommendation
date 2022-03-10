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

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@Slf4j
@Service
public class GmcDoctorConnectionSyncService {

  private final QueueMessagingTemplate queueMessagingTemplate;
  private final DoctorsForDBRepository doctorsForDBRepository;
  private final RecommendationService recommendationService;
  @Value("${app.rabbit.reval.exchange}")
  private String exchange;
  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;

  public GmcDoctorConnectionSyncService(QueueMessagingTemplate queueMessagingTemplate,
      DoctorsForDBRepository doctorsForDBRepository,
      RecommendationService recommendationService) {
    this.queueMessagingTemplate = queueMessagingTemplate;
    this.doctorsForDBRepository = doctorsForDBRepository;
    this.recommendationService = recommendationService;
  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.recommendation.syncstart}")
  public void receiveMessage(final String gmcSyncStart) {
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

  private void sendToSqsQueue(final List<DoctorsForDB> gmcDoctors) {
    gmcDoctors.stream()
        .forEach(doctor ->
            {
              final var summary = RevalidationSummaryDto.builder()
                  .doctor(doctor)
                  .gmcOutcome(getGmcOutcomeForDoctor(doctor.getGmcReferenceNumber()))
                  .build();
              final var message = IndexSyncMessage.builder()
                  .payload(summary)
                  .syncEnd(false)
                  .build();
              queueMessagingTemplate.convertAndSend(sqsEndPoint, message);
            }
        );

    log.info("GMC doctors have been published to the SQS queue ");

    final var syncEnd = IndexSyncMessage.builder()
        .syncEnd(true)
        .build();
    queueMessagingTemplate.convertAndSend(sqsEndPoint, syncEnd);
  }

  private String getGmcOutcomeForDoctor(String gmcId) {
    return recommendationService.getLatestRecommendation(gmcId).getGmcOutcome();
  }

}
