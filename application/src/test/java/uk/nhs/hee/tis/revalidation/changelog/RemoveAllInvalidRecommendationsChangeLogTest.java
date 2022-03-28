package uk.nhs.hee.tis.revalidation.changelog;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.Snapshot;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;
import uk.nhs.hee.tis.revalidation.repository.SnapshotRepository;
import uk.nhs.hee.tis.revalidation.entity.SnapshotRevalidation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.SUBMITTED_TO_GMC;

@ExtendWith(MockitoExtension.class)
class RemoveAllInvalidRecommendationsChangeLogTest {

  @InjectMocks
  RemoveAllInvalidRecommendationsChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  @Mock
  SnapshotRepository snapshotRepository;

  private static final String RECOMMENDATION_ID = "123";
  private static final String GMC_REVALIDATION_ID = "456";

  private Recommendation validRecommendation = buildValidTestRecommendation();
  private Recommendation invalidRecommendation = buildInvalidTestRecommendation();
  private Snapshot validSnapshot = buildValidTestSnapshot();
  private Snapshot invalidSnapshot = buildInvalidTestSnapshot();

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

  @Test
  void shouldNotDeleteSnapshotIfSubmittedWithGmcRecommendationId() {
    when(snapshotRepository.findAll()).thenReturn(
        List.of(validSnapshot)
    );
    changeLog.removeAllInvalidSnapshots(snapshotRepository);
    verify(snapshotRepository, never()).delete(validSnapshot);
  }

  @Test
  void shouldDeleteSnapshotIfSubmittedWithNoGmcRecommendationId() {
    when(snapshotRepository.findAll()).thenReturn(
        List.of(invalidSnapshot)
    );
    changeLog.removeAllInvalidSnapshots(snapshotRepository);
    verify(snapshotRepository).delete(invalidSnapshot);
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

  private Snapshot buildValidTestSnapshot() {
    return Snapshot.builder()
        .id(RECOMMENDATION_ID)
        .revalidation(
            SnapshotRevalidation.builder().gmcRecommendationId(GMC_REVALIDATION_ID).build()
        )
        .build();
  }

  private Snapshot buildInvalidTestSnapshot() {
    return Snapshot.builder()
        .id(RECOMMENDATION_ID)
        .revalidation(
            SnapshotRevalidation.builder().build()
        )
        .build();
  }

}
