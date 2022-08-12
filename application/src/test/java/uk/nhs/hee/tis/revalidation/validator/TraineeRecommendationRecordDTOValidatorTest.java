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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.DEFER;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@ExtendWith(MockitoExtension.class)
class TraineeRecommendationRecordDTOValidatorTest {

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
  private DoctorsForDB doctorsForDB;

  @Test
  void shouldValidateWhenGmcNumberOrRecommendationTypeIsEmptyInRecommendationRequest() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber("")
        .recommendationType("")
        .comments(List.of())
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullGmcOrRecommendationType");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(true, is(errors.hasErrors()));
    assertThat("nullGmcOrRecommendationType", is(errors.getObjectName()));
    assertThat("GmcNumber", is(errors.getAllErrors().get(0).getCode()));
    assertThat("Gmc Number can't be empty or null",
        is(errors.getAllErrors().get(0).getDefaultMessage()));
    assertThat("RecommendationType", is(errors.getAllErrors().get(1).getCode()));
    assertThat("Recommendation type can't be empty or null",
        is(errors.getAllErrors().get(1).getDefaultMessage()));
  }

  @Test
  void shouldValidateWhenGmcNumberOrRecommendationTypeIsNullInRecommendationRequest() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(null)
        .recommendationType(null)
        .comments(List.of())
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullGmcOrRecommendationType");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(true, is(errors.hasErrors()));
    assertThat("nullGmcOrRecommendationType", is(errors.getObjectName()));
    assertThat("GmcNumber", is(errors.getAllErrors().get(0).getCode()));
    assertThat("Gmc Number can't be empty or null",
        is(errors.getAllErrors().get(0).getDefaultMessage()));
    assertThat("RecommendationType", is(errors.getAllErrors().get(1).getCode()));
    assertThat("Recommendation type can't be empty or null",
        is(errors.getAllErrors().get(1).getDefaultMessage()));
  }

  @Test
  void shouldValidateWhenRecommendationIsDeferAndDateAndReasonAreMissingInRecommendationRequest() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .comments(List.of())
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullDeferralDateAndReason");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(true, is(errors.hasErrors()));
    assertThat("nullDeferralDateAndReason", is(errors.getObjectName()));
    assertThat("DeferralDate", is(errors.getAllErrors().get(0).getCode()));
    assertThat("Deferral date can't be empty or in past",
        is(errors.getAllErrors().get(0).getDefaultMessage()));
    assertThat("DeferralReason", is(errors.getAllErrors().get(1).getCode()));
    assertThat("Deferral Reason can't be empty or null",
        is(errors.getAllErrors().get(1).getDefaultMessage()));
  }

  @Test
  void shouldValidateWhenDeferralReasonRequiredAndSubReasonNotProvidedInRecommendationRequest() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(gmcSubmissionDate)
        .deferralDate(deferralDate.minusDays(1))
        .deferralReason(deferralReason1)
        .deferralSubReason(null)
        .comments(List.of())
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "nullDeferralSubReason");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(true, is(errors.hasErrors()));
    assertThat("nullDeferralSubReason", is(errors.getObjectName()));
    assertThat("DeferralDate", is(errors.getAllErrors().get(0).getCode()));
    assertThat("Deferral date can't be empty or in past",
        is(errors.getAllErrors().get(0).getDefaultMessage()));
    assertThat("DeferralSubReason", is(errors.getAllErrors().get(1).getCode()));
    assertThat("Deferral Sub Reason can't be empty or null",
        is(errors.getAllErrors().get(1).getDefaultMessage()));
  }

  @Test
  void shouldValidateWhenRecommendationIsDeferAndGmcSubmissionDateIsMoreThan120DaysAfterToday() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(today.plusDays(121))
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();
    when(doctorsForDBRepository.findById(recordDTO.getGmcNumber())).thenReturn(Optional.ofNullable(
        DoctorsForDB.builder().submissionDate(today.plusDays(121)).build()));

    Errors errors = new BeanPropertyBindingResult(recordDTO, "gmcSubmissionDateValidation");

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(true, is(errors.hasErrors()));
    assertThat("gmcSubmissionDateValidation", is(errors.getObjectName()));
    assertThat("GmcSubmissionDate", is(errors.getAllErrors().get(0).getCode()));
    assertThat(
        "Deferral is not permitted at this time since submission due date is greater than 120 days from today or submission due date is null",
        is(errors.getAllErrors().get(0).getDefaultMessage()));
  }

  @Test
  void shouldThrowExceptionWhenDoctorsForDbIsEmpty() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber("6576811")
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(today.plusDays(121))
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "gmcSubmissionDateValidation");

    when(doctorsForDBRepository.findById(recordDTO.getGmcNumber())).thenReturn(Optional.empty());

    //When
    Exception exception = assertThrows(RecommendationException.class, () -> {
      validatorUnderTest.validate(recordDTO, errors);
    });

    //Then
    assertThat("Doctor 6576811 does not exist!", is(exception.getMessage()));
  }

  @Test
  void shouldThrowExceptionWhenDoctorsSubmissionDueDateIsNull() {

    //Given
    final var recordDTO = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcId)
        .recommendationType(DEFER.name())
        .gmcSubmissionDate(today.plusDays(121))
        .deferralDate(deferralDate)
        .deferralReason(deferralReason1)
        .deferralSubReason(deferralSubReason1)
        .comments(List.of())
        .build();

    Errors errors = new BeanPropertyBindingResult(recordDTO, "gmcSubmissionDateValidation");

    when(doctorsForDBRepository.findById(recordDTO.getGmcNumber())).thenReturn(Optional.ofNullable(
        DoctorsForDB.builder().submissionDate(null).build()));

    //When
    validatorUnderTest.validate(recordDTO, errors);

    //Then
    assertThat(true, is(errors.hasErrors()));
    assertThat("gmcSubmissionDateValidation", is(errors.getObjectName()));
    assertThat("GmcSubmissionDate", is(errors.getAllErrors().get(0).getCode()));
    assertThat(
        "Deferral is not permitted at this time since submission due date is greater than 120 days from today or submission due date is null",
        is(errors.getAllErrors().get(0).getDefaultMessage()));
  }
}