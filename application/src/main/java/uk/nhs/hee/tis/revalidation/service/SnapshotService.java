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
    return (reasonCode == null || "".equals(reasonCode)) ? null
        : deferralReasonService.getDeferralReasonByCode(reasonCode).getReason();
  }

  private String getDeferralSubReasonByCode(final String reasonCode, final String subCode) {
    return (reasonCode == null || "".equals(reasonCode)) && (subCode == null || "".equals(subCode)) ? null :
        deferralReasonService.getDeferralSubReasonByReasonCodeAndReasonSubCode(reasonCode, subCode)
            .getReason();
  }

  private String toUpperCase(final String code) {
    return !(code == null || "".equals(code)) ? code.toUpperCase() : code;
  }

}
