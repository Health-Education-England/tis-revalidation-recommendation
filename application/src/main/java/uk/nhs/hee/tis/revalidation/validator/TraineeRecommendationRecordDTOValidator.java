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

package uk.nhs.hee.tis.revalidation.validator;

import static java.lang.String.format;
import static java.time.LocalDate.now;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationType;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@Component
public class TraineeRecommendationRecordDTOValidator implements Validator {

  public static final String INSUFFICIENT_EVIDENCE = "1";
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

  private DoctorsForDBRepository doctorsForDBRepository;

  public TraineeRecommendationRecordDTOValidator(DoctorsForDBRepository doctorsForDBRepository) {
    this.doctorsForDBRepository = doctorsForDBRepository;
  }

  @Override
  public boolean supports(Class<?> aClass) {
    return TraineeRecommendationRecordDto.class.isAssignableFrom(aClass);
  }

  //validate TraineeRecommendationRecordDTO for the creation of new Recommendation
  @Override
  public void validate(Object target, Errors errors) {

    if (errors.getErrorCount() == 0) {
      final var recordDTO = (TraineeRecommendationRecordDto) target;

      if (!StringUtils.hasLength(recordDTO.getGmcNumber())) {
        errors.reject("GmcNumber", GMC_NUMBER_NOT_SPECIFIED);
      }

      if (!StringUtils.hasLength(recordDTO.getRecommendationType())) {
        errors.reject("RecommendationType", RECOMMENDATION_TYPE_NOT_SPECIFIED);
      } else {
        final var recommendationType = RecommendationType.valueOf(
            recordDTO.getRecommendationType());

        if (RecommendationType.DEFER.equals(recommendationType)) {
          validateDefer(recordDTO, errors);
        }
      }
    }
  }

  private void validateDefer(TraineeRecommendationRecordDto recordDto, Errors errors) {

    if (recordDto.getDeferralDate() == null || recordDto.getDeferralDate().isBefore(now())) {
      errors.reject("DeferralDate", DEFERRAL_DATE_NOT_SPECIFIED_OR_NOT_CORRECT);
    }
    validateIfDeferAllowed(recordDto, errors);
    validateDeferReasons(recordDto, errors);
  }

  private void validateIfDeferAllowed(TraineeRecommendationRecordDto recordDto, Errors errors) {
    if (!StringUtils.hasLength(recordDto.getGmcNumber())) {
      return;
    }
    final var doctorsForDb = doctorsForDBRepository.findById(recordDto.getGmcNumber());
    if (doctorsForDb.isEmpty()) {
      errors.reject("DoctorForDB", format(DOCTOR_NOT_FOUND_MESSAGE, recordDto.getGmcNumber()));
    } else {
      final var doctor = doctorsForDb.get();
      LocalDate gmcSubmissionDueDate = doctor.getSubmissionDate();
      if (gmcSubmissionDueDate == null
          || (ChronoUnit.DAYS.between(now(), gmcSubmissionDueDate)) > 120) {
        errors.reject("GmcSubmissionDate", DEFERRAL_NOT_PERMITTED);
      }
    }
  }

  private void validateDeferReasons(TraineeRecommendationRecordDto recordDto, Errors errors) {
    if (!StringUtils.hasLength(recordDto.getDeferralReason())) {
      errors.reject("DeferralReason", DEFERRAL_REASON_NOT_SPECIFIED);
    } else if (recordDto.getDeferralReason().equalsIgnoreCase(INSUFFICIENT_EVIDENCE)
        && !StringUtils.hasLength(recordDto.getDeferralSubReason())) {
      errors.reject("DeferralSubReason", DEFERRAL_SUB_REASON_NOT_SPECIFIED);
    }
  }
}