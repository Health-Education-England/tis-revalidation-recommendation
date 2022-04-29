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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;


@ExtendWith(MockitoExtension.class)
class GmcDoctorConnectionSyncServiceTest {

  @Captor
  ArgumentCaptor<String> syncStartMessage;
  @Captor
  ArgumentCaptor<IndexSyncMessage<RevalidationSummaryDto>> messageCaptor;

  @InjectMocks
  private GmcDoctorConnectionSyncService gmcDoctorConnectionSyncService;
  @Mock
  private QueueMessagingTemplate queueMessagingTemplate;
  @Mock
  private DoctorsForDBRepository doctorsForDBRepository;
  @Mock
  private RecommendationService recommendationService;

  private DoctorsForDB doctor1;
  private List<DoctorsForDB> doctorsForDBList;
  private TraineeRecommendationRecordDto recommendation1;
  private String gmcOutcome1 = "APPROVED";
  private String gmcRef1 = "1111111";
  private String admin1 = "admin";
  private String sqsEndpoint = "/endpoint";

  @BeforeEach
  void setup() {
    buildDoctorsList();
    buildRecommendations();
    ReflectionTestUtils.setField(gmcDoctorConnectionSyncService, "sqsEndPoint", sqsEndpoint);
  }

  @Test
  void shouldRetrieveAllDoctors() {
    gmcDoctorConnectionSyncService.receiveMessage("gmcSyncStart");

    verify(doctorsForDBRepository).findAll();
  }

  @Test
  void shouldNotRetireveDoctorsIfNullMessageSupplied() {
    gmcDoctorConnectionSyncService.receiveMessage(null);

    verify(doctorsForDBRepository, never()).findAll();
  }

  @Test
  void shouldNotRetireveDoctorsIfIncorrectMessageSupplied() {
    gmcDoctorConnectionSyncService.receiveMessage("anyString");

    verify(doctorsForDBRepository, never()).findAll();
  }

  @Test
  void shouldSendRetrievedDoctorsToSqs() {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1))
        .thenReturn(recommendation1);

    gmcDoctorConnectionSyncService.receiveMessage("gmcSyncStart");

    verify(queueMessagingTemplate, times(2))
        .convertAndSend(eq(sqsEndpoint), messageCaptor.capture());

    final var messagePayloads = messageCaptor.getAllValues();
    assertThat(messagePayloads.get(0).getSyncEnd(), is(false));
    assertThat(messagePayloads.get(0).getPayload().getGmcOutcome(), is(gmcOutcome1));
    assertThat(messagePayloads.get(0).getPayload().getDoctor().getGmcReferenceNumber(), is(gmcRef1));
    assertThat(messagePayloads.get(0).getPayload().getDoctor().getAdmin(), is(admin1));


  }

  @Test
  void shouldSendSyncEndFlagAtEndOfDoctorsList() {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1))
        .thenReturn(recommendation1);

    gmcDoctorConnectionSyncService.receiveMessage("gmcSyncStart");

    verify(queueMessagingTemplate, times(2))
        .convertAndSend(eq(sqsEndpoint), messageCaptor.capture());

    final var messagePayloads = messageCaptor.getAllValues();
    assertThat(messagePayloads.get(1).getSyncEnd(), is(true));
  }

  private void buildDoctorsList() {
    doctor1 = DoctorsForDB.builder()
        .gmcReferenceNumber(gmcRef1)
        .admin(admin1)
        .build();
    doctorsForDBList = List.of(doctor1);
  }

  private void buildRecommendations() {
    recommendation1 = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcRef1)
        .gmcOutcome(gmcOutcome1)
        .build();
  }
}