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

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.mapper.RecommendationViewMapper;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;
import uk.nhs.hee.tis.revalidation.service.RecommendationElasticSearchService;

@Slf4j
@Component
public class RabbitMessageListener {

  @Autowired
  private DoctorsForDBService doctorsForDBService;

  @Autowired
  private RecommendationStatusCheckUpdatedMessageHandler recommendationStatusCheckUpdatedMessageHandler;

  @Autowired
  private RecommendationViewMapper recommendationViewMapper;

  @Autowired
  private RecommendationElasticSearchService recommendationElasticSearchService;

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
  public void receiveUpdateDoctorConnectionMessage(final ConnectionMessageDto message) {
    try {
      log.info("Message received to update designated body code from rabbit, Message: {}", message);
      doctorsForDBService.updateDoctorConnection(message);
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
   * get updated doctors from Master index then update recommendation indexes.
   */
  @RabbitListener(queues = "${app.rabbit.reval.queue.masterdoctorview.updated.recommendation}",
      ackMode = "NONE")
  public void receiveUpdateMessageFromMasterDoctorView(
      final MasterDoctorViewDto masterDoctorViewDto) {
    try {
      if (masterDoctorViewDto.getGmcReferenceNumber() == null &&
          masterDoctorViewDto.getTcsPersonId() == null) {
        throw new RecommendationException(
            "Received update message MasterDoctorView with "
                + "null tis personId and null gmc reference number"
        );
      }

      RecommendationView recommendationView =
          recommendationViewMapper.mapMasterDoctorViewDtoToRecommendationView(masterDoctorViewDto);

      if (!recommendationElasticSearchService.removeTisInfo(recommendationView)) {
        log.info("Message received from Master index to update doctor record. gmcRefNo: {}",
            masterDoctorViewDto.getGmcReferenceNumber());
        recommendationElasticSearchService.saveRecommendationViews(recommendationView);
      }
    } catch (Exception exception) {
      log.warn("Rejecting message for failed recommendation status update", exception);
      throw new AmqpRejectAndDontRequeueException(exception);
    }
  }
}
