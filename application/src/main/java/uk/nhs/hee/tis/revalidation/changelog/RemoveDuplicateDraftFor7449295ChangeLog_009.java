package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import java.util.Optional;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ChangeLog(order = "009")
public class RemoveDuplicateDraftFor7449295ChangeLog_009 {

  private static final String RECOMMENDATION_ID = "65b79baae6d8ff00033dd1b7";

  @ChangeSet(order="001", id = "removeInvalidRecommendationForDoctor7449295", author = "")
  public void removeDuplicateDraftFor7449295(
      RecommendationRepository recommendationRepository
  ) {
    Optional<Recommendation> recommendation = recommendationRepository.findById(
        RECOMMENDATION_ID
    );
    if(recommendation.isPresent()) {
      recommendationRepository.delete(recommendation.get());
    }
  }

}
