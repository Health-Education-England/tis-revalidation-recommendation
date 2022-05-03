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

import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.fromCode;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.UNDER_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.DEFER;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.convertDateInGmcFormat;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import uk.nhs.hee.tis.gmc.client.generated.CheckRecommendationStatus;
import uk.nhs.hee.tis.gmc.client.generated.CheckRecommendationStatusRequest;
import uk.nhs.hee.tis.gmc.client.generated.CheckRecommendationStatusResponse;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationV2;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationV2Request;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationV2Response;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;

@Slf4j
@Service
public class GmcClientService {

  private static final String INTERNAL_USER_ID = "InternalUserId";
  private static final String TRY_RECOMMENDATION_V2 = "TryRecommendationV2";
  private static final String CHECK_RECOMMENDATION_STATUS = "CheckRecommendationStatus";

  @Value("${app.gmc.url}")
  private String gmcConnectUrl;

  @Value("${app.gmc.gmcUsername}")
  private String gmcUserName;

  @Value("${app.gmc.gmcPassword}")
  private String gmcPassword;

  @Value("${app.gmc.soapActionBase}")
  private String gmcSoapBaseAction;

  @Value("${app.rabbit.reval.exchange}")
  private String revalExchange;

  @Value("${app.rabbit.reval.routingKey.recommendationstatuscheck.requested}")
  private String revalRoutingKeyRecommendationStatus;

  private WebServiceTemplate webServiceTemplate;

  private RecommendationService recommendationService;

  private RabbitTemplate rabbitTemplate;

  /**
   * Constructor of GmcClientService.
   */
  public GmcClientService(
      WebServiceTemplate webServiceTemplate,
      RecommendationService recommendationService,
      RabbitTemplate rabbitTemplate
  ) {
    this.webServiceTemplate = webServiceTemplate;
    this.recommendationService = recommendationService;
    this.rabbitTemplate = rabbitTemplate;
  }

  /**
   * Cron job to send RecommendationStatusDtos to Rabbit queue for GMC recommendation status check.
   */
  @Scheduled(cron = "${app.gmc.recommendationstatuscheck.cronExpression}")
  @SchedulerLock(name = "RecommendationStatusCheckJob")
  public void sendRecommendationStatusRequestToRabbit() {
    log.info("Start cron job: sendRecommendationStatusRequestToRabbit()");
    recommendationService.getRecommendationStatusCheckDtos().forEach(
        recommendation -> rabbitTemplate
            .convertAndSend(revalExchange, revalRoutingKeyRecommendationStatus, recommendation));
  }

  public RecommendationGmcOutcome checkRecommendationStatus(final String gmcNumber,
      final String gmcRecommendationId,
      final String recommendationId,
      final String designatedBody) {

    log.info("checking recommendation status for gmcId: {} and recommendationId: {}", gmcNumber,
        recommendationId);
    final var checkRecommendationStatus =
        buildCheckRecommendationStatusRequest(gmcNumber, gmcRecommendationId, recommendationId,
            designatedBody);
    try {
      final var checkRecommendationStatusResponse = (CheckRecommendationStatusResponse) webServiceTemplate
          .marshalSendAndReceive(gmcConnectUrl, checkRecommendationStatus,
              new SoapActionCallback(gmcSoapBaseAction + CHECK_RECOMMENDATION_STATUS));

      final var checkRecommendationStatusResult = checkRecommendationStatusResponse
          .getCheckRecommendationStatusResult();
      final var gmcReturnCode = checkRecommendationStatusResult.getReturnCode();
      log.info("Check recommendation status return code: {}", gmcReturnCode);
      if (SUCCESS.getCode().equals(gmcReturnCode)) {
        final var status = checkRecommendationStatusResult.getStatus();
        return RecommendationGmcOutcome.fromString(status);
      } else {
        final var responseCode = fromCode(gmcReturnCode);
        log.error(
            "Gmc recommendation check status request is failed for GmcId: {} and recommendationId: {}. Reason: {}."
                + " Recommendation will stay in Under Review state", gmcNumber, recommendationId,
            responseCode);
      }
    } catch (Exception e) {
      String errMessage = String
          .format("Failed to check status with GMC, GmcId: %s, recommendationId: %s", gmcNumber,
              recommendationId);
      log.error(errMessage, e);
    }

    return UNDER_REVIEW;
  }

  public TryRecommendationV2Response submitToGmc(final DoctorsForDB doctorForDB,
      final Recommendation recommendation, final RoUserProfileDto userProfileDto) {
    final var tryRecommendation = new TryRecommendationV2();
    final var request = new TryRecommendationV2Request();
    request.setDoctorSurname(doctorForDB.getDoctorLastName());
    request.setDoctorUID(doctorForDB.getGmcReferenceNumber());
    request.setRoSurname(userProfileDto.getLastName());
    request.setRoUID(userProfileDto.getGmcId());
    request.setRoDesignatedBodyCode(doctorForDB.getDesignatedBodyCode());
    request.setRoPhoneNumber(userProfileDto.getPhoneNumber());
    request.setRoRecommendation(recommendation.getRecommendationType().getCode());
    if (DEFER.equals(recommendation.getRecommendationType())) {
      request.setRoRecommendationReason(recommendation.getDeferralReason());
      request.setRoRecommendationSubreason(recommendation.getDeferralSubReason());
      request.setRequestedDeferralDate(convertDateInGmcFormat(recommendation.getDeferralDate()));
    } else {
      request.setRoRecommendationReason("");
      request.setRoRecommendationSubreason("");
      request.setRequestedDeferralDate(null);
    }
    request.setClientRequestID(recommendation.getId());
    request.setInternalUser(userProfileDto.getUserName());
    request.setInternalUserEmailAddress(userProfileDto.getEmailAddress());
    tryRecommendation.setRecReq(request);
    tryRecommendation.setUsername(gmcUserName);
    tryRecommendation.setPassword(gmcPassword);

    try {
      log.info("Submitting recommendation to GMC for gmcId: {}",
          doctorForDB.getGmcReferenceNumber());
      return (TryRecommendationV2Response) webServiceTemplate
          .marshalSendAndReceive(gmcConnectUrl, tryRecommendation,
              new SoapActionCallback(gmcSoapBaseAction + TRY_RECOMMENDATION_V2));
    } catch (Exception e) {
      log.error("Failed to submit to GMC", e);
    }
    return new TryRecommendationV2Response();
  }

  private CheckRecommendationStatus buildCheckRecommendationStatusRequest(final String gmcNumber,
      final String gmcRecommendationId,
      final String recommendationId,
      final String designatedBody) {
    final var checkRecommendationStatus = new CheckRecommendationStatus();
    final var checkRecommendationStatusRequest = new CheckRecommendationStatusRequest();
    checkRecommendationStatusRequest.setGMCReferenceNumber(gmcNumber);
    checkRecommendationStatusRequest.setRecommendationId(gmcRecommendationId);
    checkRecommendationStatusRequest.setDesignatedBodyId(designatedBody);
    checkRecommendationStatusRequest.setClientRequestId(recommendationId);
    checkRecommendationStatusRequest
        .setInternalUserId(INTERNAL_USER_ID); //TODO: replace when we have login details
    checkRecommendationStatus.setRequest(checkRecommendationStatusRequest);
    checkRecommendationStatus.setUsername(gmcUserName);
    checkRecommendationStatus.setPassword(gmcPassword);
    return checkRecommendationStatus;
  }
}
