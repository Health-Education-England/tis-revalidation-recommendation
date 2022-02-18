package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import java.util.Optional;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.Snapshot;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;
import uk.nhs.hee.tis.revalidation.repository.SnapshotRepository;

@ChangeLog(order = "002")
public class RemoveInvalidRecommendationsFor6167961ChangeLog {

  private static final String RECOMMENDATION_ID = "618d12661f777d65dc5ee439";
  private static final String SNAPSHOT_ID = "618d12e61f777d65dc5f5750";

  @ChangeSet(order="001", id = "removeInvalidRecommendationForDoctor6167961", author = "")
  public void RemoveInvalidRecommendationForDoctor6167961(
      RecommendationRepository recommendationRepository
  ) {
    Optional<Recommendation> recommendation = recommendationRepository.findById(
        RECOMMENDATION_ID
    );
    if(recommendation.isPresent()) {
      recommendationRepository.delete(recommendation.get());
    }
  }

  @ChangeSet(order="002", id = "removeInvalidSnapshotForDoctor6167961", author = "")
  public void RemoveInvalidSnapshotForDoctor6167961(SnapshotRepository snapshotRepository) {
    Optional<Snapshot> snapshot = snapshotRepository.findById(SNAPSHOT_ID);
    if(snapshot.isPresent()) {
      snapshotRepository.delete(snapshot.get());
    }
  }
}
