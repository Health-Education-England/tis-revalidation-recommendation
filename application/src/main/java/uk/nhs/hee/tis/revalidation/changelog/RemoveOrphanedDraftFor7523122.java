package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import java.util.Optional;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.Snapshot;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;
import uk.nhs.hee.tis.revalidation.repository.SnapshotRepository;

@ChangeLog(order = "005")
public class RemoveOrphanedDraftFor7523122 {

  private static final String RECOMMENDATION_ID = "62d150a28492f85d3fca8dba";

  @ChangeSet(order="001", id = "removeInvalidRecommendationForDoctor7523122", author = "")
  public void RemoveOrphanedDraftForDoctor7523122(
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
