/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.validator;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.DEFER;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@ExtendWith(MockitoExtension.class)
class TraineeRecommendationRecordDTOValidatorTest {

  private static final String DOCTOR_NOT_FOUND_MESSAGE = "Doctor %s does not exist!";
  private static final String GMC_NUMBER_NOT_SPECIFIED = "Gmc Number can't be empty or null";
  private static final String RECOMMENDATION_TYPE_NOT_SPECIFIED =
      "Recommendation type can't be empty or null";
  private static final String DEFERRAL_DATE_NOT_SPECIFIED_OR_NOT_CORRECT =
      "Deferral date can't be empty or in past";
  private static final String DEFERRAL_NOT_PERMITTED =
      "Deferral is not permitted at this time since submission due date is greater than 120 days from today or submission due date is null";
  private static final String DEFERRAL_REASON_NOT_SPECIFIED =
      "Deferral Reason can't be empty or null";
  private static final String DEFERRAL_SUB_REASON_NOT_SPECIFIED =
      "Deferral Sub Reason can't be empty or null";

  private final static LocalDate today = now();
  private final Faker faker = new Faker();
  private final LocalDate gmcSubmissionDate = now();
  private final LocalDate deferralDate = now();
  private final String gmcId = faker.number().digits(7);
  private final String deferralReason1 = "1";
  private final String deferralSubReason1 = "1";

  @Mock
  private DoctorsForDBRepository doctorsForDBRepository;
  @InjectMocks
  private TraineeRecommendationRecordDTOValidator validatorUnderTest;

  @Test
  void shouldValidateWhenGmcNumberAndRecommendationTypeIsEmpty() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber("")
        .recommendationType("")
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullGmcOrRecommendationType");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(2));
    assertThat(errors.getObjectName(), is("nullGmcOrRecommendationType"));

    List<String> errMsgList = errors.getAllErrors().stream().map(
        e -> e.getDefaultMessage()).collect(Collectors.toList());
    assertThat(errMsgList, hasItem(GMC_NUMBER_NOT_SPECIFIED));
    assertThat(errMsgList, hasItem(RECOMMENDATION_TYPE_NOT_SPECIFIED));
  }

  @Test
  void shouldValidateWhenGmcNumberAndRecommendationTypeIsNull() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(null)
        .recommendationType(null)
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullGmcOrRecommendationType");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(2));
    assertThat(errors.getObjectName(), is("nullGmcOrRecommendationType"));

    List<String> errMsgList = errors.getAllErrors().stream().map(
        e -> e.getDefaultMessage()).collect(Collectors.toList());
    assertThat(errMsgList, hasItem(GMC_NUMBER_NOT_SPECIFIED));
    assertThat(errMsgList, hasItem(RECOMMENDATION_TYPE_NOT_SPECIFIED));
  }

  @Test
  void shouldValidateWhenRecommendationIsDeferAndDateAndReasonAreMissing() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .build();

    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.of(
        DoctorsForDB.builder().submissionDate(today).build()));

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullDeferralDateAndReason");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(2));
    assertThat(errors.getObjectName(), is("nullDeferralDateAndReason"));

    List<String> errMsgList = errors.getAllErrors().stream().map(
        e -> e.getDefaultMessage()).collect(Collectors.toList());
    assertThat(errMsgList, hasItem(DEFERRAL_DATE_NOT_SPECIFIED_OR_NOT_CORRECT));
    assertThat(errMsgList, hasItem(DEFERRAL_REASON_NOT_SPECIFIED));
  }

  @Test
  void shouldValidateWhenDeferralReasonRequiredAndSubReasonNotProvided() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate.minusDays(1))
        .deferralReason(deferralReason1)
        .deferralSubReason(null)
        .build();

    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.of(
        DoctorsForDB.builder().submissionDate(today.plusDays(120)).build()));

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullDeferralSubReason");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(2));
    assertThat(errors.getObjectName(), is("nullDeferralSubReason"));

    List<String> errMsgList = errors.getAllErrors().stream().map(
        e -> e.getDefaultMessage()).collect(Collectors.toList());
    assertThat(errMsgList, hasItem(DEFERRAL_DATE_NOT_SPECIFIED_OR_NOT_CORRECT));
    assertThat(errMsgList, hasItem(DEFERRAL_SUB_REASON_NOT_SPECIFIED));
  }

  @Test
  void shouldValidateWhenRecommendationIsDeferAndGmcSubmissionDateIsMoreThan120DaysAfterToday() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .build();

    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.of(
        DoctorsForDB.builder().submissionDate(today.plusDays(121)).build()));

    Errors errors = new BeanPropertyBindingResult(recordDTO, "gmcSubmissionDateValidation");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(1));
    assertThat(errors.getObjectName(), is("gmcSubmissionDateValidation"));
    assertThat(errors.getAllErrors().get(0).getDefaultMessage(), is(DEFERRAL_NOT_PERMITTED));
  }

  @Test
  void shouldValidateWhenDoctorInDBNotExists() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .build();

    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.empty());

    Errors errors = new BeanPropertyBindingResult(recordDTO, "gmcSubmissionDateValidation");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(1));
    assertThat(errors.getObjectName(), is("gmcSubmissionDateValidation"));

    String expectedErrorMsg = String.format(DOCTOR_NOT_FOUND_MESSAGE, gmcId);
    assertThat(errors.getAllErrors().get(0).getDefaultMessage(), is(expectedErrorMsg));
  }

  @Test
  void shouldValidateWhenDoctorsSubmissionDueDateIsNull() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "gmcSubmissionDateValidation");

    when(doctorsForDBRepository.findById(gmcId)).thenReturn(Optional.ofNullable(
        DoctorsForDB.builder().submissionDate(null).build()));

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(errors.getErrorCount(), is(1));
    assertThat(errors.getObjectName(), is("gmcSubmissionDateValidation"));
    assertThat(errors.getAllErrors().get(0).getDefaultMessage(), is(DEFERRAL_NOT_PERMITTED));
  }
}