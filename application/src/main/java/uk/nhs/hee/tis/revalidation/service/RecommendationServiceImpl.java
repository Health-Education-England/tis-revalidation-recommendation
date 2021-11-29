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
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.fromCode;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.REJECTED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.UNDER_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.READY_TO_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.SUBMITTED_TO_GMC;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.NON_ENGAGEMENT;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationType.REVALIDATE;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.*;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@Slf4j
@Transactional
@Service
public class RecommendationServiceImpl implements RecommendationService {

  private static final int MIN_DAYS_FROM_SUBMISSION_DATE = 60;
  private static final int MAX_DAYS_FROM_SUBMISSION_DATE = 365;

  @Autowired
  private DoctorsForDBRepository doctorsForDBRepository;

  @Autowired
  private SnapshotService snapshotService;

  @Autowired
  private RecommendationRepository recommendationRepository;

  @Autowired
  private DeferralReasonService deferralReasonService;

  @Autowired
  private GmcClientService gmcClientService;

  //get trainee information with current and legacy recommendations
  public TraineeRecommendationDto getTraineeInfo(String gmcId) {
    RecommendationServiceImpl.log.info("Fetching trainee info for GmcId: {}", gmcId);
    final var optionalDoctorsForDB = doctorsForDBRepository.findById(gmcId);

    if (optionalDoctorsForDB.isPresent()) {
      final var doctorsForDB = optionalDoctorsForDB.get();

      return TraineeRecommendationDto.builder()
              .fullName(
                      String.format("%s %s", doctorsForDB.getDoctorFirstName(), doctorsForDB.getDoctorLastName()))
              .gmcNumber(doctorsForDB.getGmcReferenceNumber())
              .underNotice(doctorsForDB.getUnderNotice().value())
              .designatedBody(doctorsForDB.getDesignatedBodyCode())
              .gmcSubmissionDate(doctorsForDB.getSubmissionDate())
              .revalidations(getCurrentAndLegacyRecommendation(doctorsForDB))
              .deferralReasons(deferralReasonService.getAllCurrentDeferralReasons()).build();
    }

    return null;
  }

  //save a new recommendation
  public Recommendation saveRecommendation(TraineeRecommendationRecordDto recordDTO) {
    isAllowedToCreateNewRecommendation(recordDTO.getGmcNumber(), recordDTO.getRecommendationId());

    final var doctorsForDB = doctorsForDBRepository.findById(recordDTO.getGmcNumber());
    if(doctorsForDB.isPresent()) {
      final var doctor = doctorsForDB.get();
      final var submissionDate = doctor.getSubmissionDate();

      final var recommendationType = RecommendationType.valueOf(recordDTO.getRecommendationType());
      Recommendation recommendation = null;

      if (REVALIDATE.equals(recommendationType) || NON_ENGAGEMENT.equals(recommendationType)) {
        recommendation = Recommendation.builder()
                .id(recordDTO.getRecommendationId())
                .gmcNumber(recordDTO.getGmcNumber())
                .recommendationType(recommendationType)
                .recommendationStatus(READY_TO_REVIEW)
                .comments(recordDTO.getComments())
                .gmcSubmissionDate(submissionDate)
                .admin(recordDTO.getAdmin())
                .build();
      } else {
        final var deferralDate = recordDTO.getDeferralDate();
        final var validateDeferralDate = validateDeferralDate(deferralDate, submissionDate);
        final var deferralReason = deferralReasonService
                .getDeferralReasonByCode(recordDTO.getDeferralReason());
        final var deferralSubReason = deferralReason
                .getSubReasonByCode(recordDTO.getDeferralSubReason());
        final var deferralSubReasonCode =
                deferralSubReason != null ? deferralSubReason.getCode() : null;
        if (validateDeferralDate) {
          recommendation = Recommendation.builder()
                  .id(recordDTO.getRecommendationId())
                  .gmcNumber(recordDTO.getGmcNumber())
                  .comments(recordDTO.getComments())
                  .recommendationType(recommendationType)
                  .recommendationStatus(READY_TO_REVIEW)
                  .deferralDate(recordDTO.getDeferralDate())
                  .deferralReason(deferralReason.getCode())
                  .deferralSubReason(deferralSubReasonCode)
                  .gmcSubmissionDate(submissionDate)
                  .admin(recordDTO.getAdmin())
                  .build();
        } else {
          throw new RecommendationException(format(
                  "Deferral date is invalid, should be in between of 60 and 365 days of Gmc Submission Date: %s",
                  submissionDate));
        }
      }

      recommendation.setActualSubmissionDate(now());
      Recommendation recommendationToSave = recommendationRepository.save(recommendation);
      doctor.setLastUpdatedDate(now());
      doctor.setDoctorStatus(
              getRecommendationStatusForTrainee(recordDTO.getGmcNumber())
      );
      doctorsForDBRepository.save(doctor);
      return recommendationToSave;
    }
    else {
      throw new RecommendationException(format(
        "Doctor %s does not exist!", recordDTO.getGmcNumber())
      );
    }
  }

  //update an existing recommendation
  public Recommendation updateRecommendation(TraineeRecommendationRecordDto recordDTO) {
    validationRecommendationId(recordDTO.getRecommendationId());
    return saveRecommendation(recordDTO);
  }

  //submit a recommendation to gmc
  public boolean submitRecommendation(String recommendationId, String gmcNumber,
                                       RoUserProfileDto userProfileDto) {
    RecommendationServiceImpl.log.info("submitting request to gmc for recommendation: {} and gmcNumber: {}", recommendationId,
            gmcNumber);
    final var doctorsForDB = doctorsForDBRepository.findById(gmcNumber);
    final var recommendation = recommendationRepository
            .findByIdAndGmcNumber(recommendationId, gmcNumber);

    if(doctorsForDB.isPresent()) {
      final var doctor = doctorsForDB.get();

      final var tryRecommendationV2Response = gmcClientService.submitToGmc(doctor, recommendation,
              userProfileDto);
      final var tryRecommendationV2Result = tryRecommendationV2Response
              .getTryRecommendationV2Result();
      if (tryRecommendationV2Result != null) {
        RecommendationServiceImpl.log.info("Receive response for submit request for gmcId: {} with return code: {}",
                doctor.getGmcReferenceNumber(), tryRecommendationV2Result.getReturnCode());
        final var returnCode = tryRecommendationV2Result.getReturnCode();
        if (SUCCESS.getCode().equals(returnCode)) {
          recommendation.setRecommendationStatus(SUBMITTED_TO_GMC);
          recommendation.setOutcome(UNDER_REVIEW);
          recommendation.setActualSubmissionDate(now());
          recommendation.setGmcRevalidationId(tryRecommendationV2Result.getRecommendationID());
          recommendationRepository.save(recommendation);
          doctor.setLastUpdatedDate(now());
          doctor.setDoctorStatus(
                  getRecommendationStatusForTrainee(gmcNumber)
          );
          doctorsForDBRepository.save(doctor);
          return true;
        } else {
          final var responseCode = fromCode(returnCode);
          RecommendationServiceImpl.log.error(
                  "Submission of recommendation to GMC is failed for GmcId: {} and RecommendationId: {}. Gmc response is: {}",
                  gmcNumber, recommendation.getId(), responseCode.getMessage());
          throw new RecommendationException(
                  format("Fail to submit recommendation: %s", responseCode.getMessage()));
        }
      }
    } else {
      throw new RecommendationException(format(
              "Doctor %s does not exist!", gmcNumber)
      );
    }
    return false;
  }

  //get latest recommendations of a trainee
  public TraineeRecommendationRecordDto getLatestRecommendation(String gmcId) {
    RecommendationServiceImpl.log.info("Fetching latest recommendation info for GmcId: {}", gmcId);
    Optional<Recommendation> optionalRecommendation = recommendationRepository
            .findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcId);

    if (optionalRecommendation.isPresent()) {
      final var recommendation = optionalRecommendation.get();

      return buildTraineeRecommendationRecordDto(recommendation.getGmcNumber(),
              recommendation.getGmcSubmissionDate(), recommendation);
    }
    return new TraineeRecommendationRecordDto();
  }

  //get latest recommendations of a list of trainees
  public Map<String, TraineeRecommendationRecordDto> getLatestRecommendations(
          List<String> gmcIds) {
    RecommendationServiceImpl.log.info("Mapping latest recommendation info for GmcIds: {}", gmcIds);
    return gmcIds.stream().collect(toMap(identity(), this::getLatestRecommendation));
  }

  public RecommendationStatus getRecommendationStatusForTrainee(String gmcId) {
    TraineeRecommendationRecordDto recommendation = getLatestRecommendation(gmcId);
    String outcome = recommendation.getGmcOutcome();
    String type = recommendation.getRecommendationType();

    if(outcome == null && type == null) {
      return RecommendationStatus.NOT_STARTED;
    }
    else if(outcome == null) {
      return RecommendationStatus.DRAFT;
    }
    else if(outcome.equals(APPROVED.getOutcome())
            || outcome.equals(REJECTED.getOutcome())
    ) {
      return RecommendationStatus.COMPLETED;
    }
    else if(outcome.equals(UNDER_REVIEW.getOutcome())) {
      return RecommendationStatus.SUBMITTED_TO_GMC;
    }
    else {
      return RecommendationStatus.DRAFT;
    }
  }

  private List<TraineeRecommendationRecordDto> getCurrentAndLegacyRecommendation(
      final DoctorsForDB doctorsForDB) {
    final var gmcNumber = doctorsForDB.getGmcReferenceNumber();
    checkRecommendationStatus(gmcNumber, doctorsForDB.getDesignatedBodyCode());
    log.info("Fetching snapshot record for GmcId: {}", gmcNumber);

    final var recommendations = recommendationRepository.findByGmcNumber(gmcNumber);
    final var currentRecommendations = recommendations.stream().map(rec ->
      buildTraineeRecommendationRecordDto(gmcNumber, doctorsForDB.getSubmissionDate(), rec)
    ).collect(toList());

    final var snapshotRecommendations = snapshotService.getSnapshotRecommendations(doctorsForDB);
    currentRecommendations.addAll(snapshotRecommendations);
    return currentRecommendations;
  }

  // And if GMC update status with Approved or Rejected, recommendation will be moved to snapshot.
  private void checkRecommendationStatus(final String gmcNumber, final String designatedBody) {
    log.info("Check status of Gmc outcome for under review recommendations of GmcNumber: {}",
        gmcNumber);
    final var recommendations = recommendationRepository.findByGmcNumber(gmcNumber);
    recommendations.stream().forEach(rec -> {
      log.debug("Checking recommendation status for recommendationId: {}", rec.getId());
      final var recommendationGmcOutcome = gmcClientService
          .checkRecommendationStatus(rec.getGmcNumber(),
              rec.getGmcRevalidationId(), rec.getId(), designatedBody);
      if (APPROVED.equals(recommendationGmcOutcome) || REJECTED.equals(recommendationGmcOutcome)) {
        log.debug("Update status to: {}, for GmcId: {}", recommendationGmcOutcome,
            rec.getGmcNumber());
        rec.setOutcome(recommendationGmcOutcome);
        recommendationRepository.save(rec);
        snapshotService.saveRecommendationToSnapshot(rec);
      }
    });
  }

  private String getOutcome(RecommendationGmcOutcome outcome) {
    return Objects.nonNull(outcome) ? outcome.getOutcome() : null;
  }

  //Deferral date should be atleast after 60 days from submission date and max up to 365 days from submission date
  private boolean validateDeferralDate(final LocalDate deferralDate,
      final LocalDate submissionDate) {
    final var submissionDateWith60Days = submissionDate.plusDays(MIN_DAYS_FROM_SUBMISSION_DATE);
    final var submissionDateWith365Days = submissionDate.plusDays(MAX_DAYS_FROM_SUBMISSION_DATE);

    return deferralDate.isAfter(submissionDateWith60Days) && deferralDate
        .isBefore(submissionDateWith365Days);
  }

  //before saving new recommendationId, check if the request is for update
  private void validationRecommendationId(final String recommendationId) {
    log.info("Request for the update of existing recommendation: {}", recommendationId);
    final var existingRecommendation = recommendationRepository.findById(recommendationId);
    if (existingRecommendation.isEmpty()) {
      throw new RecommendationException(
          "No recommendation record found against given recommendationId");
    }
  }

  //if recommendation for trainee is already in draft state, admins are not allowed to create a new one but are allowed to update
  //if recommendation for trainee is Submitted to gmc but still in Under Review state, admin are not allowed to create a new one.
  private void isAllowedToCreateNewRecommendation(final String gmcNumber,
      final String recommendationId) {
    final var recommendations = recommendationRepository.findByGmcNumber(gmcNumber);
    final var recommendation = recommendations.stream().filter(r -> {
      if (r.getId().equals(recommendationId)) { //check if the request is for update
        return false;
      }
      return SUBMITTED_TO_GMC != r.getRecommendationStatus() || UNDER_REVIEW == r.getOutcome();
    }).findFirst();

    if (recommendation.isPresent()) {
      throw new RecommendationException(
          "Trainee already have an recommendation in draft state or waiting for approval from GMC.");
    }
  }

  private TraineeRecommendationRecordDto buildTraineeRecommendationRecordDto(String gmcNumber,
      LocalDate submissionDate, Recommendation rec) {
    return TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcNumber)
        .recommendationId(rec.getId())
        .deferralDate(rec.getDeferralDate())
        .deferralReason(rec.getDeferralReason())
        .deferralSubReason(rec.getDeferralSubReason())
        .gmcOutcome(getOutcome(rec.getOutcome()))
        .recommendationStatus((rec.getRecommendationStatus() != null)
            ? rec.getRecommendationStatus().name() : null)
        .recommendationType((rec.getRecommendationType() != null)
            ? rec.getRecommendationType().name() : null)
        .gmcSubmissionDate(submissionDate)
        .actualSubmissionDate(rec.getActualSubmissionDate())
        .admin(rec.getAdmin())
        .comments(rec.getComments())
        .build();
  }
}