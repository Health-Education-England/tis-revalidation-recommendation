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

package uk.nhs.hee.tis.revalidation.repository;

import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.REJECTED;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {

  Recommendation findByIdAndGmcNumber(final String id, final String gmcNumber);

  Optional<Recommendation> findFirstByGmcNumberOrderByActualSubmissionDateDesc(
      final String gmcNumber);

  //get recommendation which can be update, APPROVED and REJECTED recommendation cannot be update and will be fetch from snapshot
  default List<Recommendation> findByGmcNumber(final String gmcNumber) {
    return findAllByGmcNumberAndOutcomeNotIn(gmcNumber, APPROVED, REJECTED);
  }

  List<Recommendation> findAllByGmcNumberAndOutcomeNotIn(final String gmcNumber,
      final RecommendationGmcOutcome... outcome);

  List<Recommendation> findAllByRecommendationStatus(
      final RecommendationStatus recommendationStatus);

  Optional<Recommendation> findFirstByGmcNumberOrderByGmcSubmissionDateDesc(String gmcNumber);
}
