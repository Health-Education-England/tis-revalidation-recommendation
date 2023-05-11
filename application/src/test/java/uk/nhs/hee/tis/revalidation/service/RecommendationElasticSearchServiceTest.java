/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package uk.nhs.hee.tis.revalidation.service;

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.repository.RecommendationElasticSearchRepository;

@ExtendWith(MockitoExtension.class)
class RecommendationElasticSearchServiceTest {

  private final Faker faker = new Faker();
  @InjectMocks
  RecommendationElasticSearchService recommendationElasticSearchService;
  @Mock
  RecommendationElasticSearchRepository recommendationElasticSearchRepository;
  private String gmcRef1;
  private String firstName1;
  private String lastName1;
  private LocalDate submissionDate1;
  private String designatedBody1;
  private String programmeName1;
  private String admin;
  private String underNotice;
  private RecommendationView recommendationView = new RecommendationView();
  private List<RecommendationView> recommendationViews = new ArrayList<>();

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {

    gmcRef1 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    submissionDate1 = now();
    designatedBody1 = faker.lorem().characters(8);
    programmeName1 = faker.lorem().characters(20);
    admin = faker.lorem().characters(20);
    underNotice = faker.lorem().characters(20);

    recommendationView = RecommendationView.builder().id("1a2a").tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef1).doctorFirstName(firstName1).doctorLastName(lastName1)
        .submissionDate(submissionDate1).programmeName(programmeName1)
        .designatedBody(designatedBody1).admin(admin).underNotice(underNotice).build();
    recommendationViews.add(recommendationView);
  }

  @Test
  void shouldAddRecommendationViews() {
    recommendationElasticSearchService.addRecommendationViews(recommendationView);
    verify(recommendationElasticSearchRepository, times(1)).save(recommendationView);
  }

  @Test
  void shouldThrowExceptionWhenSavingNull() {
    doThrow(new NullPointerException()).when(recommendationElasticSearchRepository).save(null);
    assertThrows(Exception.class, () -> {
      recommendationElasticSearchService.addRecommendationViews(null);
    });
  }

  @Test
  void shouldSaveRecommendationViewsWhenTheRecordIsAlreadyThereInTheESRepository() {
    when(recommendationElasticSearchRepository.findByGmcReferenceNumber(gmcRef1)).thenReturn(
        recommendationViews);
    recommendationElasticSearchService.saveRecommendationViews(recommendationView);
    verify(recommendationElasticSearchRepository, times(1)).save(recommendationView);
  }

  @Test
  void shouldSaveRecommendationViewsWhenTheRecordIsNotThereInTheESRepository() {
    recommendationElasticSearchService.saveRecommendationViews(recommendationView);
    verify(recommendationElasticSearchRepository, times(1)).save(recommendationView);
  }

  @Test
  void shouldThrowExceptionIfGmcReferenceNumberNull() {
    recommendationView = RecommendationView.builder().id("1a2a").tcsPersonId((long) 111)
        .gmcReferenceNumber(null).doctorFirstName(firstName1).doctorLastName(lastName1)
        .submissionDate(submissionDate1).programmeName(programmeName1)
        .designatedBody(designatedBody1).admin(admin).underNotice(underNotice).build();

    assertThrows(Exception.class, () -> {
      recommendationElasticSearchService.saveRecommendationViews(recommendationView);
    });
  }

  @Test
  void shouldFormatDesignatedBodyCodesForElasticsearchQuery() {
    final String dbc1 = "1-AIIDHJ";
    final String dbc2 = "AIIDMQ";
    final String dbcformatted = "aiidhj aiidmq";
    List<String> dbcs = List.of(dbc1, dbc2);

    final var result = recommendationElasticSearchService.formatDesignatedBodyCodesForElasticsearchQuery(
        dbcs);

    assertThat(result, is(dbcformatted));
  }

  @Test
  void shouldReturnDistinctAutocompleteValuesForProgrammeName() {
    final var fieldNameParam = "programmeName";
    final var dbcsParam = List.of("1-AIIDWQ");
    final var escapedDbcsParam = "aiidwq";
    final var inputParam = "General prac";
    when(recommendationElasticSearchRepository.findByFieldNameParameter(fieldNameParam, inputParam,
        escapedDbcsParam)).thenReturn(generateListOfRecommendationViews());
    final var results = recommendationElasticSearchService.getAutocompleteResults(fieldNameParam,
        inputParam, dbcsParam);

    assertThat(results, is(List.of("General Practice 1", "General Practice 2")));
    assertThat(results.size(), is(2));
  }

  @Test
  void shouldNotReturnUnsupportedAutocompleteFields() {
    final var fieldNameParam = "unsupportedField";
    final var dbcsParam = List.of("1-AIIDWQ");
    final var escapedDbcsParam = "aiidwq";
    final var inputParam = "General prac";
    when(recommendationElasticSearchRepository.findByFieldNameParameter(fieldNameParam, inputParam,
        escapedDbcsParam)).thenReturn(generateListOfRecommendationViews());
    final var results = recommendationElasticSearchService.getAutocompleteResults(fieldNameParam,
        inputParam, dbcsParam);

    assertThat(results.size(), is(0));
  }

  private List<RecommendationView> generateListOfRecommendationViews() {
    return List.of(
        recommendationView = RecommendationView.builder().id("1a2a").tcsPersonId((long) 111)
            .gmcReferenceNumber(null).doctorFirstName(firstName1).doctorLastName(lastName1)
            .submissionDate(submissionDate1).programmeName("General Practice 1")
            .designatedBody(designatedBody1).admin(admin).underNotice(underNotice).build(),
        RecommendationView.builder().id("1a2a").tcsPersonId((long) 111).gmcReferenceNumber(null)
            .doctorFirstName(firstName1).doctorLastName(lastName1).submissionDate(submissionDate1)
            .programmeName("General Practice 1").designatedBody(designatedBody1).admin(admin)
            .underNotice(underNotice).build(),
        RecommendationView.builder().id("1a2a").tcsPersonId((long) 111).gmcReferenceNumber(null)
            .doctorFirstName(firstName1).doctorLastName(lastName1).submissionDate(submissionDate1)
            .programmeName("General Practice 2").designatedBody(designatedBody1).admin(admin)
            .underNotice(underNotice).build());
  }
}
