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
class RemoveOrphanedDraftFor7328921ChangeLogTest {

  @InjectMocks
  RemoveOrphanedDraftFor7328921ChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  private static final String RECOMMENDATION_ID = "651ef176bf65a715e9bc469a";

  @BeforeEach
  void setup() {
    changeLog = new RemoveOrphanedDraftFor7328921ChangeLog();
  }

  @Test
  void shouldRemoveRecommendationWithGivenIdIfPresent() {
    var testRecommendation = buildTestRecommendation();

    when(recommendationRepository.findById(RECOMMENDATION_ID))
        .thenReturn(Optional.of(testRecommendation));

    changeLog.removeOrphanedDraftForDoctor7328921(recommendationRepository);

    verify(recommendationRepository).delete(testRecommendation);
  }

  @Test
  void shouldNotRemoveRecommendationWithGivenIdIfNotPresent() {
    when(recommendationRepository.findById(RECOMMENDATION_ID))
        .thenReturn(Optional.empty());

    changeLog.removeOrphanedDraftForDoctor7328921(recommendationRepository);

    verify(recommendationRepository, never()).delete(any());
  }

  private Recommendation buildTestRecommendation() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .build();
  }
}
