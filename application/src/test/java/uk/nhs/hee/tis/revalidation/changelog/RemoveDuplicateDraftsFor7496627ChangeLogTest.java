package uk.nhs.hee.tis.revalidation.changelog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RemoveDuplicateDraftsFor7496627ChangeLogTest {

  @InjectMocks
  RemoveDuplicateDraftsFor7496627ChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  private static final String RECOMMENDATION_ID_1 = "63ea10872ad97b4aa7ad1145";
  private static final String RECOMMENDATION_ID_2 = "63ea10872ad97b4aa7ad1146";

  @BeforeEach
  void setup() {
    changeLog = new RemoveDuplicateDraftsFor7496627ChangeLog();
  }

  @Test
  void shouldRemoveRecommendationWithGivenIdIfPresent() {
    var testRecommendation1 = buildTestRecommendation(RECOMMENDATION_ID_1);
    var testRecommendation2 = buildTestRecommendation(RECOMMENDATION_ID_2);

    when(recommendationRepository.findById(RECOMMENDATION_ID_1))
        .thenReturn(Optional.of(testRecommendation1));
    when(recommendationRepository.findById(RECOMMENDATION_ID_2))
            .thenReturn(Optional.of(testRecommendation2));

    changeLog.removeDuplicateDraftsFor7496627(recommendationRepository);

    verify(recommendationRepository).delete(testRecommendation1);
    verify(recommendationRepository).delete(testRecommendation2);
  }

  @Test
  void shouldNotRemoveRecommendationWithGivenIdIfNotPresent() {
    when(recommendationRepository.findById(RECOMMENDATION_ID_1))
        .thenReturn(Optional.empty());
    when(recommendationRepository.findById(RECOMMENDATION_ID_2))
            .thenReturn(Optional.empty());

    changeLog.removeDuplicateDraftsFor7496627(recommendationRepository);

    verify(recommendationRepository, never()).delete(any());
  }

  private Recommendation buildTestRecommendation(String id) {
    return Recommendation.builder()
        .id(id)
        .build();
  }
}
