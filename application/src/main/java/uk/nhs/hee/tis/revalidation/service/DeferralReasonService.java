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

import static java.util.stream.Collectors.toList;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.dto.DeferralReasonDto;
import uk.nhs.hee.tis.revalidation.entity.DeferralReason;
import uk.nhs.hee.tis.revalidation.entity.Status;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.repository.DeferralReasonRepository;

@Slf4j
@Service
public class DeferralReasonService {

  @Autowired
  private DeferralReasonRepository deferralReasonRepository;

  //get All deferral reasons
  public List<DeferralReasonDto> getAllDeferralReasons() {
    final var deferralReasons = deferralReasonRepository.findAll();
    return deferralReasons.stream().map(dr -> convertToDTO(dr)).collect(toList());
  }

  //get All CURRENT deferral reasons
  public List<DeferralReasonDto> getAllCurrentDeferralReasons() {
    final var deferralReasons = deferralReasonRepository.findAllByStatus(Status.CURRENT);
    return deferralReasons.stream().map(dr -> convertToDTO(dr)).collect(toList());
  }

  /**
   * Get deferral subreason by code and sub reason code
   *
   * @param reasonCode code associated with the deferral reason
   * @return deferralReason deferral reason by code - can be null
   */
  public DeferralReason getDeferralReasonByCode(final String reasonCode) {
    final var deferralReason = deferralReasonRepository.findById(reasonCode);
    if (deferralReason.isEmpty()) {
      throw new RecommendationException("Deferral Reason code is invalid");
    }
    return deferralReason.get();
  }

  /**
   * Get deferral subreason by code and sub reason code
   *
   * @param reasonCode code associated with the deferral reason
   * @param reasonSubCode code associated with the deferral subreason
   * @return deferralSubReason deferral sub reason by code - can be null
   */
  public DeferralReason getDeferralSubReasonByReasonCodeAndReasonSubCode(final String reasonCode,
      final String reasonSubCode) {
    final var deferralReason = getDeferralReasonByCode(reasonCode);
    return deferralReason.getSubReasonByCode(reasonSubCode);
  }

  private DeferralReasonDto convertToDTO(DeferralReason dr) {
    return DeferralReasonDto.builder().code(dr.getCode()).reason(dr.getReason())
        .subReasons(dr.getDeferralSubReasons().stream()
            .map(sub -> DeferralReasonDto.builder().code(sub.getCode())
                .reason(sub.getReason()).build()).collect(toList())).build();
  }
}
