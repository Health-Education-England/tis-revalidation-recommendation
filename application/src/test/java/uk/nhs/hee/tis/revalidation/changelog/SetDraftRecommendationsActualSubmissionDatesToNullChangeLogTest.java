package uk.nhs.hee.tis.revalidation.changelog;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.repository.RecommendationRepository;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.READY_TO_REVIEW;

@ExtendWith(MockitoExtension.class)
class SetDraftRecommendationsActualSubmissionDatesToNullChangeLogTest {

  @InjectMocks
  SetDraftRecommendationsActualSubmissionDatesToNullChangeLog changeLog;

  @Mock
  RecommendationRepository recommendationRepository;

  private static final String RECOMMENDATION_ID = "123";

  private Recommendation draftWithDate = buildDraftWithActualSubmissionDate();
  private Recommendation draftWithoutDate = buildDraftWithoutActualSubmissionDate();

  @Test
  void shouldNotUpdateDraftIfActualSubmissionDateNull() {
    when(recommendationRepository.findAllByRecommendationStatus(READY_TO_REVIEW)).thenReturn(
        List.of(draftWithoutDate)
    );
    changeLog.setActualSubmissionDatesToNull(recommendationRepository);
    verify(recommendationRepository, never()).save(draftWithoutDate);
  }

  @Test
  void shouldUpdateDraftIfActualSubmissionDateNotNull() {
    when(recommendationRepository.findAllByRecommendationStatus(READY_TO_REVIEW)).thenReturn(
        List.of(draftWithDate)
    );
    changeLog.setActualSubmissionDatesToNull(recommendationRepository);
    verify(recommendationRepository).save(draftWithoutDate);
  }

  private Recommendation buildDraftWithActualSubmissionDate() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .actualSubmissionDate(LocalDate.now())
        .build();
  }

  private Recommendation buildDraftWithoutActualSubmissionDate() {
    return Recommendation.builder()
        .id(RECOMMENDATION_ID)
        .build();
  }
}
