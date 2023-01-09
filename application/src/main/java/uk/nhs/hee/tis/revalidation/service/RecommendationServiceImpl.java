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

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static uk.nhs.hee.tis.revalidation.entity.GmcResponseCode.SUCCESS;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.REJECTED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.UNDER_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.READY_TO_REVIEW;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.SUBMITTED_TO_GMC;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.GmcResponseCode;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationType;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@Slf4j
@Transactional
@Service
public class RecommendationServiceImpl implements RecommendationService {

  private static final int MIN_DAYS_FROM_SUBMISSION_DATE = 60;
  private static final int MAX_DAYS_FROM_SUBMISSION_DATE = 365;
  private static final String DOCTOR_NOT_FOUND_MESSAGE = "Doctor %s does not exist!";

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

  @Autowired
  private RabbitTemplate rabbitTemplate;

  @Value("${app.rabbit.reval.exchange}")
  private String revalExchange;

  @Value("${app.rabbit.reval.routingKey.recommendationstatuscheck.requested}")
  private String revalRoutingKeyRecommendationStatus;


  /**
   * Cron job to send RecommendationStatusDtos to Rabbit queue for GMC recommendation status check.
   */
  @Scheduled(cron = "${app.gmc.recommendationstatuscheck.cronExpression}")
  @SchedulerLock(name = "RecommendationStatusCheckJob")
  public void sendRecommendationStatusRequestToRabbit() {
    log.info("Start cron job: sendRecommendationStatusRequestToRabbit()");
    getRecommendationStatusCheckDtos().forEach(
        recommendation -> rabbitTemplate
            .convertAndSend(revalExchange, revalRoutingKeyRecommendationStatus, recommendation));
  }

  /**
   * Get trainee information with current and legacy recommendations
   *
   * @param gmcId - GMC Identifier/Number of the trainee requested
   * @return A {@link TraineeRecommendationDto} if found, null otherwise
   */
  public TraineeRecommendationDto getTraineeInfo(String gmcId) {
    log.info("Fetching trainee info for GmcId: {}", gmcId);
    final var optionalDoctorsForDB = doctorsForDBRepository.findById(gmcId);

    if (optionalDoctorsForDB.isPresent()) {
      final var doctorsForDB = optionalDoctorsForDB.get();

      return TraineeRecommendationDto.builder()
          .fullName(String.format("%s %s",
              doctorsForDB.getDoctorFirstName(), doctorsForDB.getDoctorLastName()))
          .gmcNumber(doctorsForDB.getGmcReferenceNumber())
          .underNotice(doctorsForDB.getUnderNotice().value())
          .designatedBody(doctorsForDB.getDesignatedBodyCode())
          .gmcSubmissionDate(doctorsForDB.getSubmissionDate())
          .revalidations(getCurrentAndLegacyRecommendation(doctorsForDB))
          .deferralReasons(deferralReasonService.getAllCurrentDeferralReasons())
          .build();
    }

    return null;
  }

  /**
   * Save a new recommendation
   *
   * @param recordDTO The recommendation to save
   * @return The persisted Recommendation entity (rather than a DTO)
   * @throws RecommendationException when the Doctor doesn't exist or the Deferral date is invalid
   */
  public Recommendation saveRecommendation(TraineeRecommendationRecordDto recordDTO) {
    isSaveRecommendationPermitted(recordDTO.getGmcNumber(), recordDTO.getRecommendationId());

    final var doctorsForDB = doctorsForDBRepository.findById(recordDTO.getGmcNumber());
    if (doctorsForDB.isEmpty()) {
      throw new RecommendationException(
          format(DOCTOR_NOT_FOUND_MESSAGE, recordDTO.getGmcNumber()));
    }
    final var doctor = doctorsForDB.get();
    final var submissionDate = doctor.getSubmissionDate();

    final var recommendationType = RecommendationType.valueOf(recordDTO.getRecommendationType());
    Recommendation recommendation;

    switch (recommendationType) {
      case REVALIDATE:
      case NON_ENGAGEMENT:
        recommendation = Recommendation.builder()
            .id(recordDTO.getRecommendationId())
            .gmcNumber(recordDTO.getGmcNumber())
            .recommendationType(recommendationType)
            .recommendationStatus(READY_TO_REVIEW)
            .comments(recordDTO.getComments())
            .gmcSubmissionDate(submissionDate)
            .admin(recordDTO.getAdmin())
            .build();
        break;
      case DEFER:
      default:
        final var deferralDate = recordDTO.getDeferralDate();
        if (isDeferralDateValid(deferralDate, submissionDate)) {
          final var deferralReason =
              deferralReasonService.getDeferralReasonByCode(recordDTO.getDeferralReason());
          final var deferralSubReason =
              deferralReason.getSubReasonByCode(recordDTO.getDeferralSubReason());
          final var deferralSubReasonCode =
              deferralSubReason != null ? deferralSubReason.getCode() : null;

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
        break;
    }

    Recommendation savedRecommendation = recommendationRepository.save(recommendation);
    doctor.setLastUpdatedDate(now());
    doctor.setDoctorStatus(
        getRecommendationStatusForTrainee(recordDTO.getGmcNumber())
    );
    doctorsForDBRepository.save(doctor);
    return savedRecommendation;
  }

  /**
   * Update an existing recommendation
   *
   * @param recordDTO Modified TraineeRecommendationDto containing the Recommendation
   * @return The updated Recommendation
   * @throws RecommendationException when the Recommendation fails validation
   */
  public Recommendation updateRecommendation(TraineeRecommendationRecordDto recordDTO) {
    validateRecommendationExists(recordDTO.getRecommendationId());
    return saveRecommendation(recordDTO);
  }

  /**
   * @param recommendationId The persistence id for the Recommendation to be submitted
   * @param gmcNumber        The Doctor's GMC ID/Number
   * @param userProfileDto   The details of the Responsible Officer to submit on behalf of.
   * @return Flag indicating the Recommendation was submitted to the GMC service
   * @throws RecommendationException when the Doctor is unknown to TIS or GMC submission fails
   */
  public boolean submitRecommendation(String recommendationId, String gmcNumber,
      RoUserProfileDto userProfileDto) {
    log.info("submitting request to gmc for recommendation: {} and gmcNumber: {}", recommendationId,
        gmcNumber);
    final var doctorsForDB = doctorsForDBRepository.findById(gmcNumber);
    final var recommendation = recommendationRepository
        .findByIdAndGmcNumber(recommendationId, gmcNumber);

    if (doctorsForDB.isEmpty()) {
      throw new RecommendationException(format(DOCTOR_NOT_FOUND_MESSAGE, gmcNumber));
    }
    final var doctor = doctorsForDB.get();

    final var tryRecommendationV2Response = gmcClientService.submitToGmc(doctor, recommendation,
        userProfileDto);
    final var tryRecommendationResponseCT = tryRecommendationV2Response
        .getTryRecommendationV2Result();
    if (tryRecommendationResponseCT != null) {
      log.info("Receive response for submit request for gmcId: {} with return code: {}",
          doctor.getGmcReferenceNumber(), tryRecommendationResponseCT.getReturnCode());
      final var returnCode = tryRecommendationResponseCT.getReturnCode();
      if (SUCCESS.getCode().equals(returnCode)) {
        recommendation.setRecommendationStatus(SUBMITTED_TO_GMC);
        recommendation.setOutcome(UNDER_REVIEW);
        recommendation.setActualSubmissionDate(now());
        recommendation.setGmcRevalidationId(tryRecommendationResponseCT.getRecommendationID());
        recommendationRepository.save(recommendation);
        doctor.setLastUpdatedDate(now());
        doctor.setDoctorStatus(getRecommendationStatusForTrainee(gmcNumber)
        );
        doctorsForDBRepository.save(doctor);
        return true;
      } else {
        final var responseCode = GmcResponseCode.fromCode(returnCode);
        log.error(
            "Submission of recommendation to GMC failed for GmcId: {} and RecommendationId: {}. Gmc response is: {}",
            gmcNumber, recommendation.getId(), responseCode.getMessage());
        throw new RecommendationException(
            format("Fail to submit recommendation: %s", responseCode.getMessage()));
      }
    }
    return false;
  }

  /**
   * Get latest recommendation of a trainee
   *
   * @param gmcId The GMC ID/Number of the Trainee to search for
   * @return The last submitted Recommendation or empty
   */
  public TraineeRecommendationRecordDto getLatestRecommendation(String gmcId) {
    log.info("Fetching latest recommendation info for GmcId: {}", gmcId);
    final var recommendations = recommendationRepository.findByGmcNumber(gmcId);
    final var draftRecommendations = recommendations.stream().filter(
        recommendation -> recommendation.getRecommendationStatus().name().equals("READY_TO_REVIEW"))
        .collect(Collectors.toList());
    //Check draft recommendation: if yes return draft else normal original flow
    if (draftRecommendations.size() > 0) {
      final var draftRecommendation = draftRecommendations.get(0);
      return buildTraineeRecommendationRecordDto(draftRecommendation.getGmcNumber(),
          draftRecommendation.getGmcSubmissionDate(), draftRecommendation);
    } else {
      final var optionalDoctorsForDB = doctorsForDBRepository.findById(gmcId);
      final var optionalRecommendation = recommendationRepository
          .findFirstByGmcNumberOrderByActualSubmissionDateDesc(gmcId);
      if (!optionalDoctorsForDB.isPresent()) {
        throw new RecommendationException(format(DOCTOR_NOT_FOUND_MESSAGE, gmcId));
      }

      if (optionalRecommendation.isPresent()) {
        final Recommendation recommendation = optionalRecommendation.get();

        final boolean isPastCompletedRecommendation = checkIfPastCompletedRecommendation(
            recommendation,
            optionalDoctorsForDB.get()
        );

        if (!isPastCompletedRecommendation) {
          return buildTraineeRecommendationRecordDto(recommendation.getGmcNumber(),
              recommendation.getGmcSubmissionDate(), recommendation);
        }
      }
      return new TraineeRecommendationRecordDto();
    }
  }

  /**
   * Get latest recommendation for each of a list of trainees
   *
   * @param gmcIds The GMC IDs/Numbers of the Trainees to search for
   * @return A mapping of input GMC Numbers to the last submitted Recommendation for each which may
   * be empty
   */
  public Map<String, TraineeRecommendationRecordDto> getLatestRecommendations(
      List<String> gmcIds) {
    log.info("Mapping latest recommendation info for GmcIds: {}", gmcIds);
    return gmcIds.stream().collect(toMap(identity(), this::getLatestRecommendation));
  }

  public RecommendationStatus getRecommendationStatusForTrainee(String gmcId) {
    TraineeRecommendationRecordDto recommendation = getLatestRecommendation(gmcId);
    String outcome = recommendation.getGmcOutcome();
    String type = recommendation.getRecommendationType();

    if (outcome == null && type == null) {
      return RecommendationStatus.NOT_STARTED;
    } else if (APPROVED.getOutcome().equals(outcome) || REJECTED.getOutcome().equals(outcome)) {
      return RecommendationStatus.COMPLETED;
    } else if (UNDER_REVIEW.getOutcome().equals(outcome)) {
      return RecommendationStatus.SUBMITTED_TO_GMC;
    } else {
      return RecommendationStatus.DRAFT;
    }
  }

  /**
   * Get recommendation status check dtos
   *
   * @return A list of submitted to gmc recommendation
   */
  public List<RecommendationStatusCheckDto> getRecommendationStatusCheckDtos() {
    List<RecommendationStatusCheckDto> recommendationStatusCheckDtos = new ArrayList<>();
    recommendationRepository
        .findAllByRecommendationStatus(RecommendationStatus.SUBMITTED_TO_GMC).forEach(rec -> {
      final var doctorsForDB = doctorsForDBRepository.findById(rec.getGmcNumber());
      if (doctorsForDB.isPresent() && rec.getGmcRevalidationId() != null) {
        final var recommendationStatusDto = RecommendationStatusCheckDto.builder()
            .designatedBodyId(doctorsForDB.get().getDesignatedBodyCode())
            .gmcReferenceNumber(rec.getGmcNumber())
            .gmcRecommendationId(rec.getGmcRevalidationId())
            .recommendationId(rec.getId())
            .build();
        recommendationStatusCheckDtos.add(recommendationStatusDto);
      }
    });
    return recommendationStatusCheckDtos;
  }

  /**
   * Get all Recommendations for a Doctor
   *
   * @param doctorsForDB The Doctor to search for
   * @return A list of all Recommendations for the Doctor
   */
  private List<TraineeRecommendationRecordDto> getCurrentAndLegacyRecommendation(
      final DoctorsForDB doctorsForDB) {
    final var gmcNumber = doctorsForDB.getGmcReferenceNumber();

    log.info("Fetching snapshot record for GmcId: {}", gmcNumber);
    final var recommendations = recommendationRepository.findByGmcNumber(gmcNumber);
    final var currentRecommendations = recommendations.stream().map(rec ->
        buildTraineeRecommendationRecordDto(gmcNumber, doctorsForDB.getSubmissionDate(), rec)
    ).collect(toList());

    final var snapshotRecommendations = snapshotService.getSnapshotRecommendations(doctorsForDB);
    currentRecommendations.addAll(snapshotRecommendations);

    //sort most recent first (note nulls are typically drafts and will be first due to reversed())
    currentRecommendations.sort(
        Comparator.comparing(TraineeRecommendationRecordDto::getActualSubmissionDate,
            Comparator.nullsFirst(Comparator.reverseOrder()))
    );
    return currentRecommendations;
  }

  private String getOutcome(RecommendationGmcOutcome outcome) {
    return Objects.nonNull(outcome) ? outcome.getOutcome() : null;
  }

  /**
   * Validates deferral date, which should be at least after 60 days from submission date and less
   * than 365 days from submission date
   *
   * @param deferralDate   The date that the revalidation is deferred until
   * @param submissionDate The date that the Recommendation to Defer is submitted
   * @return A flag indicating whether the deferral date is valid
   */
  private boolean isDeferralDateValid(final LocalDate deferralDate,
      final LocalDate submissionDate) {
    final var submissionDateWith60Days = submissionDate.plusDays(MIN_DAYS_FROM_SUBMISSION_DATE);
    final var submissionDateWith365Days = submissionDate.plusDays(MAX_DAYS_FROM_SUBMISSION_DATE);

    return deferralDate.isAfter(submissionDateWith60Days)
        && deferralDate.isBefore(submissionDateWith365Days);
  }

  /**
   * Check if the request exists. This is called before updating a recommendationId
   *
   * @param recommendationId The database ID of a recommendation
   * @throws RecommendationException when no Recommendation is found
   */
  private void validateRecommendationExists(final String recommendationId) {
    log.info("Request for the update of existing recommendation: {}", recommendationId);
    final var existingRecommendation = recommendationRepository.findById(recommendationId);
    if (existingRecommendation.isEmpty()) {
      throw new RecommendationException(
          "No recommendation record found against given recommendationId");
    }
  }

  /**
   * Checks and returns whether a Doctor's recommendations are in a state that permits saving the
   * Recommendation with the specified database identifier. The {@param recommendationId}
   * <ol>
   *   <li>If recommendation for trainee is already in draft state, admins are not allowed to
   *   create a new one but are allowed to update</li>
   *   <li>If recommendation for trainee is Submitted to gmc but still in Under Review state,
   *   admin are not allowed to create a new one</li>
   * </ol>
   *
   * @param gmcNumber        The GMC ID/Number of the Doctor to check against
   * @param recommendationId The recommendation to check if we can save
   * @throws RecommendationException where I'd expect this to return false
   */
  private void isSaveRecommendationPermitted(final String gmcNumber,
      final String recommendationId) {
    final var inProgressFilter = new InProgressPredicate(recommendationId);
    recommendationRepository.findByGmcNumber(gmcNumber).stream()
        .filter(inProgressFilter)
        .findFirst().ifPresent(r -> {
      throw new RecommendationException(
          "Trainee already has a recommendation in draft or waiting for approval from GMC.");
    });
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
        .recommendationStatus(
            (rec.getRecommendationStatus() != null) ? rec.getRecommendationStatus().name() : null)
        .recommendationType(
            (rec.getRecommendationType() != null) ? rec.getRecommendationType().name() : null)
        .gmcSubmissionDate(submissionDate)
        .actualSubmissionDate(rec.getActualSubmissionDate())
        .admin(rec.getAdmin())
        .comments(rec.getComments())
        .build();
  }

  private boolean checkIfPastCompletedRecommendation(Recommendation recommendation,
      DoctorsForDB doctor) {
    final boolean approved = APPROVED.equals(recommendation.getOutcome());
    final boolean underNotice = doctor.getUnderNotice().equals(YES);
    //TODO find more empirical timeframe
    final boolean notRecent = recommendation.getActualSubmissionDate() != null
        && recommendation.getActualSubmissionDate()
        .isBefore(LocalDate.now().minusMonths(1));

    return approved && underNotice && notRecent;
  }

  /**
   * This predicate evaluates whether a recommendation is "In Progress".  This includes those with a
   * `COMPLETED` status of {@link RecommendationStatus} and excludes a {@link Recommendation} with
   * the id provided to the Predicate's constructor
   */
  public class InProgressPredicate implements Predicate<Recommendation> {

    private final String recommendationId;

    public InProgressPredicate(String recommendationId) {
      this.recommendationId = recommendationId;
    }

    @Override
    public boolean test(Recommendation r) {
      if (r.getId().equals(recommendationId)) {
        //check if the request is for update / exclude recommendation specified
        return false;
      }
      return SUBMITTED_TO_GMC != r.getRecommendationStatus() || UNDER_REVIEW == r.getOutcome();
    }
  }
}
