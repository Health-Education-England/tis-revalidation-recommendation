package uk.nhs.hee.tis.revalidation.changelog;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.SUBMITTED_TO_GMC;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveAllInvalidRecommendationsChangeLogTest {

  @InjectMocks
  RemoveAllInvalidRecommendationsChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  private static final String RECOMMENDATION_ID = "123";
  private static final String GMC_REVALIDATION_ID = "456";

  private Recommendation validRecommendation = buildValidTestRecommendation();
  private Recommendation invalidRecommendation = buildInvalidTestRecommendation();

  @Test
  void shouldNotDeleteRecommendationIfSubmittedWithGmcRevalidationId() {
    when(recommendationRepository.findAllByRecommendationStatus(SUBMITTED_TO_GMC)).thenReturn(
        List.of(validRecommendation)
    );
    changeLog.removeAllInvalidRecommendations(recommendationRepository);
    verify(recommendationRepository, never()).delete(validRecommendation);
  }

  @Test
  void shouldDeleteRecommendationIfSubmittedWithNoGmcRevalidationId() {
    when(recommendationRepository.findAllByRecommendationStatus(SUBMITTED_TO_GMC)).thenReturn(
        List.of(invalidRecommendation)
    );
    changeLog.removeAllInvalidRecommendations(recommendationRepository);
    verify(recommendationRepository).delete(invalidRecommendation);
  }

  private Recommendation buildValidTestRecommendation() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .gmcRevalidationId(GMC_REVALIDATION_ID)
        .build();
  }

  private Recommendation buildInvalidTestRecommendation() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .build();
  }

}
