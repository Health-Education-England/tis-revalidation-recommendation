package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import java.util.Optional;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ChangeLog(order = "008")
public class RemoveDuplicateDraftFor7409676ChangeLog {

  private static final String RECOMMENDATION_ID = "65c2415de6d8ff00033dd29b";

  @ChangeSet(order="001", id = "removeInvalidRecommendationForDoctor7409676", author = "")
  public void removeDuplicateDraftFor7409676(
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
