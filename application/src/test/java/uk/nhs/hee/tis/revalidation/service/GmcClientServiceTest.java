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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.INTERNAL_ERROR;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.INVALID_CREDENTIALS;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.INVALID_RECOMMENDATION;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.INVALID_RECOMMENDATION_ID;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.MISSING_INTERNAL_USER;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.MISSING_OR_INVALID_DESIGNATED_BODY;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.MISSING_OR_INVALID_GMC_REF_NUMBER;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.YOUR_ACCOUNT_DOES_NOT_HAVE_ACCESS_TO_DB;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.UNDER_REVIEW;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.core.SoapActionCallback;
import uk.nhs.hee.tis.gmc.client.generated.CheckRecommendationStatus;
import uk.nhs.hee.tis.gmc.client.generated.CheckRecommendationStatusResponse;
import uk.nhs.hee.tis.gmc.client.generated.CheckRecommendationStatusResponseCT;

@ExtendWith(MockitoExtension.class)
class GmcClientServiceTest {

  private final Faker faker = new Faker();

  @InjectMocks
  private GmcClientService gmcClientService;

  @Mock
  private WebServiceTemplate webServiceTemplate;

  @Mock
  private CheckRecommendationStatusResponse statusResponse;

  @Mock
  private CheckRecommendationStatusResponseCT statusResponseCT;


  private final String gmcId = faker.number().digits(8);
  private final String recommendationId = faker.number().digits(10); //internal recommendationId
  private final String gmcRecommendationId = faker.number()
      .digits(10); //gmc Revalidation Id which we will receive when we submit recommendation.
  private final String designatedBodyCode = faker.lorem().characters(8);
  private final String gmcId2 = faker.number().digits(8);
  private final String recommendationId2 = faker.number().digits(10);
  private final String gmcRecommendationId2 = faker.number()
      .digits(10);
  private final String designatedBodyCode2 = faker.lorem().characters(8);
  private final String url = faker.lorem().characters(10);
  private final String username = faker.lorem().characters(10);
  private final String password = faker.lorem().characters(10);
  private final String exchange = faker.lorem().characters(10);
  private final String routingKey = faker.lorem().characters(10);

  /**
   * Data setup.
   */
  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(gmcClientService, "gmcConnectUrl", url);
    ReflectionTestUtils.setField(gmcClientService, "gmcUserName", username);
    ReflectionTestUtils.setField(gmcClientService, "gmcPassword", password);

  }

  @Test
  void shouldReturnSuccessForCheckStatusOfRecommendation() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);
    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode()).thenReturn(SUCCESS.getCode());
    when(statusResponseCT.getStatus()).thenReturn(APPROVED.getOutcome());

    final var checkRecommendationStatusResponse = gmcClientService
        .checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId,
            designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(APPROVED));
  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenInvalidCredentials() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);
    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode()).thenReturn(INVALID_CREDENTIALS.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));
  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenInvalidGmcNumber() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);

    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode()).thenReturn(MISSING_OR_INVALID_GMC_REF_NUMBER.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));

  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenInternalError() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);

    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode()).thenReturn(INTERNAL_ERROR.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));
  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenInvalidRecommendationId() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);

    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode()).thenReturn(INVALID_RECOMMENDATION_ID.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));
  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenNoDBAccess() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);

    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode())
        .thenReturn(YOUR_ACCOUNT_DOES_NOT_HAVE_ACCESS_TO_DB.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));
  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenDesignatedBodyInvalid() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);

    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode())
        .thenReturn(MISSING_OR_INVALID_DESIGNATED_BODY.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));
  }

  @Test
  void shouldReturnErrorForCheckStatusOfRecommendationWhenMissingInternalUser() {

    when(webServiceTemplate
        .marshalSendAndReceive(any(String.class), any(CheckRecommendationStatus.class)
            , any(SoapActionCallback.class))).thenReturn(statusResponse);

    when(statusResponse.getCheckRecommendationStatusResult()).thenReturn(statusResponseCT);
    when(statusResponseCT.getReturnCode())
        .thenReturn(MISSING_INTERNAL_USER.getCode());

    final var checkRecommendationStatusResponse = gmcClientService.
        checkRecommendationStatus(gmcId, gmcRecommendationId, recommendationId, designatedBodyCode);

    assertNotNull(checkRecommendationStatusResponse);
    assertThat(checkRecommendationStatusResponse, is(UNDER_REVIEW));
  }
}