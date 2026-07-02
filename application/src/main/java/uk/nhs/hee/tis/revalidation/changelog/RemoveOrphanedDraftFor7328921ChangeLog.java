package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import java.util.Optional;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ChangeLog(order = "008")
public class RemoveOrphanedDraftFor7328921ChangeLog {

  private static final String RECOMMENDATION_ID = "651ef176bf65a715e9bc469a";

  @ChangeSet(order="001", id = "removeInvalidRecommendationForDoctor7328921", author = "")
  public void removeOrphanedDraftForDoctor7328921(
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
