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

import static java.time.LocalDate.now;
import static java.util.stream.Collectors.toList;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDate;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDateTime;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.parseDate;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.Snapshot;
import uk.nhs.hee.tis.revalidation.entity.SnapshotRevalidation;
import uk.nhs.hee.tis.revalidation.repository.SnapshotRepository;

@Slf4j
@Transactional
@Service
public class SnapshotService {

  @Autowired
  private SnapshotRepository snapshotRepository;

  @Autowired
  private DeferralReasonService deferralReasonService;

  @Autowired
  private GmcClientService gmcClientService;

  public Snapshot saveRecommendationToSnapshot(final Recommendation recommendation) {
    log.info("Creating snapshot record for recommendation: {}, gmcId: {}", recommendation.getId(),
        recommendation.getGmcNumber());
    final var snapshot = Snapshot.builder()
        .gmcNumber(recommendation.getGmcNumber())
        .revalidation(SnapshotRevalidation.builder()
            .id(recommendation.getId())
            .proposedOutcomeCode(recommendation.getRecommendationType().name())
            .gmcOutcomeCode(recommendation.getOutcome().getOutcome())
            .gmcRecommendationId(recommendation.getGmcRevalidationId())
            .deferralDate(parseDate(recommendation.getDeferralDate()))
            .deferralReason(getDeferralReasonByCode(recommendation.getDeferralReason()))
            .deferralSubReason(getDeferralSubReasonByCode(recommendation.getDeferralReason(),
                recommendation.getDeferralSubReason()))
            .revalidationStatusCode(recommendation.getRecommendationStatus().name())
            .gmcSubmissionDateTime(parseDate(recommendation.getGmcSubmissionDate()))
            .gmcOutcomeCode(recommendation.getOutcome().getOutcome())
            .submissionDate(parseDate(recommendation.getActualSubmissionDate()))
            .dateAdded(now().toString())
            .admin(recommendation.getAdmin())
            .recommendationSubmitter(recommendation.getAdmin())
            .comments(recommendation.getComments())
            .build())
        .build();

    log.debug("Saving snapshot : {}", snapshot);
    return snapshotRepository.save(snapshot);
  }

  public List<TraineeRecommendationRecordDto> getSnapshotRecommendations(
      final DoctorsForDB doctorsForDB) {
    final var gmcId = doctorsForDB.getGmcReferenceNumber();
    final var snapshots = snapshotRepository.findByGmcNumber(gmcId);
    return snapshots.stream().map(snapshot -> {
      final var snapshotRecommendation = snapshot.getRevalidation();
      return TraineeRecommendationRecordDto.builder()
          .recommendationId(snapshotRecommendation.getId())
          .gmcNumber(gmcId)
          .deferralDate(formatDate(snapshotRecommendation.getDeferralDate()))
          .deferralReason(snapshotRecommendation.getDeferralReason())
          .deferralSubReason(snapshotRecommendation.getDeferralSubReason())
          .deferralComment(snapshotRecommendation.getDeferralComment())
          .gmcOutcome(gmcClientService
              .checkRecommendationStatus(gmcId, snapshotRecommendation.getGmcRecommendationId(),
                  snapshotRecommendation.getId(), doctorsForDB.getDesignatedBodyCode())
              .getOutcome())
          .recommendationStatus(toUpperCase(snapshotRecommendation.getRevalidationStatusCode()))
          .recommendationType(toUpperCase(snapshotRecommendation.getProposedOutcomeCode()))
          .gmcSubmissionDate(formatDateTime(snapshotRecommendation.getGmcSubmissionDateTime()))
          .actualSubmissionDate(formatDate(snapshotRecommendation.getSubmissionDate()))
          .comments(snapshotRecommendation.getComments())
          .admin(snapshotRecommendation.getAdmin())
          .build();
    }).collect(toList());
  }

  private String getDeferralReasonByCode(final String reasonCode) {
    if(!StringUtils.hasLength(reasonCode)) {
      return null;
    }
    final var reason = deferralReasonService.getDeferralReasonByCode(reasonCode);
    return reason != null ? reason.getReason() : null;
  }

  private String getDeferralSubReasonByCode(final String reasonCode, final String subCode) {
    if(!(StringUtils.hasLength(reasonCode) || StringUtils.hasLength(subCode))){
      return null;
    }
    final var subReason = deferralReasonService
        .getDeferralSubReasonByReasonCodeAndReasonSubCode(reasonCode, subCode);
    return subReason != null ? subReason.getReason() : null;
  }

  private String toUpperCase(final String code) {
    return StringUtils.hasLength(code) ? code.toUpperCase() : code;
  }

}
