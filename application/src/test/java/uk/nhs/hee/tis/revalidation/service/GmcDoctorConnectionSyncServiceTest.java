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
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.messages.publisher.ElasticsearchSyncMessagePublisher;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class GmcDoctorConnectionSyncServiceTest {

  public static final String GMC_SYNC_START = "gmcSyncStart";

  @InjectMocks
  private GmcDoctorConnectionSyncService gmcDoctorConnectionSyncService;

  @Mock
  private ElasticsearchSyncMessagePublisher elasticsearchSyncMessagePublisher;

  @Mock
  private DoctorsForDBRepository doctorsForDBRepository;

  @Mock
  private RecommendationRepository recommendationRepository;

  @Captor
  ArgumentCaptor<IndexSyncMessage> indexSyncMessageArgumentCaptor;

  private RevalidationSummaryDto summary;
  private DoctorsForDB doctor;
  private Page<DoctorsForDB> doctorPage;
  private Recommendation recommendation;
  private IndexSyncMessage message1, endMessage;

  private final RecommendationGmcOutcome gmcOutcome1 = APPROVED;

  private final String gmcRef1 = "1111111";

  private final String designatedBody = "AAAAAAA";

  @BeforeEach
  void setup() {
    setupData();
    ReflectionTestUtils.setField(gmcDoctorConnectionSyncService, "batchSize", 1);
  }

  @Test
  void shouldRetrieveAllDoctors() {
    PageRequest pageRequest = PageRequest.of(0, 1);

    when(doctorsForDBRepository.findAll(pageRequest))
        .thenReturn(doctorPage);
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(
        gmcRef1)).thenReturn(
        Optional.of(recommendation));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(elasticsearchSyncMessagePublisher, times(2)).publishToBroker(
        indexSyncMessageArgumentCaptor.capture());

    var results = indexSyncMessageArgumentCaptor.getAllValues();
    assertThat(results.get(0), is(message1));
    assertThat(results.get(1), is(endMessage));
  }

  @Test
  void shouldNotRetrieveDoctorsIfNullMessageSupplied() {
    gmcDoctorConnectionSyncService.receiveMessage(null);

    verify(doctorsForDBRepository, never()).findAll(any(Pageable.class));
    verify(recommendationRepository, never()).findFirstByGmcNumberOrderByActualSubmissionDateDesc(
        any());
    verify(elasticsearchSyncMessagePublisher, never()).publishToBroker(any());
  }

  @Test
  void shouldNotRetrieveDoctorsIfIncorrectMessageSupplied() {
    gmcDoctorConnectionSyncService.receiveMessage("wrongMessage");

    verify(doctorsForDBRepository, never()).findAll(any(Pageable.class));
    verify(recommendationRepository, never()).findFirstByGmcNumberOrderByActualSubmissionDateDesc(
        any());
    verify(elasticsearchSyncMessagePublisher, never()).publishToBroker(any());
  }

  private void setupData() {
    recommendation = Recommendation.builder()
        .gmcNumber(gmcRef1)
        .outcome(gmcOutcome1)
        .build();

    doctor = DoctorsForDB.builder()
        .gmcReferenceNumber(gmcRef1)
        .designatedBodyCode(designatedBody)
        .build();

    doctorPage = new PageImpl<>(List.of(doctor));

    summary = RevalidationSummaryDto.builder()
        .doctor(doctor)
        .gmcOutcome(String.valueOf(APPROVED))
        .build();

    message1 = IndexSyncMessage.builder().payload(List.of(summary)).syncEnd(false).build();
    endMessage = IndexSyncMessage.builder().payload(List.of()).syncEnd(true).build();
  }
}
