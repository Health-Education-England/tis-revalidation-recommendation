package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

import java.util.Optional;

@ChangeLog(order = "006")
public class RemoveDuplicateDraftsFor7496627ChangeLog {

  private static final String RECOMMENDATION_ID_1 = "63ea10872ad97b4aa7ad1145";
  private static final String RECOMMENDATION_ID_2 = "63ea10872ad97b4aa7ad1146";

  @ChangeSet(order="001", id = "removeDuplicateDraftsForDoctor7496627", author = "")
  public void removeDuplicateDraftsFor7496627(
      RecommendationRepository recommendationRepository
  ) {
    Optional<Recommendation> recommendation1 = recommendationRepository.findById(
            RECOMMENDATION_ID_1
    );
    if(recommendation1.isPresent()) {
      recommendationRepository.delete(recommendation1.get());
    }
    Optional<Recommendation> recommendation2 = recommendationRepository.findById(
            RECOMMENDATION_ID_2
    );
    if(recommendation2.isPresent()) {
      recommendationRepository.delete(recommendation2.get());
    }
  }
}
