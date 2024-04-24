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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.event.DoctorsForDbCollectedEvent;
import uk.nhs.hee.tis.revalidation.mapper.RecommendationViewMapper;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;
import uk.nhs.hee.tis.revalidation.service.RecommendationElasticSearchService;

@ExtendWith(MockitoExtension.class)
class RabbitMessageListenerTest {

  private final Faker faker = new Faker();

  @InjectMocks
  RabbitMessageListener rabbitMessageListener;

  @Mock
  RecommendationStatusCheckUpdatedMessageHandler recommendationStatusCheckUpdatedMessageHandler;

  @Mock
  DoctorsForDBService doctorsForDBService;

  @Captor
  ArgumentCaptor<RecommendationStatusCheckDto> recommendationStatusCheckDtoCaptor;
  @Captor
  ArgumentCaptor<ConnectionMessageDto> connectionMessageDtoArgumentCaptor;
  @Mock
  RecommendationElasticSearchService recommendationElasticSearchService;
  @Mock
  private RecommendationViewMapper recommendationViewMapper;
  @Captor
  ArgumentCaptor<RecommendationView> recommendationViewArgCaptor;

  private final String id = faker.number().digits(1);
  private final String gmcNumber = faker.number().digits(7);
  private final String gmcRecommendationId = faker.number().digits(3);
  private final String recommendationId = faker.number().digits(3);
  private final String designatedBody = faker.lorem().characters(5);
  private final LocalDate submissionDate = LocalDate.now();
  private final LocalDateTime gmcLastUpdatedDateTime = LocalDateTime.now();
  private final LocalDateTime requestDateTime = LocalDateTime.now();

  private final RecommendationStatusCheckDto recommendationStatusCheckDto =
      RecommendationStatusCheckDto.builder()
          .gmcReferenceNumber(gmcNumber)
          .gmcRecommendationId(gmcRecommendationId)
          .recommendationId(recommendationId)
          .designatedBodyId(designatedBody)
          .outcome(RecommendationGmcOutcome.APPROVED)
          .build();

  private MasterDoctorViewDto getMasterDoctorViewDto() {
    return MasterDoctorViewDto.builder()
        .id(id)
        .gmcReferenceNumber(gmcNumber)
        .designatedBody(designatedBody)
        .underNotice("Yes")
        .build();
  }

  @Test
  void shouldHandleRecommendationStatusCheckMessages() {
    rabbitMessageListener.receiveMessageForRecommendationStatusUpdate(recommendationStatusCheckDto);
    verify(recommendationStatusCheckUpdatedMessageHandler)
        .updateRecommendationAndTisStatus(recommendationStatusCheckDtoCaptor.capture());

    assertThat(recommendationStatusCheckDtoCaptor.getValue().getGmcReferenceNumber(),
        is(gmcNumber));
    assertThat(recommendationStatusCheckDtoCaptor.getValue().getGmcRecommendationId(),
        is(gmcRecommendationId));
    assertThat(recommendationStatusCheckDtoCaptor.getValue().getRecommendationId(),
        is(recommendationId));
    assertThat(recommendationStatusCheckDtoCaptor.getValue().getDesignatedBodyId(),
        is(designatedBody));
    assertThat(recommendationStatusCheckDtoCaptor.getValue().getOutcome(),
        is(RecommendationGmcOutcome.APPROVED));
  }

  @Test
  void shouldHandleDoctorsForDbCollectedMessage() {
    DoctorsForDbCollectedEvent event = new DoctorsForDbCollectedEvent(designatedBody,
        requestDateTime, null);

    rabbitMessageListener.handleDoctorsForDbCollectedMessage(event);

    verify(doctorsForDBService, times(1))
        .handleDoctorsForDbCollectedEvent(event);
  }

  @Test
  void shouldHandleDoctorConnectionMessage() {
    final var message = ConnectionMessageDto.builder()
        .gmcId(gmcNumber)
        .submissionDate(submissionDate)
        .designatedBodyCode(designatedBody)
        .gmcLastUpdatedDateTime(gmcLastUpdatedDateTime)
        .build();

    rabbitMessageListener.receiveUpdateDoctorConnectionMessage(message);

    verify(doctorsForDBService).updateDoctorConnection(
        connectionMessageDtoArgumentCaptor.capture());

    final var capture = connectionMessageDtoArgumentCaptor.getValue();
    assertThat(capture.getGmcId(), is(gmcNumber));
    assertThat(capture.getSubmissionDate(), is(submissionDate));
    assertThat(capture.getDesignatedBodyCode(), is(designatedBody));
    assertThat(capture.getGmcLastUpdatedDateTime(), is(gmcLastUpdatedDateTime));
  }

  @Test
  void shouldNotRequeueDBCStatusUpdateMessageOnException() {
    doThrow(new NullPointerException()).when(doctorsForDBService).updateDoctorConnection(any());

    assertThrows(AmqpRejectAndDontRequeueException.class,
        () -> rabbitMessageListener.receiveUpdateDoctorConnectionMessage(null));
  }

  @Test
  void shouldNotRequeueRecommendationStatusUpdateMessageOnException() {
    doThrow(new NullPointerException()).when(recommendationStatusCheckUpdatedMessageHandler)
        .updateRecommendationAndTisStatus(any());

    assertThrows(AmqpRejectAndDontRequeueException.class,
        () -> rabbitMessageListener.receiveMessageForRecommendationStatusUpdate(null));
  }

  @Test
  void shouldDiscardUpdateMessagesFromMasterDoctorViewIfGmcReferenceNumberNull() {
    MasterDoctorViewDto testDto =
        MasterDoctorViewDto.builder()
            .gmcReferenceNumber(null)
            .tcsPersonId(1L)
            .designatedBody(designatedBody)
            .underNotice("Yes")
            .build();

    assertThrows(AmqpRejectAndDontRequeueException.class,
        () -> rabbitMessageListener.receiveUpdateMessageFromMasterDoctorView(testDto));
  }

  @Test
  void shouldNotUpdateMessageFromMasterDoctorViewOnException() {
    assertThrows(AmqpRejectAndDontRequeueException.class,
        () -> rabbitMessageListener.receiveUpdateMessageFromMasterDoctorView(null));
  }

  @Test
  void shouldThrowExceptionWhenIdNullInReceivedMsgFromMasterDoctorView() {
    MasterDoctorViewDto masterDoctorViewDto = getMasterDoctorViewDto();
    masterDoctorViewDto.setId(null);
    assertThrows(AmqpRejectAndDontRequeueException.class,
        () -> rabbitMessageListener.receiveUpdateMessageFromMasterDoctorView(masterDoctorViewDto));
  }

  @Test
  void shouldReceiveUpdateMessageFromMasterDoctorView() {
    MasterDoctorViewDto masterDoctorViewDto = getMasterDoctorViewDto();
    RecommendationView recommendationView = RecommendationView.builder()
        .id(id)
        .gmcReferenceNumber(gmcNumber)
        .designatedBody(designatedBody)
        .underNotice("Yes")
        .build();
    when(recommendationViewMapper.mapMasterDoctorViewDtoToRecommendationView(masterDoctorViewDto))
        .thenReturn(recommendationView);
    rabbitMessageListener.receiveUpdateMessageFromMasterDoctorView(masterDoctorViewDto);

    verify(recommendationElasticSearchService)
        .saveRecommendationView(recommendationViewArgCaptor.capture());

    RecommendationView savedRecommendationView = recommendationViewArgCaptor.getValue();
    assertEquals(savedRecommendationView, recommendationView);
  }
}