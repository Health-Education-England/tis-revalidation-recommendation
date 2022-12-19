package uk.nhs.hee.tis.revalidation.changelog;

import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.READY_TO_REVIEW;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ChangeLog(order = "006")
public class SetDraftRecommendationsActualSubmissionDatesToNull {

  @ChangeSet(order = "001", id = "setDraftRecommendationsActualSubmissionDatesToNull", author = "")
  public void setActualSubmissionDatesToNull(
      RecommendationRepository recommendationRepository
  ) {
    recommendationRepository
        .findAllByRecommendationStatus(READY_TO_REVIEW).forEach(
        recommendation -> {
          if (recommendation.getActualSubmissionDate() != null) {
            recommendation.setActualSubmissionDate(null);
            recommendationRepository.save(recommendation);
          }
        }
    );
  }
}
