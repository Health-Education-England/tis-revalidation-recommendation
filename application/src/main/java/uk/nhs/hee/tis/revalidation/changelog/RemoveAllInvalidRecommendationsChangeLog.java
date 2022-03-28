package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.SUBMITTED_TO_GMC;

import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ChangeLog(order = "004")
public class RemoveAllInvalidRecommendationsChangeLog {

  @ChangeSet(order = "001", id = "removeAllInvalidRecommendations", author = "")
  public void removeAllInvalidRecommendations(
      RecommendationRepository recommendationRepository
  ) {
    recommendationRepository
        .findAllByRecommendationStatus(SUBMITTED_TO_GMC).forEach(
        recommendation -> {
          if (recommendation.getGmcRevalidationId() == null) {
            recommendationRepository.delete(recommendation);
          }
        }
    );
  }
}
