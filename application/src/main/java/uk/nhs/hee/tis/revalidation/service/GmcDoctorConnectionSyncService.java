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

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.entity.RevalidationSummary;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.messages.publisher.ElasticsearchSyncMessagePublisher;
import uk.nhs.hee.tis.revalidation.repository.RevalidationSummaryRepository;

@Slf4j
@Service
public class GmcDoctorConnectionSyncService {

  private final RevalidationSummaryRepository revalidationSummaryRepository;
  private final ElasticsearchSyncMessagePublisher elasticsearchSyncMessagePublisher;
  @Value("${app.gmc.designatedBodies}")
  private List<String> designatedBodies;

  public GmcDoctorConnectionSyncService(
      RevalidationSummaryRepository revalidationSummaryRepository,
      ElasticsearchSyncMessagePublisher elasticsearchSyncMessagePublisher) {
    this.revalidationSummaryRepository = revalidationSummaryRepository;
    this.elasticsearchSyncMessagePublisher = elasticsearchSyncMessagePublisher;
  }

  /**
   * Receive message to begin syncing revalidation data to elasticsearch grouped by DB.
   *
   * @param gmcSyncStart - Message to initiate sync process
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.recommendation.syncstart}", ackMode = "NONE")
  @SchedulerLock(name = "IndexRebuildGetGmcJob")
  public void receiveMessage(final String gmcSyncStart) {
    log.info("Message from integration service to start gmc sync {}", gmcSyncStart);

    if (gmcSyncStart == null || !gmcSyncStart.equals("gmcSyncStart")) {
      return;
    }
    designatedBodies.forEach(db -> {
      log.info(db);
      IndexSyncMessage connectedPayload = IndexSyncMessage.builder()
          .payload(getRevalidationSummaryDtoListForDesignatedBody(db)).syncEnd(false).build();
      log.info(connectedPayload.toString());
      elasticsearchSyncMessagePublisher.publishToBroker(connectedPayload);
    });
    //find disconnected doctors
    IndexSyncMessage disconnectedPayload = IndexSyncMessage.builder()
        .payload(getRevalidationSummaryDtoListForDesignatedBody("")).syncEnd(false).build();
    log.info(disconnectedPayload.toString());
    elasticsearchSyncMessagePublisher.publishToBroker(disconnectedPayload);

    IndexSyncMessage syncEndPayload = IndexSyncMessage.builder().payload(List.of()).syncEnd(true)
        .build();
    elasticsearchSyncMessagePublisher.publishToBroker(syncEndPayload);
  }

  private List<RevalidationSummary> getRevalidationSummaryDtoListForDesignatedBody(String db) {
    log.info(db);
    return db.isBlank() ?
        revalidationSummaryRepository.findByDesignatedBodyCodeIsNull()
        : revalidationSummaryRepository.findByDesignatedBodyCode(db);
  }

}
