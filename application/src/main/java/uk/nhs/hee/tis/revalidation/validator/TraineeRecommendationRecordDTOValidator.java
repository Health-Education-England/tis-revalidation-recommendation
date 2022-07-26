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

import static java.time.LocalDate.now;

import java.time.temporal.ChronoUnit;
import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationType;

public class TraineeRecommendationRecordDTOValidator implements Validator {

  public static final String INSUFFICIENT_EVIDENCE = "1";

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
        errors.reject("GmcNumber", "Gmc Number can't be empty or null");
      }
      if (!StringUtils.hasLength(recordDTO.getRecommendationType())) {
        errors.reject("RecommendationType", "Recommendation type can't be empty or null");
      } else {
        final var recommendationType = RecommendationType
            .valueOf(recordDTO.getRecommendationType());
        if (RecommendationType.DEFER.equals(recommendationType)) {
          if (recordDTO.getDeferralDate() == null || recordDTO.getDeferralDate().isBefore(now())) {
            errors.reject("DeferralDate", "Deferral date can't be empty or in past");
          }
          //Doctor X has a submission due date of 120 days from today (today <= 120 days)
          if (recordDTO.getGmcSubmissionDate() == null
              || !((ChronoUnit.DAYS.between(now(), recordDTO.getGmcSubmissionDate())) <= 120)) {
            errors.reject("GmcSubmissionDate",
                "Deferral is not permitted at this time since submission due date is greater than 120 days from today");
          }
          if (!StringUtils.hasLength(recordDTO.getDeferralReason())) {
            errors.reject("DeferralReason", "Deferral Reason can't be empty or null");
          } else if (recordDTO.getDeferralReason().equalsIgnoreCase(INSUFFICIENT_EVIDENCE)
              && !StringUtils.hasLength(recordDTO.getDeferralSubReason())) {
            errors.reject("DeferralSubReason", "Deferral Sub Reason can't be empty or null");
          }
        }
      }
    }
  }
}