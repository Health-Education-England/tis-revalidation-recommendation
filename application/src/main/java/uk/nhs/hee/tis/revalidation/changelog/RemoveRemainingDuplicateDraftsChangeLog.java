package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import java.util.List;
import java.util.Optional;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ChangeLog(order = "9")
public class RemoveRemainingDuplicateDraftsChangeLog {

  public static final List<String> RECOMMENDATION_IDS = List.of(
      "63bbeaf462e248676dc63123", //7084074
      "64d5ecf2f7af912d9a631f64", //7085093
      "65687a07e40b787853e75432", //7411770
      "65ae7a80e6d8ff00033dd140", //7414340
      "65dc71356154806479cf53c6", //7427018
      "65b79baae6d8ff00033dd1b6", //7449295
      "658d872eb090ba0239b81da9", //7458929
      "64bac1ecdf03a63d83248464"  //7562929
  );

  @ChangeSet(order = "001", id = "removeAllRemainingDuplicateDrafts", author = "")
  public void removeAllRemainingDuplicateDrafts(
      RecommendationRepository recommendationRepository
  ) {
    RECOMMENDATION_IDS.stream().forEach(recommendationId -> {
      Optional<Recommendation> recommendation = recommendationRepository.findById(
          recommendationId
      );
      if (recommendation.isPresent()) {
        recommendationRepository.delete(recommendation.get());
      }
    });
  }
}
