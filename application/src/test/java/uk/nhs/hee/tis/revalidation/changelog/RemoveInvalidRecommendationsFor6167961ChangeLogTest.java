package uk.nhs.hee.tis.revalidation.changelog;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.Snapshot;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;
import uk.nhs.hee.tis.revalidation.repository.SnapshotRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RemoveInvalidRecommendationsFor6167961ChangeLogTest {

  @InjectMocks
  RemoveInvalidRecommendationsFor6167961ChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  @Mock
  SnapshotRepository snapshotRepository;

  private static final String RECOMMENDATION_ID = "618d12661f777d65dc5ee439";
  private static final String SNAPSHOT_ID = "618d12e61f777d65dc5f5750";

  @BeforeEach
  void setup() {
    changeLog = new RemoveInvalidRecommendationsFor6167961ChangeLog();
  }

  @Test
  void shouldRemoveRecommendationWithGivenIdIfPresent() {
    var testRecommendation = buildTestRecommendation();

    when(recommendationRepository.findById(RECOMMENDATION_ID))
        .thenReturn(Optional.of(testRecommendation));

    changeLog.RemoveInvalidRecommendationForDoctor6167961(recommendationRepository);

    verify(recommendationRepository).delete(testRecommendation);
  }

  @Test
  void shouldNotRemoveRecommendationWithGivenIdIfNotPresent() {
    when(recommendationRepository.findById(RECOMMENDATION_ID))
        .thenReturn(Optional.empty());

    changeLog.RemoveInvalidRecommendationForDoctor6167961(recommendationRepository);

    verify(recommendationRepository, never()).delete(any());
  }

  @Test
  void shouldRemoveSnapshotWithGivenIdIfPresent() {
    var testSnapshot = buildTestSnapshot();

    when(snapshotRepository.findById(SNAPSHOT_ID)).thenReturn(Optional.of(testSnapshot));

    changeLog.RemoveInvalidSnapshotForDoctor6167961(snapshotRepository);

    verify(snapshotRepository).delete(testSnapshot);
  }

  @Test
  void shouldNotRemoveSnapshotWithGivenIdIfNotPresent() {
    when(snapshotRepository.findById(SNAPSHOT_ID)).thenReturn(Optional.empty());

    changeLog.RemoveInvalidSnapshotForDoctor6167961(snapshotRepository);

    verify(snapshotRepository, never()).delete(any());
  }

  private Recommendation buildTestRecommendation() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .build();
  }

  private Snapshot buildTestSnapshot() {
    return Snapshot.builder()
        .id(SNAPSHOT_ID)
        .build();
  }
}
