///*
// * The MIT License (MIT)
// *
// * Copyright 2021 Crown Copyright (Health Education England)
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// * associated documentation files (the "Software"), to deal in the Software without restriction,
// * including without limitation the rights to use, copy, modify, merge, publish, distribute,
// * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or
// * substantial portions of the Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//
//package uk.nhs.hee.tis.revalidation.service;
//
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
//
//import java.util.List;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Captor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.boot.test.system.OutputCaptureExtension;
//import org.springframework.test.util.ReflectionTestUtils;
//import uk.nhs.hee.tis.revalidation.entity.Recommendation;
//import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
//import uk.nhs.hee.tis.revalidation.entity.RevalidationSummary;
//import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
//import uk.nhs.hee.tis.revalidation.messages.publisher.ElasticsearchSyncMessagePublisher;
//import uk.nhs.hee.tis.revalidation.repository.RevalidationSummaryRepository;
//
//@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
//class GmcDoctorConnectionSyncServiceTest {
//
//  public static final String GMC_SYNC_START = "gmcSyncStart";
//
//  @InjectMocks
//  private GmcDoctorConnectionSyncService gmcDoctorConnectionSyncService;
//
//  @Mock
//  private RevalidationSummaryRepository revalidationSummaryRepository;
//
//  @Mock
//  private ElasticsearchSyncMessagePublisher elasticsearchSyncMessagePublisher;
//
//  @Captor
//  ArgumentCaptor<IndexSyncMessage> indexSyncMessageArgumentCaptor;
//
//  private List<RevalidationSummary> summaryA, summaryB, summaryDisconnected;
//  private Recommendation recommendation1, recommendation2, recommendation3;
//  private IndexSyncMessage message1, message2, message3, endMessage;
//
//  private final RecommendationGmcOutcome gmcOutcome1 = APPROVED;
//  private final RecommendationGmcOutcome gmcOutcome2 = APPROVED;
//  private final RecommendationGmcOutcome gmcOutcome3 = APPROVED;
//
//  private final String gmcRef1 = "1111111";
//  private final String gmcRef2 = "2222222";
//  private final String gmcRef3 = "3333333";
//
//  private final String admin1 = "admin1";
//  private final String admin2 = "admin2";
//  private final String admin3 = "admin3";
//
//  private final String designatedBodyA = "AAAAAAA";
//  private final String designatedBodyB = "BBBBBBB";
//
//  private final List<String> dbs = List.of(designatedBodyA, designatedBodyB);
//
//  @BeforeEach
//  void setup() {
//    setupData();
//    ReflectionTestUtils.setField(gmcDoctorConnectionSyncService, "designatedBodies", dbs);
//  }
//
//  @Test
//  void shouldRetrieveAllDoctors() {
//    when(revalidationSummaryRepository.findByDesignatedBodyCode(designatedBodyA)).thenReturn(summaryA);
//    when(revalidationSummaryRepository.findByDesignatedBodyCode(designatedBodyB)).thenReturn(summaryB);
//    when(revalidationSummaryRepository.findByDesignatedBodyCodeIsNull()).thenReturn(summaryDisconnected);
//
//    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);
//
//    verify(revalidationSummaryRepository, times(1)).findByDesignatedBodyCode(designatedBodyA);
//    verify(revalidationSummaryRepository, times(1)).findByDesignatedBodyCode(designatedBodyB);
//    verify(revalidationSummaryRepository, times(1)).findByDesignatedBodyCodeIsNull();
//
//    verify(elasticsearchSyncMessagePublisher, times(4)).publishToBroker(
//        indexSyncMessageArgumentCaptor.capture());
//
//    var results = indexSyncMessageArgumentCaptor.getAllValues();
//    assertThat(results.get(0), is(message1));
//    assertThat(results.get(1), is(message2));
//    assertThat(results.get(2), is(message3));
//    assertThat(results.get(3), is(endMessage));
//  }
//
//  @Test
//  void shouldNotRetrieveDoctorsIfNullMessageSupplied() {
//    gmcDoctorConnectionSyncService.receiveMessage(null);
//
//    verify(revalidationSummaryRepository, never()).findByDesignatedBodyCode(any());
//    verify(revalidationSummaryRepository, never()).findByDesignatedBodyCodeIsNull();
//    verify(elasticsearchSyncMessagePublisher, never()).publishToBroker(any());
//  }
//
//  @Test
//  void shouldNotRetrieveDoctorsIfIncorrectMessageSupplied() {
//    gmcDoctorConnectionSyncService.receiveMessage("anyString");
//
//    verify(revalidationSummaryRepository, never()).findByDesignatedBodyCode(any());
//    verify(revalidationSummaryRepository, never()).findByDesignatedBodyCodeIsNull();
//    verify(elasticsearchSyncMessagePublisher, never()).publishToBroker(any());
//  }
//
//  private void setupData() {
//    recommendation1 = Recommendation.builder()
//        .gmcNumber(gmcRef1)
//        .outcome(gmcOutcome1)
//        .build();
//
//    recommendation2 = Recommendation.builder()
//        .gmcNumber(gmcRef2)
//        .outcome(gmcOutcome2)
//        .build();
//
//    recommendation3 = Recommendation.builder()
//        .gmcNumber(gmcRef3)
//        .outcome(gmcOutcome3)
//        .build();
//
//    summaryA = List.of(RevalidationSummary.builder()
//        .gmcReferenceNumber(gmcRef1)
//        .designatedBodyCode(designatedBodyA)
//        .admin(admin1)
//        .latestRecommendation(recommendation1)
//        .build());
//
//    summaryB = List.of(RevalidationSummary.builder()
//        .gmcReferenceNumber(gmcRef2)
//        .designatedBodyCode(designatedBodyB)
//        .admin(admin2)
//        .latestRecommendation(recommendation2)
//        .build());
//
//    summaryDisconnected = List.of(RevalidationSummary.builder()
//        .gmcReferenceNumber(gmcRef3)
//        .admin(admin3)
//        .latestRecommendation(recommendation3)
//        .build());
//
//    message1 = IndexSyncMessage.builder().payload(summaryA).syncEnd(false).build();
//    message2 = IndexSyncMessage.builder().payload(summaryB).syncEnd(false).build();
//    message3 = IndexSyncMessage.builder().payload(summaryDisconnected).syncEnd(false).build();
//    endMessage = IndexSyncMessage.builder().payload(List.of()).syncEnd(true).build();
//  }
//}
