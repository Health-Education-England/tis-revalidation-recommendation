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

import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.INVALID_RECOMMENDATION;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.*;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.*;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.DEFER;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.NON_ENGAGEMENT;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.REVALIDATE;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDate;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.javafaker.Faker;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationResponseCT;
import uk.nhs.hee.tis.gmc.client.generated.TryRecommendationV2Response;
import uk.nhs.hee.tis.revalidation.dto.DeferralReasonDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
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
  private String deferralResaon1, deferralResaon2;
  private String deferralSubResaon1, deferralSubResaon2;
  private String gmcOutcome1, gmcOutcome2;
  private String revalidatonType1, revalidatonType2;
  private String revalidationStatus1, revalidationStatus2;
  private String gmcSubmissionDate1, gmcSubmissionDate2;
  private String acutalSubmissionDate1, acutalSubmissionDate2;
  private String gmcRecommendationId1, gmcRecommendationId2;
  private String admin1, admin2;

  private String gmcNumber1, gmcNumber2;
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
  private Recommendation recommendation4, recommendation5;
  private DoctorsForDbDto docDto1;

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
    deferralResaon1 = "1";
    deferralSubResaon1 = "4";
    revalidatonType1 = faker.options().option(RecommendationType.class).name();
    revalidationStatus1 = faker.options().option(RecommendationStatus.class).name();
    gmcOutcome1 = APPROVED.getOutcome();
    gmcSubmissionDate1 = "2018-03-15 12:00:00";
    acutalSubmissionDate1 = "2018-03-15";
    admin1 = faker.internet().emailAddress();
    gmcRecommendationId1 = faker.number().digits(10);
    snapshotRevalidationId1 = faker.number().digits(10);

    deferralComment2 = faker.lorem().characters(20);
    deferralDate2 = LocalDate.of(2018, 03, 15);
    deferralResaon2 = "2";
    deferralSubResaon2 = null;
    revalidatonType2 = null;
    revalidationStatus2 = null;
    gmcOutcome2 = APPROVED.getOutcome();
    gmcSubmissionDate2 = "2018-03-15 12:00:00";
    acutalSubmissionDate2 = "2018-03-15";
    admin2 = faker.internet().emailAddress();
    gmcRecommendationId2 = faker.number().digits(10);
    snapshotRevalidationId2 = faker.number().digits(10);

    gmcNumber1 = faker.number().digits(7);
    gmcNumber2 = faker.number().digits(7);
    comments = List
        .of(faker.lorem().sentence(3), faker.lorem().sentence(3), faker.lorem().sentence(7));
    recommendationId = faker.lorem().characters(10);
    newRecommendationId = faker.lorem().characters(10);
    deferralReasons = List.of(new DeferralReasonDto("1", "evidence", "SICK_CARERS_LEAVE", List.of(), Status.CURRENT),
        new DeferralReasonDto("2", "ongoing", "ONGOING_PROCESS",  List.of(), Status.INACTIVE));

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
  }

  @Test
  void shouldReturnRecommendationWithCurrentAndLegacyRecommendations()
      throws ParseException {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(of(doctorsForDB));
    when(snapshotService.getSnapshotRecommendations(doctorsForDB))
        .thenReturn(List.of(snapshot1, snapshot2));
    when(recommendationRepository.findByGmcNumber(gmcId)).thenReturn(List.of(
        buildRecommendation(gmcNumber1, recommendationId, status, REVALIDATE, UNDER_REVIEW)));
    when(deferralReasonService.getAllCurrentDeferralReasons()).thenReturn(deferralReasons);

    when(snapshot1.getAdmin()).thenReturn(admin1);
    when(snapshot1.getGmcNumber()).thenReturn(gmcId);
    when(snapshot1.getDeferralComment()).thenReturn(deferralComment1);
    when(snapshot1.getDeferralReason()).thenReturn(deferralResaon1);
    when(snapshot1.getDeferralDate()).thenReturn(deferralDate1);
    when(snapshot1.getRecommendationStatus()).thenReturn(revalidationStatus1);
    when(snapshot1.getRecommendationType()).thenReturn(revalidatonType1);
    when(snapshot1.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot1.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate1));
    when(snapshot1.getActualSubmissionDate()).thenReturn(formatDate(acutalSubmissionDate1));
    when(snapshot1.getGmcRevalidationId()).thenReturn(gmcRecommendationId1);
    when(snapshot1.getRecommendationId()).thenReturn(snapshotRevalidationId1);

    when(snapshot2.getAdmin()).thenReturn(admin2);
    when(snapshot2.getGmcNumber()).thenReturn(gmcId);
    when(snapshot2.getDeferralComment()).thenReturn(deferralComment2);
    when(snapshot2.getDeferralReason()).thenReturn(deferralResaon2);
    when(snapshot2.getDeferralDate()).thenReturn(deferralDate2);
    when(snapshot2.getRecommendationStatus()).thenReturn(revalidationStatus2);
    when(snapshot2.getRecommendationType()).thenReturn(revalidatonType2);
    when(snapshot2.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot2.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate2));
    when(snapshot2.getActualSubmissionDate()).thenReturn(formatDate(acutalSubmissionDate2));
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
    assertThat(revalidationDTO.getDeferralReason(), is(deferralResaon1));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate1));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment1));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome1));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidatonType1));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus1));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate1)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(acutalSubmissionDate1)));
    assertThat(revalidationDTO.getRecommendationId(), is(snapshotRevalidationId1));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId1));

    revalidationDTO = recommendation.getRevalidations().get(2);
    assertThat(revalidationDTO.getGmcNumber(), is(gmcId));
    assertThat(revalidationDTO.getDeferralReason(), is(deferralResaon2));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate2));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment2));
    assertThat(revalidationDTO.getAdmin(), is(admin2));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome2));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidatonType2));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus2));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate2)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(acutalSubmissionDate2)));
    assertThat(revalidationDTO.getRecommendationId(), is(snapshotRevalidationId2));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId2));
  }

  @Test
  void shouldUpdateSnapshotAndDoctorIfRecommendationStatusBecomeApprove() {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(of(doctorsForDB));
    when(snapshotService.getSnapshotRecommendations(doctorsForDB)).thenReturn(List.of(snapshot1));
    final var recommendation1 = buildRecommendation(gmcNumber1, recommendationId, SUBMITTED_TO_GMC,
        REVALIDATE, APPROVED);
    when(recommendationRepository.findByGmcNumber(gmcId)).thenReturn(List.of(recommendation1));
    when(gmcClientService.checkRecommendationStatus(gmcNumber1,
        gmcRecommendationId2, recommendationId, designatedBodyCode)).thenReturn(APPROVED);
    when(deferralReasonService.getAllCurrentDeferralReasons()).thenReturn(deferralReasons);
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcId))

    when(snapshot1.getAdmin()).thenReturn(admin1);
    when(snapshot1.getGmcNumber()).thenReturn(gmcId);
    when(snapshot1.getDeferralComment()).thenReturn(deferralComment1);
    when(snapshot1.getDeferralReason()).thenReturn(deferralResaon1);
    when(snapshot1.getDeferralDate()).thenReturn(deferralDate1);
    when(snapshot1.getRecommendationStatus()).thenReturn(revalidationStatus1);
    when(snapshot1.getRecommendationType()).thenReturn(revalidatonType1);
    when(snapshot1.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot1.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate1));
    when(snapshot1.getActualSubmissionDate()).thenReturn(formatDate(acutalSubmissionDate1));
    when(snapshot1.getGmcRevalidationId()).thenReturn(gmcRecommendationId1);
    when(snapshot1.getRecommendationId()).thenReturn(snapshotRevalidationId1);

    final var recommendation = recommendationService.getTraineeInfo(gmcId);
    assertThat(recommendation.getGmcNumber(), is(gmcId));
    assertThat(recommendation.getFullName(), is(getFullName(firstName, lastName)));
    assertThat(recommendation.getUnderNotice(), is(underNotice.value()));
    assertThat(recommendation.getDesignatedBody(), is(designatedBodyCode));
    assertThat(recommendation.getGmcSubmissionDate(), is(submissionDate));
    assertThat(recommendation.getDeferralReasons(), hasSize(2));

    assertThat(recommendation.getRevalidations(), hasSize(2));
    var revalidationDTO = recommendation.getRevalidations().get(0);
    assertThat(revalidationDTO.getGmcOutcome(), is(APPROVED.getOutcome()));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getRecommendationType(), is(REVALIDATE.name()));
    assertThat(revalidationDTO.getRecommendationStatus(), is(SUBMITTED_TO_GMC.name()));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(submissionDate));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(actualSubmissionDate));
    assertThat(revalidationDTO.getRecommendationId(), is(recommendationId));
    assertThat(revalidationDTO.getComments(), is(comments));

    revalidationDTO = recommendation.getRevalidations().get(1);
    assertThat(revalidationDTO.getGmcNumber(), is(gmcId));
    assertThat(revalidationDTO.getDeferralReason(), is(deferralResaon1));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate1));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment1));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome1));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidatonType1));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus1));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate1)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(acutalSubmissionDate1)));
    assertThat(revalidationDTO.getRecommendationId(), is(snapshotRevalidationId1));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId1));

    verify(recommendationRepository).save(recommendation1);
    verify(snapshotService).saveRecommendationToSnapshot(recommendation1);
    verify(doctorsForDBRepository).save(doctorsForDB);
  }

  @Test
  void shouldReturnCurrentRecommendationWhichAreSubmittedToGMC() throws ParseException {
    final var gmcId = faker.number().digits(7);
    final var doctorsForDB = buildDoctorForDB(gmcId);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(of(doctorsForDB));
    when(snapshotService.getSnapshotRecommendations(doctorsForDB)).thenReturn(List.of(snapshot1));
    when(recommendationRepository.findByGmcNumber(gmcId))
        .thenReturn(List.of(buildRecommendation(gmcId, newRecommendationId,
            SUBMITTED_TO_GMC, REVALIDATE, UNDER_REVIEW)));
    when(deferralReasonService.getAllCurrentDeferralReasons()).thenReturn(deferralReasons);

    when(snapshot1.getAdmin()).thenReturn(admin1);
    when(snapshot1.getDeferralComment()).thenReturn(deferralComment1);
    when(snapshot1.getDeferralReason()).thenReturn(deferralResaon1);
    when(snapshot1.getDeferralDate()).thenReturn(deferralDate1);
    when(snapshot1.getRecommendationStatus()).thenReturn(revalidationStatus1);
    when(snapshot1.getRecommendationType()).thenReturn(revalidatonType1);
    when(snapshot1.getGmcOutcome()).thenReturn(gmcOutcome1);
    when(snapshot1.getGmcSubmissionDate()).thenReturn(formatDateTime(gmcSubmissionDate1));
    when(snapshot1.getActualSubmissionDate()).thenReturn(formatDate(acutalSubmissionDate1));
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
    assertThat(revalidationDTO.getDeferralReason(), is(deferralResaon1));
    assertThat(revalidationDTO.getDeferralDate(), is(deferralDate1));
    assertThat(revalidationDTO.getDeferralComment(), is(deferralComment1));
    assertThat(revalidationDTO.getAdmin(), is(admin1));
    assertThat(revalidationDTO.getGmcOutcome(), is(gmcOutcome1));
    assertThat(revalidationDTO.getRecommendationType(), is(revalidatonType1));
    assertThat(revalidationDTO.getRecommendationStatus(), is(revalidationStatus1));
    assertThat(revalidationDTO.getGmcSubmissionDate(), is(formatDateTime(gmcSubmissionDate1)));
    assertThat(revalidationDTO.getActualSubmissionDate(), is(formatDate(acutalSubmissionDate1)));
    assertThat(revalidationDTO.getGmcRevalidationId(), is(gmcRecommendationId1));
    assertThat(revalidationDTO.getRecommendationId(), is(recommendationId));
  }

  @Test
  void shouldReturnRecommendationWithCurrentAndLegacyRevalidationsWithoutTisCoreInformation() {
    final var gmcId = faker.number().digits(8);
    final var doctorsForDB = buildDoctorForDB(gmcId);
    when(doctorsForDBRepository.findById(gmcId)).thenReturn(of(doctorsForDB));
    final var recommendation = recommendationService.getTraineeInfo(gmcId);
    assertThat(recommendation.getGmcNumber(), is(gmcId));
    assertThat(recommendation.getFullName(), is(getFullName(firstName, lastName)));
    assertThat(recommendation.getCctDate(), is(nullValue()));
    assertThat(recommendation.getProgrammeMembershipType(), is(nullValue()));
    assertThat(recommendation.getCurrentGrade(), is(nullValue()));
    assertThat(recommendation.getRevalidations(), hasSize(0));
  }

  @Test
  void shouldSaveRevalidateRecommendationInDraftState() throws JsonProcessingException {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(REVALIDATE.name())
        .comments(comments)
        .admin(admin1)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldSaveNonEngagementRecommendationInDraftState() {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(NON_ENGAGEMENT.name())
        .comments(comments)
        .admin(admin1)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldSaveDeferRecommendationInDraftState() {
    final var deferralDate = LocalDate.now().plusDays(90);
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralResaon1)
        .deferralSubReason(deferralSubResaon1)
        .comments(comments)
        .admin(admin1)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralResaon1)).thenReturn(deferralReason);
    when(deferralReason.getSubReasonByCode(deferralSubResaon1)).thenReturn(deferralSubReason);

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldSaveDeferRecommendationInDraftStateWhenDeferralReasonIsNotRequiredSubReason() {
    final var deferralDate = LocalDate.now().plusDays(90);
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralResaon2)
        .deferralSubReason(null)
        .comments(comments)
        .admin(admin1)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralResaon2)).thenReturn(deferralReason);
    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldThrowExceptionWhenDeferralDateWithin60DaysOfSubmissionDate() {
    final var deferralDate = submissionDate.plusDays(59);
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralResaon1)
        .deferralSubReason(deferralSubResaon1)
        .comments(comments)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralResaon1)).thenReturn(deferralReason);
    when(deferralReason.getSubReasonByCode(deferralSubResaon1)).thenReturn(deferralSubReason);

    Assertions.assertThrows(RecommendationException.class, () -> {
      recommendationService.saveRecommendation(recordDTO);
    });
  }

  @Test
  void shouldThrowExceptionWhenDeferralDateMoreThen365DaysOfSubmissionDate() {
    final var deferralDate = submissionDate.plusDays(366);
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralResaon1)
        .deferralSubReason(deferralSubResaon1)
        .comments(comments)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralResaon1)).thenReturn(deferralReason);
    when(deferralReason.getSubReasonByCode(deferralSubResaon1)).thenReturn(deferralSubReason);

    Assertions.assertThrows(RecommendationException.class, () -> {
      recommendationService.saveRecommendation(recordDTO);
    });
  }

  @Test
  void shouldSubmitRecommendation() {
    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status, REVALIDATE,
        UNDER_REVIEW);
    final var userProfileDto = getUserProfileDto(gmcNumber1);
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(recommendationRepository.findByIdAndGmcNumber(recommendationId, gmcNumber1))
        .thenReturn(recommendation);
    when(gmcClientService.submitToGmc(doctorsForDB, recommendation, userProfileDto))
        .thenReturn(buildRecommendationV2Response(SUCCESS.getCode()));
    recommendationService.submitRecommendation(recommendationId, gmcNumber1, userProfileDto);
    verify(recommendationRepository).save(recommendation);
  }

  @Test
  void shouldNotUpdateRecommendationWhenSubmitFail() {
    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status, REVALIDATE,
        UNDER_REVIEW);
    final var userProfileDto = getUserProfileDto(gmcNumber1);
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(recommendationRepository.findByIdAndGmcNumber(recommendationId, gmcNumber1))
        .thenReturn(recommendation);
    when(gmcClientService.submitToGmc(doctorsForDB, recommendation, userProfileDto))
        .thenReturn(buildRecommendationV2Response(INVALID_RECOMMENDATION.getCode()));

    Assertions.assertThrows(RecommendationException.class, () -> {
      recommendationService.submitRecommendation(recommendationId, gmcNumber1, userProfileDto);
    });
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

    final var recommendation = buildRecommendation(gmcNumber1, recommendationId, status, REVALIDATE,
        UNDER_REVIEW);
    when(recommendationRepository.findByGmcNumber(gmcNumber1)).thenReturn(List.of(recommendation));
    when(recommendationRepository.findById(recommendationId))
        .thenReturn(Optional.of(recommendation));
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.updateRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldUpdateNonEngagementRecommendationInDraftState() {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationId(recommendationId)
        .recommendationType(NON_ENGAGEMENT.name())
        .comments(comments)
        .build();

    when(recommendationRepository.findById(recommendationId)).thenReturn(Optional
        .of(buildRecommendation(gmcNumber1, recommendationId, status, REVALIDATE, UNDER_REVIEW)));
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);

    recommendationService.updateRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldUpdateDeferRecommendationInDraftState() {
    final var deferralDate = LocalDate.now().plusDays(90);
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationId(recommendationId)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralResaon1)
        .deferralSubReason(deferralSubResaon1)
        .comments(comments)
        .build();

    when(recommendationRepository.findById(recommendationId)).thenReturn(Optional
        .of(buildRecommendation(gmcNumber1, recommendationId, status, REVALIDATE, UNDER_REVIEW)));
    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    when(deferralReasonService.getDeferralReasonByCode(deferralResaon1)).thenReturn(deferralReason);
    when(deferralReason.getSubReasonByCode(deferralSubResaon1)).thenReturn(deferralSubReason);

    recommendationService.updateRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldThrowExceptionWhenInvalidRecommendationIdProvidedForUpdate() {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationId(recommendationId)
        .recommendationType(REVALIDATE.name())
        .comments(comments)
        .build();

    Assertions.assertThrows(RecommendationException.class, () -> {
      recommendationService.updateRecommendation(recordDTO);
    });
  }

  @Test
  void shouldAllwedSaveRecommendationWhenOneAlreadyInSubmittedAndRejectedState() {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(REVALIDATE.name())
        .comments(comments)
        .build();

    when(doctorsForDBRepository.findById(gmcNumber1)).thenReturn(Optional.of(doctorsForDB));
    when(doctorsForDB.getSubmissionDate()).thenReturn(submissionDate);
    final var draftRecommendation1 = Recommendation.builder().id(recommendationId)
        .gmcNumber(gmcNumber1)
        .recommendationStatus(SUBMITTED_TO_GMC).outcome(REJECTED).build();
    when(recommendationRepository.findByGmcNumber(gmcNumber1))
        .thenReturn(List.of(draftRecommendation1));

    recommendationService.saveRecommendation(recordDTO);

    verify(recommendationRepository).save(anyObject());
  }

  @Test
  void shouldNotSaveRecommendationWhenOneAlreadyInDraft() throws JsonProcessingException {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(REVALIDATE.name())
        .comments(comments)
        .build();

    final var draftRecommendation1 = Recommendation.builder().id(recommendationId)
        .gmcNumber(gmcNumber1).recommendationStatus(READY_TO_REVIEW).build();
    when(recommendationRepository.findByGmcNumber(gmcNumber1))
        .thenReturn(List.of(draftRecommendation1));
    Assertions.assertThrows(RecommendationException.class, () -> {
      recommendationService.saveRecommendation(recordDTO);
    });
  }

  @Test
  void shouldNotSaveRecommendationWhenStatusIsSubmittedButStillUnderReview()
      throws JsonProcessingException {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber1)
        .recommendationType(REVALIDATE.name())
        .comments(comments)
        .build();

    final var draftRecommendation1 = Recommendation.builder().id(recommendationId)
        .gmcNumber(gmcNumber1)
        .recommendationStatus(SUBMITTED_TO_GMC).outcome(UNDER_REVIEW).build();
    when(recommendationRepository.findByGmcNumber(gmcNumber1))
        .thenReturn(List.of(draftRecommendation1));

    Assertions.assertThrows(RecommendationException.class, () -> {
      recommendationService.saveRecommendation(recordDTO);
    });
  }

  @Test
  void shouldReturnLatestRecommendations() throws ParseException {
    final var gmcId = faker.number().digits(7);
    final var recommendation = buildRecommendation(gmcId, recommendationId, status, REVALIDATE,
        UNDER_REVIEW);
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
  void shouldMatchTisStatusCompletedToApproved() {
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
            .thenReturn(Optional.of(recommendation1));
    RecommendationStatus result = recommendationService.getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, Matchers.is(RecommendationStatus.COMPLETED));
  }

  @Test
  void shouldMatchTisStatusCompletedToRejected() {
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
            .thenReturn(Optional.of(recommendation2));
    RecommendationStatus result = recommendationService.getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, Matchers.is(RecommendationStatus.COMPLETED));
  }

  @Test
  void shouldMatchTisStatusUnderReviewToSubmittedToGmc() {
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
            .thenReturn(Optional.of(recommendation3));
    RecommendationStatus result = recommendationService.getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, Matchers.is(SUBMITTED_TO_GMC));
  }

  @Test
  void shouldMatchTisStatusDraftToNonNullTypeAndNullOutcome() {
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
            .thenReturn(Optional.of(recommendation4));
    RecommendationStatus result = recommendationService.getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, Matchers.is(DRAFT));
  }

  @Test
  void shouldMatchTisStatusToNotStartedIfTypeAndOutcomeNull() {
    when(recommendationRepository.findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcNumber1))
            .thenReturn(Optional.of(recommendation5));
    RecommendationStatus result = recommendationService.getRecommendationStatusForTrainee(gmcNumber1);
    assertThat(result, Matchers.is(NOT_STARTED));
  }

  private DoctorsForDB buildDoctorForDB(final String gmcId) {
    return DoctorsForDB.builder()
        .gmcReferenceNumber(gmcId)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .doctorStatus(status)
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
      final RecommendationStatus status, final RecommendationType recommendationType,
      final RecommendationGmcOutcome outcome) {
    return Recommendation.builder()
        .id(recommendationId)
        .gmcNumber(gmcId)
        .recommendationStatus(status)
        .recommendationType(recommendationType)
        .admin(admin1)
        .gmcRevalidationId(gmcRecommendationId2)
        .gmcSubmissionDate(submissionDate)
        .actualSubmissionDate(actualSubmissionDate)
        .outcome(outcome)
        .comments(comments)
        .build();
  }

  private RoUserProfileDto getUserProfileDto(final String gmcId) {
    return RoUserProfileDto.builder()
        .gmcId(gmcId)
        .firstName(roFirstName)
        .lastName(roLastName)
        .emailAddress(roEmailAddress)
        .phoneNumber(roPhoneNumber)
        .userName(roUserName)
        .build();
  }
}