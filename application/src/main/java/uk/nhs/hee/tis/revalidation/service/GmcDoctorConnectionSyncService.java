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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.messages.publisher.ElasticsearchSyncMessagePublisher;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@Slf4j
@Service
public class GmcDoctorConnectionSyncService {

  private final ElasticsearchSyncMessagePublisher elasticsearchSyncMessagePublisher;
  private final DoctorsForDBRepository doctorsForDBRepository;
  private final RecommendationRepository recommendationRepository;
  @Value("${app.reval.essync.batchsize}")
  private int BATCH_SIZE;

  public GmcDoctorConnectionSyncService(
      ElasticsearchSyncMessagePublisher elasticsearchSyncMessagePublisher,
      DoctorsForDBRepository doctorsForDBRepository,
      RecommendationRepository recommendationRepository) {

    this.elasticsearchSyncMessagePublisher = elasticsearchSyncMessagePublisher;
    this.doctorsForDBRepository = doctorsForDBRepository;
    this.recommendationRepository = recommendationRepository;
  }

  /**
   * Receive message to begin syncing revalidation data to elasticsearch grouped by DB.
   *
   * @param gmcSyncStart - Message to initiate sync process
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.recommendation.syncstart}", ackMode = "NONE")
  @SchedulerLock(name = "IndexRebuildGetGmcJob")
  public void receiveMessage(final String gmcSyncStart) {
    log.info("Message from integration service to start gmc data sync {}", gmcSyncStart);

    if (gmcSyncStart == null || !gmcSyncStart.equals("gmcSyncStart")) {
      return;
    }
    PageRequest pageRequest = PageRequest.of(0, BATCH_SIZE);
    Page<DoctorsForDB> doctors;

    do {
      doctors = doctorsForDBRepository.findAll(pageRequest);
      List<RevalidationSummaryDto> payload = new ArrayList<>();
      doctors.forEach(doc -> {
        Optional<Recommendation> reccomendation = recommendationRepository.findFirstByGmcNumberOrderByGmcSubmissionDateDesc(
            doc.getGmcReferenceNumber());
        RevalidationSummaryDto summary = RevalidationSummaryDto.builder()
            .doctor(doc)
            .build();
        if (reccomendation.isPresent() && reccomendation.get().getOutcome() != null) {
          summary.setGmcOutcome(String.valueOf(reccomendation.get().getOutcome()));
        }
        payload.add(summary);
      });
      IndexSyncMessage syncEndPayload = IndexSyncMessage.builder().payload(payload).syncEnd(false)
          .build();
      elasticsearchSyncMessagePublisher.publishToBroker(syncEndPayload);
      pageRequest = pageRequest.next();
    } while (doctors.hasNext());

    IndexSyncMessage syncEndPayload = IndexSyncMessage.builder().payload(List.of()).syncEnd(true)
        .build();
    elasticsearchSyncMessagePublisher.publishToBroker(syncEndPayload);
  }
}
