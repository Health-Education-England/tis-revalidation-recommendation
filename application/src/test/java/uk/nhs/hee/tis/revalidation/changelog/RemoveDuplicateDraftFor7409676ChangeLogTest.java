package uk.nhs.hee.tis.revalidation.changelog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

@ExtendWith(MockitoExtension.class)
class RemoveDuplicateDraftFor7409676ChangeLogTest {

  @InjectMocks
  RemoveDuplicateDraftFor7409676ChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  private static final String RECOMMENDATION_ID = "65c2415de6d8ff00033dd29b";

  @BeforeEach
  void setup() {
    changeLog = new RemoveDuplicateDraftFor7409676ChangeLog();
  }

  @Test
  void shouldRemoveRecommendationWithGivenIdIfPresent() {
    var testRecommendation = buildTestRecommendation();

    when(recommendationRepository.findById(RECOMMENDATION_ID))
        .thenReturn(Optional.of(testRecommendation));

    changeLog.removeDuplicateDraftFor7409676(recommendationRepository);

    verify(recommendationRepository).delete(testRecommendation);
  }

  @Test
  void shouldNotRemoveRecommendationWithGivenIdIfNotPresent() {
    when(recommendationRepository.findById(RECOMMENDATION_ID))
        .thenReturn(Optional.empty());

    changeLog.removeDuplicateDraftFor7409676(recommendationRepository);

    verify(recommendationRepository, never()).delete(any());
  }

  private Recommendation buildTestRecommendation() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .build();
  }
}
