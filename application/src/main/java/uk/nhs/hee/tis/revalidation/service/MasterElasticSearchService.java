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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.mapper.ElasticSearchIndexMapper;

@Service
@Slf4j
public class MasterElasticSearchService {

  private static final int SCROLL_TIMEOUT_MS = 30000;
  private ElasticSearchIndexMapper elasticSearchIndexMapper;
  private ElasticsearchRestTemplate elasticsearchTemplate;

  /**
   * constructor.
   */
  public MasterElasticSearchService(
      ElasticSearchIndexMapper elasticSearchIndexMapper,
      ElasticsearchRestTemplate elasticsearchTemplate) {
    this.elasticSearchIndexMapper = elasticSearchIndexMapper;
    this.elasticsearchTemplate = elasticsearchTemplate;
  }

  /**
   * find all trainee from ES Master Index. (by `scroll` to avoid ES index max_result_window excess
   * error)
   */
  public List<MasterDoctorViewDto> findAllScroll() {
    var mapper = new ObjectMapper();
    List<MasterDoctorView> masterViews = new ArrayList<>();
    List<String> scrollIds = new ArrayList<>();

    var index = IndexCoordinates.of("masterdoctorindex");

    // initial search
    var searchQuery = new NativeSearchQueryBuilder().build();
    SearchScrollHits<MasterDoctorView> scroll = elasticsearchTemplate
        .searchScrollStart(
            SCROLL_TIMEOUT_MS,
            searchQuery,
            MasterDoctorView.class,
            index
        );

    // while it is not the end of data
    while (scroll.hasSearchHits()) {
      // convert and store data to list
      for (SearchHit hit : scroll.getSearchHits()) {
        var masterDoctorView = mapper.convertValue(hit.getContent(), MasterDoctorView.class);
        masterViews.add(masterDoctorView);
      }

      // do search scroll with last scrollId
      String scrollId = scroll.getScrollId();
      scroll = elasticsearchTemplate
          .searchScrollContinue(
              scrollId,
              SCROLL_TIMEOUT_MS,
              MasterDoctorView.class,
              index
          );
      scrollIds.add(scrollId);
    }
    elasticsearchTemplate.searchScrollClear(scrollIds);

    return elasticSearchIndexMapper.masterToDtos(masterViews);
  }
}
