/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHitsImpl;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import uk.nhs.hee.tis.revalidation.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.mapper.ElasticSearchIndexMapper;

@ExtendWith(MockitoExtension.class)
public class MasterElasticSearchServiceTest {

  @InjectMocks
  MasterElasticSearchService masterElasticSearchService;

  @Mock
  ElasticSearchIndexMapper elasticSearchIndexMapper;

  @Mock
  ElasticsearchRestTemplate elasticsearchTemplate;

  @Mock
  NativeSearchQuery nativeSearchQuery;

  MasterDoctorView masterDoctorView1;
  SearchScrollHits<MasterDoctorView> searchScrollHits;
  SearchScrollHits<MasterDoctorView> searchScrollContinueHits;


  private String id1 = "111";
  private String gmcRef1 = "1111111";
  private String indexName = "masterdoctorindex";
  private long totalHits = 1;
  private TotalHitsRelation totalHitsRelation = TotalHitsRelation.EQUAL_TO;
  private float maxScore = 1;

  @BeforeEach
  void setup() throws IOException {
    buildMasterDoctorViews();
    buildSearchScrollHits();
  }

  @Test
  void shouldReturnMasterDoctorViews() {

    doReturn(searchScrollHits).
    when(elasticsearchTemplate).searchScrollStart(
        30000,
        nativeSearchQuery,
        MasterDoctorView.class,
        IndexCoordinates.of(indexName)
    );

    doReturn(searchScrollContinueHits).
        when(elasticsearchTemplate).searchScrollContinue(
        "1",
        30000,
        MasterDoctorView.class,
        IndexCoordinates.of(indexName)
    );

    masterElasticSearchService.findAllScroll();

    verify(elasticsearchTemplate).searchScrollClear(any());

    verify(elasticSearchIndexMapper).masterToDtos(List.of(masterDoctorView1));
  }

  private void buildMasterDoctorViews() {
    masterDoctorView1 = MasterDoctorView.builder()
        .id(id1)
        .gmcReferenceNumber(gmcRef1)
        .build();
  }

  private void buildSearchScrollHits() throws IOException {
    SearchHit<MasterDoctorView> searchHit = new SearchHit<MasterDoctorView>(
        indexName,
        id1,
        null,
        maxScore,
        null,
        null,
        null,
        null,
        null,
        null,
        masterDoctorView1
        );

    final var searchHitList = List.of(searchHit);

    searchScrollHits = new SearchHitsImpl<MasterDoctorView>(
        totalHits,
        totalHitsRelation,
        maxScore,
        "1",
        searchHitList,
        null
    );

    List<SearchHit<MasterDoctorView>> emptyHits = List.of();

    searchScrollContinueHits = new SearchHitsImpl<MasterDoctorView>(
        0,
        TotalHitsRelation.EQUAL_TO,
        0,
        "1",
        emptyHits,
        null
    );
  }

}
