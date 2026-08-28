package uk.nhs.hee.tis.revalidation.changelog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
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
class RemoveRemainingDuplicateDraftsChangeLogTest {

  @InjectMocks
  RemoveRemainingDuplicateDraftsChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

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

  @BeforeEach
  void setup() {
    changeLog = new RemoveRemainingDuplicateDraftsChangeLog();
  }

  @Test
  void shouldRemoveRecommendationWithGivenIdIfPresent() {
    var testRecommendation = buildTestRecommendation();

    when(recommendationRepository.findById(any()))
        .thenReturn(Optional.of(testRecommendation));

    changeLog.removeAllRemainingDuplicateDrafts(recommendationRepository);

    verify(recommendationRepository, times(8)).delete(any());
  }

  @Test
  void shouldNotRemoveRecommendationWithGivenIdIfNotPresent() {
    when(recommendationRepository.findById(any()))
        .thenReturn(Optional.empty());

    changeLog.removeAllRemainingDuplicateDrafts(recommendationRepository);

    verify(recommendationRepository, never()).delete(any());
  }

  private Recommendation buildTestRecommendation() {
    return Recommendation.builder()
        .id("test")
        .build();
  }
}
