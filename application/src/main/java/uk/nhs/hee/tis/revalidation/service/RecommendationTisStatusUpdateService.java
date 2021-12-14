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

import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.REJECTED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.COMPLETED;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@Slf4j
@Service
public class RecommendationTisStatusUpdateService {

  @Autowired
  private RecommendationRepository recommendationRepository;
  @Autowired
  private DoctorsForDBRepository doctorsForDBRepository;
  @Autowired
  private RecommendationService recommendationService;
  @Autowired
  private SnapshotService snapshotService;

  public void updateRecommendationAndTisStatus(
      final RecommendationStatusCheckDto recommendationStatusCheckDto) {
    RecommendationGmcOutcome outcome = recommendationStatusCheckDto.getOutcome();
    setRecommendationStatusAndUpdateRepositories(recommendationStatusCheckDto.getRecommendationId(),
        outcome);

    String gmcReferenceNumber = recommendationStatusCheckDto.getGmcReferenceNumber();
    Optional<DoctorsForDB> optionalDoctorsForDB = doctorsForDBRepository
        .findById(gmcReferenceNumber);

    //update the tis status in doctorsfordb to "complete"
    if (optionalDoctorsForDB.isPresent() && (APPROVED.equals(outcome) || REJECTED
        .equals(outcome))) {
      final var doctorsForDB = optionalDoctorsForDB.get();
      doctorsForDB.setDoctorStatus(
          recommendationService.getRecommendationStatusForTrainee(gmcReferenceNumber));
      doctorsForDBRepository.save(doctorsForDB);
    }
  }

  //if gmc outcome is approved/rejected, update the relevant recommendation to approved/rejected,
  //tis status to complete and also update the snapshot repository
  private void setRecommendationStatusAndUpdateRepositories(final String recommendationId,
      final RecommendationGmcOutcome recommendationGmcOutcome) {

    final var optionalRecommendation = recommendationRepository.findById(recommendationId);

    if (optionalRecommendation.isPresent()) {
      Recommendation recommendation = optionalRecommendation.get();
      if (APPROVED.equals(recommendationGmcOutcome) || REJECTED.equals(recommendationGmcOutcome)) {
        recommendation.setOutcome(recommendationGmcOutcome);
        recommendation.setRecommendationStatus(COMPLETED);
        recommendationRepository.save(recommendation);
        snapshotService.saveRecommendationToSnapshot(recommendation);
      }
    }
  }
}