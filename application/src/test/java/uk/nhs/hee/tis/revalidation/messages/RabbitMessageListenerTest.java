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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.github.javafaker.Faker;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.mapper.RecommendationViewMapper;
import uk.nhs.hee.tis.revalidation.messages.receiver.EsRebuildMessageReceiver;
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

  private final String getMaster = faker.number().digits(7);

  @Captor
  ArgumentCaptor<RecommendationStatusCheckDto> recommendationStatusCheckDtoCaptor;
  @Mock
  RecommendationElasticSearchService recommendationElasticSearchService;
  @Mock
  private RecommendationViewMapper recommendationViewMapper;

  private final String gmcNumber = faker.number().digits(7);
  private final String gmcRecommendationId = faker.number().digits(3);
  private final String recommendationId = faker.number().digits(3);
  private final String designatedBody = faker.lorem().characters(5);
  private final RecommendationGmcOutcome outcome = RecommendationGmcOutcome.APPROVED;
  @Mock
  EsRebuildMessageReceiver esRebuildMessageReceiver;

  private final RecommendationStatusCheckDto recommendationStatusCheckDto =
      RecommendationStatusCheckDto.builder()
          .gmcReferenceNumber(gmcNumber)
          .gmcRecommendationId(gmcRecommendationId)
          .recommendationId(recommendationId)
          .designatedBodyId(designatedBody)
          .outcome(RecommendationGmcOutcome.APPROVED)
          .build();
  @Captor
  ArgumentCaptor<RecommendationView> recommendationViewArgumentCaptor;

  private final MasterDoctorViewDto masterDoctorViewDto =
      MasterDoctorViewDto.builder()
          .gmcReferenceNumber(gmcNumber)
          .designatedBody(designatedBody)
          .underNotice("Yes")
          .build();

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
  void shouldNotRequeueDoctorUpdateMessageOnException() {
    doThrow(new NullPointerException()).when(doctorsForDBService).updateTrainee(any());

    assertThrows(AmqpRejectAndDontRequeueException.class, ()->{
      rabbitMessageListener.receivedMessage(null);
    });
  }

  @Test
  void shouldNotRequeueDBCStatusUpdateMessageOnException() {
    doThrow(new NullPointerException()).when(doctorsForDBService).removeDesignatedBodyCode(any());

    assertThrows(AmqpRejectAndDontRequeueException.class, ()->{
      rabbitMessageListener.receiveRemoveDoctorDesignatedBodyCodeMessage(null);
    });
  }

  @Test
  void shouldNotRequeueRecommendationStatusUpdateMessageOnException() {
    doThrow(new NullPointerException()).when(recommendationStatusCheckUpdatedMessageHandler)
        .updateRecommendationAndTisStatus(any());

    assertThrows(AmqpRejectAndDontRequeueException.class, () -> {
      rabbitMessageListener.receiveMessageForRecommendationStatusUpdate(null);
    });
  }

  @Test
  void shouldGetdatafromMasterIndex() throws IOException {
    rabbitMessageListener.receiveMessageGetMaster(getMaster);
    verify(esRebuildMessageReceiver)
        .handleMessage(getMaster);
  }

  @Test
  void shouldReceiveUpdateMessageFromMasterDoctorView() {
    rabbitMessageListener.receiveUpdateMessageFromMasterDoctorView(masterDoctorViewDto);
    verify(recommendationElasticSearchService)
        .saveRecommendationViews(recommendationViewArgumentCaptor.capture());
  }
}