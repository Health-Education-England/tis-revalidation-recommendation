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

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.INVALID_RECOMMENDATION;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.REJECTED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.UNDER_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.COMPLETED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.DRAFT;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.NOT_STARTED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.READY_TO_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.SUBMITTED_TO_GMC;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.DEFER;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.NON_ENGAGEMENT;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.REVALIDATE;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDate;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDateTime;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationResponseCT;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationV2Response;
import uk.nhs.hee.tis.revalidation.dto.DeferralReasonDto;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DeferralReason;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationType;
import uk.nhs.hee.tis.revalidation.entity.Status;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

  private final Faker faker = new Faker();

  @InjectMocks
  private RecommendationServiceImpl recommendationService;

  @Mock
  private RecommendationRepository recommendationRepository;

  @Mock
  private GmcClientService gmcClientService;

  @Mock
  private DeferralReasonService deferralReasonService;

  @Mock
  private DoctorsForDBRepository doctorsForDBRepository;

  @Mock
  private TraineeRecommendationRecordDto snapshot1;

  @Mock
  private TraineeRecommendationRecordDto snapshot2;

  @Mock
  private DoctorsForDB doctorsForDB;

  @Mock
  private DeferralReason deferralReason;

  @Mock
  private DeferralReason deferralSubReason;

  @Mock
  private SnapshotService snapshotService;

  private String firstName;
  private String lastName;
  private LocalDate submissionDate;
  private LocalDate actualSubmissionDate;
  private LocalDate dateAdded;
  private UnderNotice underNotice;
  private String sanction;
  private String designatedBodyCode;
  private RecommendationStatus status;

  private String deferralComment1, deferralComment2;
  private LocalDate deferralDate1, deferralDate2;
  private String deferralReason1, deferralReason2;
  private String deferralSubReason1;
  private String gmcOutcome1, gmcOutcome2;
  private String revalidationType1, revalidationType2;
  private String revalidationStatus1, revalidationStatus2;
  private String gmcSubmissionDate1, gmcSubmissionDate2;
  private String actualSubmissionDate1, actualSubmissionDate2;
  private String gmcRecommendationId1, gmcRecommendationId2;
  private String admin1, admin2;

  private String gmcNumber1;
  private List<String> comments;
  private String recommendationId, newRecommendationId;
  private String snapshotRevalidationId1, snapshotRevalidationId2;
  private List<DeferralReasonDto> deferralReasons;

  private String roFirstName;
  private String roLastName;
  private String roPhoneNumber;
  private String roEmailAddress;
  private String roUserName;

  private Recommendation recommendation1, recommendation2, recommendation3;
  private Recommendation recommendation4, recommendation5, recommendation6;

  private DoctorsForDB doctorsForDB1;

  @BeforeEach
  public void setup() {
    firstName = faker.name().firstName();
    lastName = faker.name().lastName();
    status = NOT_STARTED;
    submissionDate = LocalDate.now();
    actualSubmissionDate = LocalDate.now();
    dateAdded = LocalDate.now();
    underNotice = faker.options().option(UnderNotice.class);
    sanction = faker.lorem().characters(2);
    designatedBodyCode = faker.lorem().characters(7);

    deferralComment1 = faker.lorem().characters(20);
    deferralDate1 = LocalDate.of(2018, 03, 15);
    deferralReason1 = "1";
    deferralSubReason1 = "4";
    revalidationType1 = faker.options().option(RecommendationType.class).name();
    revalidationStatus1 = faker.options().option(RecommendationStatus.class).name();
    gmcOutcome1 = APPROVED.getOutcome();
    gmcSubmissionDate1 = "2018-03-15 12:00:00";
    actualSubmissionDate1 = "2018-03-15";
    admin1 = faker.internet().emailAddress();
    gmcRecommendationId1 = faker.number().digits(10);
    snapshotRevalidationId1 = faker.number().digits(10);

    deferralComment2 = faker.lorem().characters(20);
    deferralDate2 = LocalDate.of(2018, 03, 15);
    deferralReason2 = "2";
    revalidationType2 = null;
    revalidationStatus2 = null;
    gmcOutcome2 = APPROVED.getOutcome();
    gmcSubmissionDate2 = "2018-03-15 12:00:00";
    actualSubmissionDate2 = "2018-03-15";
    admin2 = faker.internet().emailAddress();
    gmcRecommendationId2 = faker.number().digits(10);
    snapshotRevalidationId2 = faker.number().digits(10);

    gmcNumber1 = faker.number().digits(7);
    comments = List
        .of(faker.lorem().sentence(3), faker.lorem().sentence(3), faker.lorem().sentence(7));
    recommendationId = faker.lorem().characters(10);
    newRecommendationId = faker.lorem().characters(10);
    deferralReasons = List
        .of(new DeferralReasonDto("1", "evidence", "SICK_CARERS_LEAVE", List.of(), Status.CURRENT),
            new DeferralReasonDto("2", "ongoing", "ONGOING_PROCESS", List.of(), Status.INACTIVE));

    roFirstName = faker.name().firstName();
    roLastName = faker.name().lastName();
    roUserName = faker.name().username();
    roEmailAddress = faker.internet().emailAddress();
    roPhoneNumber = faker.phoneNumber().phoneNumber();

    recommendation1 = new Recommendation();
    recommendation1.setRecommendationType(REVALIDATE);
    recommendation1.setOutcome(APPROVED);

    recommendation2 = new Recommendation();
    recommendation2.setRecommendationType(REVALIDATE);
    recommendation2.setOutcome(REJECTED);

    recommendation3 = new Recommendation();
    recommendation3.setRecommendationType(REVALIDATE);
    recommendation3.setOutcome(UNDER_REVIEW);

    recommendation4 = new Recommendation();
    recommendation4.setRecommendationType(REVALIDATE);

    recommendation5 = new Recommendation();

    recommendation6 = new Recommendation();
    recommendation6.setRecommendationType(REVALIDATE);
    recommendation6.setOutcome(APPROVED);
    recommendation6.setGmcSubmissionDate(LocalDate.now().minusMonths(1));


    doctorsForDB1 = buildDoctorForDB(gmcNumber1, RecommendationStatus.NOT_STARTED);
  }

  @Test
  void shouldReturnRecommendationWithCurrentAndLegacyRecommendations() {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId, RecommendationStatus.NOT_STARTED);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.of(doctorsForDB));
    when(snapshotService.getSnapshotRecommendations(doctorsForDB))
        .thenReturn(List.of(snapshot1, snapshot2));
    when(recommendationRepository.findByGmcNumber(gmcId)).thenReturn(List.of(
        buildRecommendation(gmcNumber1, recommendationId, status, UNDER_REVIEW)));
    when(deferralReasonService.getAllCurrentDeferralReasons()).thenReturn(deferralReasons);

    when(snapshot1.getAdmin()).thenReturn(admin1);
    when(snapshot1.getGmcNumber()).thenReturn(gmcId);
    when(snapshot1.getDeferralComment()).thenReturn(deferralComment1);
    when(snapshot1.getDeferralReason()).thenReturn(deferralReason1);
    when(snapshot1.getDeferralDate()).thenReturn(deferralDate1);
    when(snapshot1.getRecommendationStatus()).thenReturn(revalidationStatus1);
    when(snapshot1.getRecommendationType()).thenReturn(revalidationType1);
    when(snapshot1.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot1.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate1));
    when(snapshot1.getActualSubmissionDate()).thenReturn(formatDate(actualSubmissionDate1));
    when(snapshot1.getGmcRevalidationId()).thenReturn(gmcRecommendationId1);
    when(snapshot1.getRecommendationId()).thenReturn(snapshotRevalidationId1);

    when(snapshot2.getAdmin()).thenReturn(admin2);
    when(snapshot2.getGmcNumber()).thenReturn(gmcId);
    when(snapshot2.getDeferralComment()).thenReturn(deferralComment2);
    when(snapshot2.getDeferralReason()).thenReturn(deferralReason2);
    when(snapshot2.getDeferralDate()).thenReturn(deferralDate2);
    when(snapshot2.getRecommendationStatus()).thenReturn(revalidationStatus2);
    when(snapshot2.getRecommendationType()).thenReturn(revalidationType2);
    when(snapshot2.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot2.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate2));
    when(snapshot2.getActualSubmissionDate()).thenReturn(formatDate(actualSubmissionDate2));
    when(snapshot2.getGmcRevalidationId()).thenReturn(gmcRecommendationId2);
    when(snapshot2.getRecommendationId()).thenReturn(snapshotRevalidationId2);

    final var recommendation = recommendationService.getTraineeInfo(gmcId);
    assertThat(recommendation.getGmcNumber(), is(gmcId));
    assertThat(recommendation.getFullName(), is(getFullName(firstName, lastName)));
    assertThat(recommendation.getUnderNotice(), is(underNotice.value()));
    assertThat(recommendation.getDesignatedBody(), is(designatedBodyCode));
    assertThat(recommendation.getGmcSubmissionDate(), is(submissionDate));

    assertThat(recommendation.getRevalidations(), hasSize(3));
    var revalidationDTO = recommendation.getRevalidations().get(0);
    assertThat(revalidationDTO.getGmcOutcome(), is(UNDER_REVIEW.getOutcome()));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getRecommendationType(), is(REVALIDATE.name()));
    assertThat(revalidationDTO.getRecommendationStatus(), is(status.name()));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(submissionDate));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(actualSubmissionDate));
    assertThat(revalidationDTO.getRecommendationId(), is(recommendationId));
    assertThat(revalidationDTO.getComments(), is(comments));

    revalidationDTO = recommendation.getRevalidations().get(1);
    assertThat(revalidationDTO.getGmcNumber(), is(gmcId));
    assertThat(revalidationDTO.getDeferralReason(), is(deferralReason1));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate1));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment1));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome1));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidationType1));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus1));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate1)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(actualSubmissionDate1)));
    assertThat(revalidationDTO.getRecommendationId(), is(snapshotRevalidationId1));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId1));

    revalidationDTO = recommendation.getRevalidations().get(2);
    assertThat(revalidationDTO.getGmcNumber(), is(gmcId));
    assertThat(revalidationDTO.getDeferralReason(), is(deferralReason2));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate2));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment2));
    assertThat(revalidationDTO.getAdmin(), is(admin2));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome2));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidationType2));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus2));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate2)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(actualSubmissionDate2)));
    assertThat(revalidationDTO.getRecommendationId(), is(snapshotRevalidationId2));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId2));
  }

  @Test
  void shouldReturnCurrentRecommendationWhichAreSubmittedToGMC() {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId, RecommendationStatus.NOT_STARTED);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.of(doctorsForDB));
    when(snapshotService.getSnapshotRecommendations(doctorsForDB)).thenReturn(List.of(snapshot1));
    when(recommendationRepository.findByGmcNumber(gmcId))
        .thenReturn(List.of(buildRecommendation(gmcId, newRecommendationId,
            SUBMITTED_TO_GMC, UNDER_REVIEW)));
    when(deferralReasonService.getAllCurrentDeferralReasons()).thenReturn(deferralReasons);

    when(snapshot1.getAdmin()).thenReturn(admin1);
    when(snapshot1.getDeferralComment()).thenReturn(deferralComment1);
    when(snapshot1.getDeferralReason()).thenReturn(deferralReason1);
    when(snapshot1.getDeferralDate()).thenReturn(deferralDate1);
    when(snapshot1.getRecommendationStatus()).thenReturn(revalidationStatus1);
    when(snapshot1.getRecommendationType()).thenReturn(revalidationType1);
    when(snapshot1.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot1.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate1));
    when(snapshot1.getActualSubmissionDate()).thenReturn(formatDate(actualSubmissionDate1));
    when(snapshot1.getGmcRevalidationId()).thenReturn(gmcRecommendationId1);
    when(snapshot1.getRecommendationId()).thenReturn(recommendationId);

    final var recommendation = recommendationService.getTraineeInfo(gmcId);
    assertThat(recommendation.getGmcNumber(), is(gmcId));
    assertThat(recommendation.getFullName(), is(getFullName(firstName, lastName)));
    assertThat(recommendation.getUnderNotice(), is(underNotice.value()));
    assertThat(recommendation.getDesignatedBody(), is(designatedBodyCode));
    assertThat(recommendation.getGmcSubmissionDate(), is(submissionDate));

    assertThat(recommendation.getRevalidations(), hasSize(2));
    var revalidationDTO = recommendation.getRevalidations().get(0);
    assertThat(revalidationDTO.getGmcOutcome(), is(UNDER_REVIEW.getOutcome()));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getRecommendationType(), is(REVALIDATE.name()));
    assertThat(revalidationDTO.getRecommendationStatus(), is(SUBMITTED_TO_GMC.name()));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(submissionDate));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(actualSubmissionDate));
    assertThat(revalidationDTO.getComments(), is(comments));

    revalidationDTO = recommendation.getRevalidations().get(1);
    assertThat(revalidationDTO.getDeferralReason(), is(deferralReason1));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate1));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment1));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome1));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidationType1));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus1));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate1)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(actualSubmissionDate1)));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId1));
    assertThat(revalidationDTO.getRecommendationId(), is(recommendationId));
  }

  @Test
  void shouldReturnRecommendationDtoWithoutTisInformation() {
    final var gmcId = faker.number().digits(8);
    final var doctorsForDB = buildDoctorForDB(gmcId, RecommendationStatus.NOT_STARTED);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.of(doctorsForDB));
    final var recommendation = recommendationService.getTraineeInfo(gmcId);
    assertThat(recommendation.getGmcNumber(), is(gmcId));
    assertThat(recommendation.getFullName(), is(getFullName(firstName, lastName)));
    assertThat(recommendation.getCctDate(), is(nullValue()));
    assertThat(recommendation.getProgrammeMembershipType(), is(nullValue()));
    assertThat(recommendation.getCurrentGrade(), is(nullValue()));
    assertThat(recommendation.getRevalidations(), hasSize(0));
  }

  @Test
  void shouldReturnNullWhenUnknownVGmc() {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId, RecommendationStatus.NOT_STARTED);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.empty());
    assertThat(recommendationService.getTraineeInfo(gmcId), is(nullValue()));
  }

  @Test
  void shouldSaveRevalidateRecommendationInDraftState() {
    final var recordDTO = buildTraineeRecommendationRecordDto(null, REVALIDATE.name(), null, null,
        null, admin1);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldSaveNonEngagementRecommendationInDraftState() {
    final var recordDTO = buildTraineeRecommendationRecordDto(null, NON_ENGAGEMENT.name(), null,
        null, null, admin1);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldSaveDeferRecommendationInDraftState() {
    final var deferralDate = LocalDate.now().plusDays(90);
    final var recordDTO = buildTraineeRecommendationRecordDto(null, DEFER.name(), deferralDate,
        deferralReason1, deferralSubReason1, admin1);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralReason1)).thenReturn(deferralReason);
    when(deferralReason.getSubReasonByCode(deferralSubReason1)).thenReturn(deferralSubReason);

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldSaveDeferRecommendationInDraftStateWhenDeferralReasonIsNotRequiredSubReason() {
    final var deferralDate = LocalDate.now().plusDays(90);
    final var recordDTO = buildTraineeRecommendationRecordDto(null, DEFER.name(), deferralDate,
        deferralReason2, null, admin1);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralReason2)).thenReturn(deferralReason);
    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldThrowExceptionWhenDeferralDateWithin60DaysOfSubmissionDate() {
    final var deferralDate = submissionDate.plusDays(59);
    final var recordDTO = buildTraineeRecommendationRecordDto(null, DEFER.name(), deferralDate,
        deferralReason1, deferralSubReason1, null);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    RecommendationException ex = assertThrows(RecommendationException.class,
        () -> recommendationService.saveRecommendation(recordDTO));
    assertThat(ex.getMessage(), is(String.format(
        "Deferral date is invalid, should be in between of 60 and 365 days of Gmc Submission Date: %s",
        submissionDate)));
  }

  @Test
  void shouldThrowExceptionWhenDeferralDateMoreThen365DaysOfSubmissionDate() {
    final var deferralDate = submissionDate.plusDays(366);
    final var recordDTO = buildTraineeRecommendationRecordDto(null, DEFER.name(), deferralDate,
        deferralReason1, deferralSubReason1, null);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    RecommendationException ex = assertThrows(RecommendationException.class,
        () -> recommendationService.saveRecommendation(recordDTO));
    assertThat(ex.getMessage(), is(String.format(
        "Deferral date is invalid, should be in between of 60 and 365 days of Gmc Submission Date: %s",
        submissionDate)));
  }

  @Test
  void shouldThrowExceptionWhenGmcNumberIsUnknown() {
    final var deferralDate = submissionDate.plusDays(61);
    final var recordDTO = buildTraineeRecommendationRecordDto(null, DEFER.name(), deferralDate,
        deferralReason1, deferralSubReason1, null);
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.empty());

    RecommendationException ex = assertThrows(RecommendationException.class,
        () -> recommendationService.saveRecommendation(recordDTO));
    assertThat(ex.getMessage(), is(String.format("Doctor %s does not exist!", gmcNumber1)));
  }

  @Test
  void shouldSubmitRecommendation() {
    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status,
        UNDER_REVIEW);
    final var userProfileDto = buildRoUserProfileDto(gmcNumber1);
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(recommendationRepository.findByIdAndGmcNumber(recommendationId, gmcNumber1))
        .thenReturn(recommendation);
    when(gmcClientService.submitToGmc(doctorsForDB, recommendation, userProfileDto))
        .thenReturn(buildRecommendationV2Response(SUCCESS.getCode()));
    recommendationService.submitRecommendation(recommendationId, gmcNumber1, userProfileDto);
    verify(recommendationRepository).save(recommendation);
  }

  @Test
  void shouldThrowExceptionWhenSubmittingWithUnknownGmcNumber() {
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.empty());
    RoUserProfileDto roUserProfileDto = buildRoUserProfileDto(gmcNumber1);
    RecommendationException ex = assertThrows(RecommendationException.class,
        () -> recommendationService
            .submitRecommendation(recommendationId, gmcNumber1, roUserProfileDto));
    assertThat(ex.getMessage(), is(String.format("Doctor %s does not exist!", gmcNumber1)));
  }

  @Test
  void shouldNotUpdateRecommendationWhenSubmitFail() {
    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status,
        UNDER_REVIEW);
    final var userProfileDto = buildRoUserProfileDto(gmcNumber1);
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(recommendationRepository.findByIdAndGmcNumber(recommendationId, gmcNumber1))
        .thenReturn(recommendation);
    when(gmcClientService.submitToGmc(doctorsForDB, recommendation, userProfileDto))
        .thenReturn(buildRecommendationV2Response(INVALID_RECOMMENDATION.getCode()));

    assertThrows(RecommendationException.class, () -> recommendationService
        .submitRecommendation(recommendationId, gmcNumber1, userProfileDto));
    verify(recommendationRepository, times(0)).save(recommendation);
  }

  @Test
  void shouldNotUpdateRecommendationWhenSubmitResponseIsNull() {
    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status,
        UNDER_REVIEW);
    final var userProfileDto = buildRoUserProfileDto(gmcNumber1);
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(recommendationRepository.findByIdAndGmcNumber(recommendationId, gmcNumber1))
        .thenReturn(recommendation);
    when(gmcClientService.submitToGmc(doctorsForDB, recommendation, userProfileDto))
        .thenReturn(new TryRecommendationV2Response());

    boolean actual = recommendationService
        .submitRecommendation(recommendationId, gmcNumber1, userProfileDto);
    assertThat(actual, is(false));
    verify(recommendationRepository, times(0)).save(recommendation);
  }

  @Test
  void shouldUpdateRevalidateRecommendationInDraftState() {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationId(recommendationId)
        .recommendationType(REVALIDATE.name())
        .comments(comments)
        .build();

    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status,
        UNDER_REVIEW);
    when(recommendationRepository.findByGmcNumber(gmcNumber1)).thenReturn(List.of(recommendation));
    when(recommendationRepository.findById(recommendationId))
        .thenReturn(Optional.of(recommendation));
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.updateRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldUpdateNonEngagementRecommendationInDraftState() {
    final var recordDTO = buildTraineeRecommendationRecordDto(recommendationId,
        NON_ENGAGEMENT.name(), null, null, null, null);

    when(recommendationRepository.findById(recommendationId)).thenReturn(Optional
        .of(buildRecommendation(gmcNumber1, recommendationId, status, UNDER_REVIEW)));
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.updateRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldUpdateDeferRecommendationInDraftState() {
    final var deferralDate = LocalDate.now().plusDays(90);
    final var recordDTO = buildTraineeRecommendationRecordDto(recommendationId, DEFER.name(),
        deferralDate,
        deferralReason1, deferralSubReason1, null);

    when(recommendationRepository.findById(recommendationId)).thenReturn(Optional
        .of(buildRecommendation(gmcNumber1, recommendationId, status, UNDER_REVIEW)));
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralReason1)).thenReturn(deferralReason);
    when(deferralReason.getSubReasonByCode(deferralSubReason1)).thenReturn(deferralSubReason);

    recommendationService.updateRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldThrowExceptionWhenInvalidRecommendationIdProvidedForUpdate() {
    final var recordDTO = buildTraineeRecommendationRecordDto(recommendationId, REVALIDATE.name(),
        null, null, null, null);

    assertThrows(RecommendationException.class,
        () -> recommendationService.updateRecommendation(recordDTO));
  }

  @Test
  void shouldAllowedSaveRecommendationWhenOneAlreadyInSubmittedAndRejectedState() {
    final var recordDTO = buildTraineeRecommendationRecordDto(null, REVALIDATE.name(), null, null,
        null, null);

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    final var draftRecommendation1 = Recommendation.builder().id(recommendationId)
        .gmcNumber(gmcNumber1)
        .recommendationStatus(SUBMITTED_TO_GMC).outcome(REJECTED).build();
    when(recommendationRepository.findByGmcNumber(gmcNumber1))
        .thenReturn(List.of(draftRecommendation1));

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(any());
  }

  @Test
  void shouldNotSaveRecommendationWhenOneAlreadyInDraft() {
    final var recordDTO = buildTraineeRecommendationRecordDto(null, REVALIDATE.name(), null, null,
        null, null);

    final var draftRecommendation1 = Recommendation.builder().id(recommendationId)
        .gmcNumber(gmcNumber1).recommendationStatus(READY_TO_REVIEW).build();
    when(recommendationRepository.findByGmcNumber(gmcNumber1))
        .thenReturn(List.of(draftRecommendation1));
    assertThrows(RecommendationException.class,
        () -> recommendationService.saveRecommendation(recordDTO));
  }

  @Test
  void shouldNotSaveRecommendationWhenStatusIsSubmittedButStillUnderReview() {
    final var recordDTO = buildTraineeRecommendationRecordDto(null, REVALIDATE.name(), null, null,
        null, null);

    final var draftRecommendation1 = Recommendation.builder().id(recommendationId)
        .gmcNumber(gmcNumber1)
        .recommendationStatus(SUBMITTED_TO_GMC).outcome(UNDER_REVIEW).build();
    when(recommendationRepository.findByGmcNumber(gmcNumber1))
        .thenReturn(List.of(draftRecommendation1));

    assertThrows(RecommendationException.class,
        () -> recommendationService.saveRecommendation(recordDTO));
  }

  @Test
  void shouldReturnLatestRecommendations() {
    final var gmcId = faker.number().digits(7);
    final var recommendation = buildRecommendation(gmcId, recommendationId, status,
        UNDER_REVIEW);
    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcId))
        .thenReturn(
            Optional.of(recommendation));
    final var recommendationResult = recommendationService.getLatestRecommendation(gmcId);

    assertThat(recommendationResult.getGmcNumber(), is(gmcId));
    assertThat(recommendationResult.getGmcSubmissionDate(), is(submissionDate));
    assertThat(recommendationResult.getActualSubmissionDate(), is(actualSubmissionDate));
    assertThat(recommendationResult.getComments(), is(comments));
    assertThat(recommendationResult.getAdmin(), is(admin1));
  }

  @Test
  void shouldReturnLatestRecommendationsList() {
    final var gmcNumber2 = faker.number().digits(7);
    final var gmcNumberX = faker.number().digits(7);
    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status,
        UNDER_REVIEW);

    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation));
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumberX))
        .thenReturn(Optional.empty());
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber2))
        .thenReturn(Optional.of(recommendation2));
    final var actualRecommendationMap = recommendationService
        .getLatestRecommendations(List.of(gmcNumber1, gmcNumberX, gmcNumber2));

    assertThat(actualRecommendationMap.size(), is(3));

    var traineeRecommendationRecordDto = actualRecommendationMap.get(gmcNumber1);
    assertThat(traineeRecommendationRecordDto.getGmcNumber(), is(gmcNumber1));
    assertThat(traineeRecommendationRecordDto.getGmcSubmissionDate(), is(submissionDate));
    assertThat(traineeRecommendationRecordDto.getActualSubmissionDate(), is(actualSubmissionDate));
    assertThat(traineeRecommendationRecordDto.getComments(), is(comments));
    assertThat(traineeRecommendationRecordDto.getAdmin(), is(admin1));

    traineeRecommendationRecordDto = actualRecommendationMap.get(gmcNumberX);
    assertThat(traineeRecommendationRecordDto.getGmcNumber(), is(nullValue()));
  }

  @Test
  void shouldMatchTisStatusCompletedToApproved() {
    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));

    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation1));
    RecommendationStatus result = recommendationService
        .getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, is(RecommendationStatus.COMPLETED));
  }

  @Test
  void shouldMatchTisStatusCompletedToRejected() {
    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));

    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation2));
    RecommendationStatus result = recommendationService
        .getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, is(RecommendationStatus.COMPLETED));
  }

  @Test
  void shouldMatchTisStatusUnderReviewToSubmittedToGmc() {
    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));

    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation3));
    RecommendationStatus result = recommendationService
        .getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, is(SUBMITTED_TO_GMC));
  }

  @Test
  void shouldMatchTisStatusDraftToNonNullTypeAndNullOutcome() {
    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation4));
    RecommendationStatus result = recommendationService
        .getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, is(DRAFT));
  }

  @Test
  void shouldMatchTisStatusToNotStartedIfTypeAndOutcomeNull() {
    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctorsForDB1));
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation5));
    RecommendationStatus result = recommendationService
        .getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, is(NOT_STARTED));
  }

  @Test
  void shouldGetRecommendationStatusCheckDtos() {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId, SUBMITTED_TO_GMC);
    final var recommendation = buildRecommendation(gmcId, recommendationId, SUBMITTED_TO_GMC,
        UNDER_REVIEW);

    when(recommendationRepository.findAllByRecommendationStatus(RecommendationStatus.SUBMITTED_TO_GMC))
        .thenReturn(Arrays.asList(recommendation));
    when(doctorsForDBRepository.findById(recommendation.getGmcNumber())).thenReturn(Optional.of(doctorsForDB));
    List<RecommendationStatusCheckDto> result = recommendationService.getRecommendationStatusCheckDtos();
    assertThat(result.size(), is(1));
    assertThat(result.get(0).getDesignatedBodyId(), is(designatedBodyCode));
    assertThat(result.get(0).getGmcReferenceNumber(), is(gmcId));
    assertThat(result.get(0).getGmcRecommendationId(), is(gmcRecommendationId2));
    assertThat(result.get(0).getRecommendationId(), is(recommendationId));
    assertThat(result.get(0).getOutcome(), is(nullValue()));
  }

  @Test
  void shouldNotGetRecommendationStatusCheckDtosIfNoDoctorsForDbMatched() {
    final var gmcId = faker.number().digits(7);
    final var recommendation = buildRecommendation(gmcId, recommendationId, SUBMITTED_TO_GMC,
        UNDER_REVIEW);

    when(recommendationRepository.findAllByRecommendationStatus(RecommendationStatus.SUBMITTED_TO_GMC))
        .thenReturn(Arrays.asList(recommendation));
    when(doctorsForDBRepository.findById(recommendation.getGmcNumber())).thenReturn(Optional.empty());
    List<RecommendationStatusCheckDto> result = recommendationService.getRecommendationStatusCheckDtos();
    assertThat(result.size(), is(0));
  }

  @Test
  void shouldReturnNewRecommendationIfDoctorUnderNoticeButHasPastRecommendation() {
    final var doctor = buildDoctorForDB(gmcNumber1, RecommendationStatus.NOT_STARTED);
    doctor.setUnderNotice(YES);

    when(doctorsForDBRepository.findById(any())).thenReturn(Optional.of(doctor));
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
        .thenReturn(Optional.of(recommendation6));

    TraineeRecommendationRecordDto result = recommendationService.getLatestRecommendation(gmcNumber1);
    assertThat(result.getGmcOutcome(), is(nullValue()));
  }

  private DoctorsForDB buildDoctorForDB(final String gmcId,
      RecommendationStatus doctorRecommendationStatus) {
    return DoctorsForDB.builder()
        .gmcReferenceNumber(gmcId)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .doctorStatus(doctorRecommendationStatus)
        .submissionDate(submissionDate)
        .dateAdded(dateAdded)
        .underNotice(underNotice)
        .sanction(sanction)
        .designatedBodyCode(designatedBodyCode)
        .admin(admin1)
        .build();

  }

  private String getFullName(final String firstName, final String lastName) {
    return String.format("%s %s", firstName, lastName);
  }

  private TryRecommendationV2Response buildRecommendationV2Response(final String returnCode) {
    final var tryRecommendationV2Response = new TryRecommendationV2Response();
    final var tryRecommendationResponseCT = new TryRecommendationResponseCT();
    tryRecommendationResponseCT.setReturnCode(returnCode);
    tryRecommendationResponseCT.setRecommendationID(recommendationId);
    tryRecommendationV2Response.setTryRecommendationV2Result(tryRecommendationResponseCT);
    return tryRecommendationV2Response;
  }

  private Recommendation buildRecommendation(final String gmcId, final String recommendationId,
      final RecommendationStatus status,
      final RecommendationGmcOutcome outcome) {
    return Recommendation.builder()
        .id(recommendationId)
        .gmcNumber(gmcId)
        .recommendationStatus(status)
        .recommendationType(RecommendationType.REVALIDATE)
        .admin(admin1)
        .gmcRevalidationId(gmcRecommendationId2)
        .gmcSubmissionDate(submissionDate)
        .actualSubmissionDate(actualSubmissionDate)
        .outcome(outcome)
        .comments(comments)
        .build();
  }

  private RoUserProfileDto buildRoUserProfileDto(final String gmcId) {
    return RoUserProfileDto.builder()
        .gmcId(gmcId)
        .firstName(roFirstName)
        .lastName(roLastName)
        .emailAddress(roEmailAddress)
        .phoneNumber(roPhoneNumber)
        .userName(roUserName)
        .build();
  }

  private TraineeRecommendationRecordDto buildTraineeRecommendationRecordDto(
      String recommendationId, String recommendationType, LocalDate deferralDate,
      String deferralReason, String deferralSubReason, String admin) {
    return TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationId(recommendationId)
        .recommendationType(recommendationType)
        .deferralDate(deferralDate)
        .deferralReason(deferralReason)
        .deferralSubReason(deferralSubReason)
        .comments(comments)
        .admin(admin)
        .build();
  }
}