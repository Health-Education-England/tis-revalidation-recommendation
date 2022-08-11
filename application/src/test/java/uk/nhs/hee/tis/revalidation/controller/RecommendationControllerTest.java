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

package uk.nhs.hee.tis.revalidation.controller;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.DEFER;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.NON_ENGAGEMENT;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.REVALIDATE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BindingResult;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationType;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.service.RecommendationService;
import uk.nhs.hee.tis.revalidation.validator.TraineeRecommendationRecordDTOValidator;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(RecommendationController.class)
class RecommendationControllerTest {

  private static final String RECOMMENDATION_API_URL = "/api/recommendation";
  private static final String RECOMMENDATION_API_GMCID_PATH_VARIABLE = "{gmcId}";
  private static final String RECOMMENDATION_API_SUBMIT_PATH_VARIABLE = "/{gmcId}/submit/{recommendationId}";
  private static final String RECOMMENDATION_API_LATEST_GMCIDS_PATH_VARIABLE = "latest/{gmcIds}";
  private final Faker faker = new Faker();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private RecommendationService service;

  @Captor
  ArgumentCaptor<BindingResult> bindingResultArgumentCaptor;

  @MockBean
  private DoctorsForDBRepository doctorsForDBRepository;
  @SpyBean
  private TraineeRecommendationRecordDTOValidator traineeRecommendationRecordDTOValidator;

  private String gmcId = faker.number().digits(7);
  private String recommendationId = faker.number().digits(8);
  private String firstName = faker.name().firstName();
  private String lastName = faker.name().lastName();
  private final static LocalDate today = now();
  private UnderNotice underNotice = faker.options().option(UnderNotice.class);
  private String sanction = faker.lorem().characters(2);
  private RecommendationStatus status = faker.options().option(RecommendationStatus.class);
  private final LocalDate curriculumEndDate = now();
  private String programmeName = faker.lorem().sentence(3);
  private String programmeMembershipType = faker.lorem().characters(10);
  private String currentGrade = faker.lorem().characters(4);
  private String deferralReason1 = "1";
  private String deferralReason2 = "2";
  private String deferralSubReason1 = "1";
  private final LocalDate deferralDate = now();
  private String deferralComments = faker.lorem().sentence(5);
  private String recommendationType = faker.options().option(RecommendationType.class).name();
  private String gmcOutcome = faker.options().option(RecommendationGmcOutcome.class).name();
  private final LocalDate gmcSubmissionDate = now();
  private final LocalDate actualSubmissionDate = now();
  private String admin = faker.name().fullName();

  @Test
  void shouldReturnTraineeRecommendation() throws Exception {
    final var recommendationDTO = prepareRecommendationDTO();
    when(service.getTraineeInfo(gmcId)).thenReturn(recommendationDTO);
    final var url = format("%s/%s", RECOMMENDATION_API_URL, RECOMMENDATION_API_GMCID_PATH_VARIABLE);
    this.mockMvc.perform(get(url, gmcId))
        .andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(recommendationDTO)));

  }

  @Test
  void shouldSaveRevalidateRecommendation() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(REVALIDATE.name())
        .comments(List.of())
        .build();

    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldSaveNonEngagementRecommendation() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(NON_ENGAGEMENT.name())
        .comments(List.of())
        .build();

    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldSaveDeferRecommendation() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldThrowExceptionWhenGmcIdOrRecommendationTypeMissingInRecommendationRequest()
      throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber("")
        .recommendationType("")
        .comments(List.of())
        .build();

    final var expectedErrors = List
        .of("Gmc Number can't be empty or null", "Recommendation type can't be empty or null");
    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().is4xxClientError())
        .andExpect(content().json(mapper.writeValueAsString(expectedErrors)));
  }

  @Test
  void shouldThrowExceptionWhenRecommendationIsDeferAndDateAndReasonAreMissingInRecommendationRequest()
      throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    final var expectedErrors = List
        .of("Deferral date can't be empty or in past", "Deferral Reason can't be empty or null");
    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().is4xxClientError())
        .andExpect(content().json(mapper.writeValueAsString(expectedErrors)));
  }

  @Test
  void shouldThrowExceptionWhenRecommendationIsDeferAndDeferralDateIsInPast()
      throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .deferralDate(deferralDate.minusDays(1))
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    final var expectedErrors = List.of("Deferral date can't be empty or in past");
    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().is4xxClientError())
        .andExpect(content().json(mapper.writeValueAsString(expectedErrors)));
  }

  @ParameterizedTest(name = "Deferral recommendation will be successful when GMC submission date: {0} is within or exactly 120 days from today")
  @MethodSource("gmcSubmissionDateProvider")
  void shouldNotThrowErrorWhenRecommendationIsDeferAndGmcSubmissionIsWithin120DaysOrExactlyFromToday(
      LocalDate newGmcSubmissionDate) throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(newGmcSubmissionDate)
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldThrowErrorWhenRecommendationIsDeferAndGmcSubmissionDateIsMoreThan120DaysAfterToday()
      throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(today.plusDays(121))
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            today.plusDays(121)).build()));

    final var expectedErrors = List
        .of("Deferral is not permitted at this time since submission due date is greater than 120 days from today or submission due date is null");

    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().is4xxClientError())
        .andExpect(content().json(mapper.writeValueAsString(expectedErrors)));
  }

  @Test
  void shouldThrowExceptionWhenRecommendationIfDeferralReasonRequiredSubReasonAndNotProvided()
      throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .deferralDate(deferralDate.minusDays(1))
        .deferralReason(deferralReason1)
        .deferralSubReason(null)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    final var expectedErrors = List.of("Deferral date can't be empty or in past",
        "Deferral Sub Reason can't be empty or null");
    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().is4xxClientError())
        .andExpect(content().json(mapper.writeValueAsString(expectedErrors)));
  }

  @Test
  void shouldSaveRecommendationWhenDeferralSubReasonIsNotRequired() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .deferralDate(deferralDate)
        .deferralReason(deferralReason2)
        .deferralSubReason(null)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    this.mockMvc.perform(post(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldSubmitRecommendation() throws Exception {
    final var url = format("%s/%s", RECOMMENDATION_API_URL,
        RECOMMENDATION_API_SUBMIT_PATH_VARIABLE);
    final var userProfileDto = RoUserProfileDto.builder().build();
    this.mockMvc.perform(post(url, gmcId, recommendationId)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(userProfileDto)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldUpdateRevalidateRecommendation() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationId(recommendationId)
        .recommendationType(REVALIDATE.name())
        .comments(List.of())
        .build();

    this.mockMvc.perform(put(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldUpdateDeferRecommendation() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationId(recommendationId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    when(doctorsForDBRepository.findById(any())).thenReturn(
        Optional.ofNullable(DoctorsForDB.builder().submissionDate(
            now()).build()));

    this.mockMvc.perform(put(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldFailToUpdateRevalidateRecommendationWhenNoRecommendationId() throws Exception {
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(REVALIDATE.name())
        .comments(List.of())
        .build();

    this.mockMvc.perform(put(RECOMMENDATION_API_URL)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(recordDTO)))
        .andExpect(status().isBadRequest())
        .andExpect(content().string("Recommendation Id should not be empty"));
  }

  @Test
  void shouldReturnLatestTraineeRecommendations() throws Exception {
    final var traineeRecommendationRecordDto = Map.of(gmcId, prepareRevalidationDTO());
    when(service.getLatestRecommendations(List.of(gmcId)))
        .thenReturn(traineeRecommendationRecordDto);
    final var url = format("%s/%s", RECOMMENDATION_API_URL,
        RECOMMENDATION_API_LATEST_GMCIDS_PATH_VARIABLE);
    this.mockMvc.perform(get(url, gmcId))
        .andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(traineeRecommendationRecordDto)));

  }

  private TraineeRecommendationDto prepareRecommendationDTO() {
    return TraineeRecommendationDto.builder()
        .fullName(firstName + " " + lastName)
        .gmcNumber(gmcId)
        .currentGrade(currentGrade)
        .programmeMembershipType(programmeMembershipType)
        .curriculumEndDate(curriculumEndDate)
        .revalidations(List.of(prepareRevalidationDTO()))
        .build();
  }

  private TraineeRecommendationRecordDto prepareRevalidationDTO() {
    return TraineeRecommendationRecordDto.builder()
        .deferralComment(deferralComments)
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .gmcOutcome(gmcOutcome)
        .recommendationType(recommendationType)
        .admin(admin)
        .gmcSubmissionDate(gmcSubmissionDate)
        .actualSubmissionDate(actualSubmissionDate)
        .build();
  }

  private static Stream<Arguments> gmcSubmissionDateProvider() {
    return Stream.of(
        Arguments.of(today.plusDays(120)),
        Arguments.of(today.plusDays(5)),
        Arguments.of(today.plusDays(1)),
        Arguments.of(today.minusDays(1)),
        Arguments.of(today.minusDays(120)),
        Arguments.of(today.minusDays(121))
    );
  }
}