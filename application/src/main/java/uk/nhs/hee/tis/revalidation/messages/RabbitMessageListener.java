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

package uk.nhs.hee.tis.revalidation.messages;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.messages.receiver.EsRebuildMessageReceiver;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;

@Slf4j
@Component
public class RabbitMessageListener {

  @Autowired
  private DoctorsForDBService doctorsForDBService;

  @Autowired
  private RecommendationStatusCheckUpdatedMessageHandler recommendationStatusCheckUpdatedMessageHandler;

  @Autowired
  private EsRebuildMessageReceiver esRebuildMessageReceiver;

  @RabbitListener(queues = "${app.rabbit.queue}")
  public void receivedMessage(final DoctorsForDbDto gmcDoctor) {
    try {
      log.debug("DoctorsForDbDto message received from rabbit: {}", gmcDoctor);
      doctorsForDBService.updateTrainee(gmcDoctor);
    } catch (Exception exception) {
      log.warn("Rejecting message for failed doctor update", exception);
      throw new AmqpRejectAndDontRequeueException(exception);
    }
  }

  @RabbitListener(queues = "${app.rabbit.connection.queue}")
  public void receiveRemoveDoctorDesignatedBodyCodeMessage(final ConnectionMessageDto message) {
    try {
      log.info("Message received to update designated body code from rabbit, Message: {}", message);
      doctorsForDBService.removeDesignatedBodyCode(message);
    } catch (Exception exception) {
      log.warn("Rejecting message for failed connection removal", exception);
      throw new AmqpRejectAndDontRequeueException(exception);
    }

  }

  @RabbitListener(queues = "${app.rabbit.reval.queue.recommendationStatusCheck.updated}")
  public void receiveMessageForRecommendationStatusUpdate(
      final RecommendationStatusCheckDto recommendationStatusCheckDto) {
    try {
      log.info(
          "Message received to update recommendation status, Message: {}",
          recommendationStatusCheckDto);
      recommendationStatusCheckUpdatedMessageHandler
          .updateRecommendationAndTisStatus(recommendationStatusCheckDto);
    } catch (Exception exception) {
      log.warn("Rejecting message for failed recommendation status update", exception);
      throw new AmqpRejectAndDontRequeueException(exception);
    }
  }

  /**
   * get trainee from Master index then update recommendation indexes.
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.indexrebuildgetmastercommand.requested}")
  @SchedulerLock(name = "IndexRebuildGetMasterJob")
  public void receiveMessageGetMaster(final String getMaster) throws IOException {
    log.info("Message received to get trainee record from Master index.");
    esRebuildMessageReceiver.handleMessage(getMaster);
  }
}
