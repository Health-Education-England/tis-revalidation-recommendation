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
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.DeferralReason;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationType;
import uk.nhs.hee.tis.revalidation.entity.Snapshot;
import uk.nhs.hee.tis.revalidation.entity.SnapshotRevalidation;
import uk.nhs.hee.tis.revalidation.repository.SnapshotRepository;

@ExtendWith(MockitoExtension.class)
class SnapshotServiceTest {

  private Faker faker = new Faker();

  @InjectMocks
  private SnapshotService snapshotService;

  @Mock
  private SnapshotRepository snapshotRepository;

  @Mock
  private DeferralReasonService deferralReasonService;

  @Mock
  private GmcClientService gmcClientService;

  @Mock
  private Recommendation recommendation;

  @Mock
  private DeferralReason reason;

  @Mock
  private DeferralReason subReason;

  @Mock
  private DoctorsForDB doctorsForDB;

  @Mock
  private Snapshot snapshot1;

  @Mock
  private SnapshotRevalidation snapshotRevalidation1;

  @Captor
  ArgumentCaptor<Snapshot> snapshotCaptor;

  private String id;
  private String gmcNumber;
  private RecommendationGmcOutcome outcome;
  private RecommendationType recommendationType;
  private RecommendationStatus recommendationStatus;
  private LocalDate gmcSubmissionDate;
  private LocalDate actualSubmissionDate;
  private String gmcRevalidationId;
  private LocalDate deferralDate;
  private String deferralReason;
  private String deferralSubReason;
  private List<String> comments;
  private String admin;
  private String designatedBody;

  @BeforeEach
  public void setup() {
    id = faker.number().digits(10);
    gmcNumber = faker.number().digits(7);
    outcome = faker.options().option(RecommendationGmcOutcome.class);
    recommendationType = faker.options().option(RecommendationType.class);
    recommendationStatus = faker.options().option(RecommendationStatus.class);
    gmcSubmissionDate = LocalDate.now();
    actualSubmissionDate = LocalDate.now();
    deferralDate = LocalDate.now().plusDays(60);
    gmcRevalidationId = faker.number().digits(10);
    deferralReason = "1";
    deferralSubReason = "1";
    comments = List.of(faker.lorem().sentence(3));
    admin = faker.name().fullName();
    designatedBody = faker.lorem().characters(7);
  }

  @Test
  void shouldSaveRecommendationToSnapshot() {
    when(recommendation.getId()).thenReturn(id);
    when(recommendation.getGmcNumber()).thenReturn(gmcNumber);
    when(recommendation.getOutcome()).thenReturn(outcome);
    when(recommendation.getRecommendationType()).thenReturn(recommendationType);
    when(recommendation.getRecommendationStatus()).thenReturn(recommendationStatus);
    when(recommendation.getGmcSubmissionDate()).thenReturn(gmcSubmissionDate);
    when(recommendation.getActualSubmissionDate()).thenReturn(actualSubmissionDate);
    when(recommendation.getDeferralDate()).thenReturn(deferralDate);
    when(recommendation.getGmcRevalidationId()).thenReturn(gmcRevalidationId);
    when(recommendation.getDeferralReason()).thenReturn(deferralReason);
    when(recommendation.getDeferralSubReason()).thenReturn(deferralSubReason);
    when(recommendation.getComments()).thenReturn(comments);
    when(recommendation.getAdmin()).thenReturn(admin);

    snapshotService.saveRecommendationToSnapshot(recommendation);

    verify(snapshotRepository).save(anyObject());
  }

  @Test
  void shouldGetSnapshotRecommendations() {
    when(doctorsForDB.getGmcReferenceNumber()).thenReturn(gmcNumber);
    when(doctorsForDB.getDesignatedBodyCode()).thenReturn(designatedBody);
    when(snapshotRepository.findByGmcNumber(gmcNumber)).thenReturn(List.of(snapshot1));
    when(snapshot1.getRevalidation()).thenReturn(snapshotRevalidation1);
    when(snapshotRevalidation1.getId()).thenReturn(id);
    when(snapshotRevalidation1.getGmcRecommendationId()).thenReturn(gmcRevalidationId);
    when(snapshotRevalidation1.getDeferralDate()).thenReturn(deferralDate.toString());
    when(snapshotRevalidation1.getDeferralReason()).thenReturn(deferralReason);
    when(snapshotRevalidation1.getDeferralSubReason()).thenReturn(deferralSubReason);
    when(snapshotRevalidation1.getRevalidationStatusCode()).thenReturn(recommendationStatus.name());
    when(snapshotRevalidation1.getProposedOutcomeCode()).thenReturn(recommendationType.name());
    when(snapshotRevalidation1.getSubmissionDate()).thenReturn(actualSubmissionDate.toString());
    when(snapshotRevalidation1.getGmcSubmissionDateTime()).thenReturn(gmcSubmissionDate.toString());
    when(snapshotRevalidation1.getAdmin()).thenReturn(admin);
    when(gmcClientService.checkRecommendationStatus(gmcNumber, gmcRevalidationId,
        id, designatedBody)).thenReturn(outcome);

    final var snapshotRecommendations = snapshotService.getSnapshotRecommendations(doctorsForDB);
    assertThat(snapshotRecommendations, hasSize(1));

    final var traineeRecommendationRecordDto = snapshotRecommendations.get(0);

    assertThat(traineeRecommendationRecordDto.getRecommendationId(), is(id));
    assertThat(traineeRecommendationRecordDto.getDeferralDate(), is(deferralDate));
    assertThat(traineeRecommendationRecordDto.getDeferralReason(), is(deferralReason));
    assertThat(traineeRecommendationRecordDto.getDeferralSubReason(), is(deferralSubReason));
    assertThat(traineeRecommendationRecordDto.getRecommendationStatus(),
        is(recommendationStatus.name()));
    assertThat(traineeRecommendationRecordDto.getRecommendationType(),
        is(recommendationType.name()));
    assertThat(traineeRecommendationRecordDto.getActualSubmissionDate(), is(actualSubmissionDate));
    assertThat(traineeRecommendationRecordDto.getGmcSubmissionDate(), is(gmcSubmissionDate));
    assertThat(traineeRecommendationRecordDto.getGmcOutcome(), is(outcome.getOutcome()));
    assertThat(traineeRecommendationRecordDto.getAdmin(), is(admin));
  }

  @Test
  void shouldHaveNullDeferralReasonIfCodeIsNull() {
    Recommendation testRecommendation = buildTestRecommendation();
    testRecommendation.setDeferralReason(null);

    snapshotService.saveRecommendationToSnapshot(testRecommendation);

    verify(snapshotRepository).save(snapshotCaptor.capture());

    final var result = snapshotCaptor.getValue();

    assertThat(result.getRevalidation().getDeferralReason(), is(nullValue()));
  }

  @Test
  void shouldHaveNullDeferralReasonIfDeferralReasonNotInDb() {
    Recommendation testRecommendation = buildTestRecommendation();
    when(deferralReasonService.getDeferralReasonByCode(deferralReason))
        .thenReturn(null);

    snapshotService.saveRecommendationToSnapshot(testRecommendation);

    verify(snapshotRepository).save(snapshotCaptor.capture());

    final var result = snapshotCaptor.getValue();

    assertThat(result.getRevalidation().getDeferralReason(), is(nullValue()));
  }

  @Test
  void shouldReturnDeferralReasonIfInDb() {
    Recommendation testRecommendation = buildTestRecommendation();
    when(deferralReasonService.getDeferralReasonByCode(deferralReason))
        .thenReturn(buildTestDeferralReason());

    snapshotService.saveRecommendationToSnapshot(testRecommendation);

    verify(snapshotRepository).save(snapshotCaptor.capture());

    final var result = snapshotCaptor.getValue();

    assertThat(result.getRevalidation().getDeferralReason(), is(deferralReason));
  }

  @Test
  void shouldHaveNullDeferralSubReasonIfCodeIsNull() {
    Recommendation testRecommendation = buildTestRecommendation();
    testRecommendation.setDeferralReason(null);
    testRecommendation.setDeferralSubReason(null);

    snapshotService.saveRecommendationToSnapshot(testRecommendation);

    verify(snapshotRepository).save(snapshotCaptor.capture());

    final var result = snapshotCaptor.getValue();

    assertThat(result.getRevalidation().getDeferralSubReason(), is(nullValue()));
  }

  @Test
  void shouldHaveNullDeferralSubReasonIfDeferralReasonNotInDb() {
    Recommendation testRecommendation = buildTestRecommendation();
    when(deferralReasonService.getDeferralSubReasonByReasonCodeAndReasonSubCode(deferralReason, deferralSubReason))
        .thenReturn(null);
    snapshotService.saveRecommendationToSnapshot(testRecommendation);

    verify(snapshotRepository).save(snapshotCaptor.capture());

    final var result = snapshotCaptor.getValue();

    assertThat(result.getRevalidation().getDeferralSubReason(), is(nullValue()));
  }

  @Test
  void shouldReturnDeferralSubReasonIfInDb() {
    Recommendation testRecommendation = buildTestRecommendation();
    when(deferralReasonService.getDeferralSubReasonByReasonCodeAndReasonSubCode(deferralReason, deferralSubReason))
        .thenReturn(buildTestDeferralReason());
    snapshotService.saveRecommendationToSnapshot(testRecommendation);

    verify(snapshotRepository).save(snapshotCaptor.capture());

    final var result = snapshotCaptor.getValue();

    assertThat(result.getRevalidation().getDeferralSubReason(), is(deferralSubReason));
  }

  private Recommendation buildTestRecommendation() {
    return Recommendation.builder()
        .id(id)
        .gmcNumber(gmcNumber)
        .outcome(outcome)
        .recommendationType(recommendationType)
        .recommendationStatus(recommendationStatus)
        .gmcSubmissionDate(gmcSubmissionDate)
        .actualSubmissionDate(actualSubmissionDate)
        .deferralDate(deferralDate)
        .gmcRevalidationId(gmcRevalidationId)
        .deferralReason(deferralReason)
        .deferralSubReason(deferralSubReason)
        .comments(comments)
        .admin(admin)
        .build();
  }

  private DeferralReason buildTestDeferralReason() {
    final var reason = DeferralReason.builder()
        .code("1")
        .reason("1")
        .abbr("1")
        .build();

    reason.setDeferralSubReasons(List.of(reason));
    return reason;
  }

}