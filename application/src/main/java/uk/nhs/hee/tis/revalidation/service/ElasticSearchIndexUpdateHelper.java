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

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;


@Slf4j
@Component
public class ElasticSearchIndexUpdateHelper {

  private static final String VISITOR = "Visitor";

  @Autowired
  RecommendationElasticSearchService recommendationElasticSearchService;

  @Autowired
  private ElasticsearchOperations elasticSearchOperations;


  /**
   * Route changes to correct elasticsearch index.
   *
   * @param masterDoctorViewDto details of changes that need to be propagated to elasticsearch
   */
  public void updateElasticSearchIndex(final MasterDoctorViewDto masterDoctorViewDto) {

    recommendationElasticSearchService.addRecommendationViews(
        buildRecommendationView(masterDoctorViewDto));
  }

  /**
   * Create entry for Recommendation elasticsearch index.
   *
   * @param masterDoctorViewDto details of changes that need to be propagated to elasticsearch
   */
  public RecommendationView buildRecommendationView(final MasterDoctorViewDto masterDoctorViewDto) {
    return RecommendationView.builder()
        .tcsPersonId(masterDoctorViewDto.getTcsPersonId())
        .gmcReferenceNumber(masterDoctorViewDto.getGmcReferenceNumber())
        .doctorFirstName(masterDoctorViewDto.getDoctorFirstName())
        .doctorLastName(masterDoctorViewDto.getDoctorLastName())
        .designatedBody(masterDoctorViewDto.getDesignatedBody())
        .submissionDate(masterDoctorViewDto.getSubmissionDate())
        .outcome(masterDoctorViewDto.getOutcome())
        .tisStatus(masterDoctorViewDto.getTisStatus())
        .programmeName(masterDoctorViewDto.getProgrammeName())
        .membershipType(masterDoctorViewDto.getMembershipType())
        .curriculumEndDate(masterDoctorViewDto.getCurriculumEndDate())
        .admin(masterDoctorViewDto.getAdmin())
        .lastUpdatedDate(masterDoctorViewDto.getLastUpdatedDate())
        .underNotice(masterDoctorViewDto.getUnderNotice())
        .build();
  }

  public void clearRecommendationIndex(String esIndex) throws IOException {
    deleteRecommendationIndex(esIndex);
    createRecommendationIndex(esIndex);
  }

  private void deleteRecommendationIndex(String esIndex) throws IOException {
    log.info("Deleting elastic search index: {}", esIndex);
//    try {
//      elasticSearchOperations.deleteIndex(esIndex);
//
//    } catch (IndexNotFoundException e) {
//      log.info("Could not delete an index that does not exist: {}", esIndex);
//    }
    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(
            new HttpHost("localhost", 9200, "http"),
            new HttpHost("localhost", 9201, "http")));

    DeleteRequest request = new DeleteRequest(esIndex, "does_not_exist");
    DeleteResponse deleteResponse = client.delete(
        request, RequestOptions.DEFAULT);
    if (deleteResponse.getResult() == DocWriteResponse.Result.NOT_FOUND) {
      log.info("Could not delete an index that does not exist: {}", esIndex);
    }
    client.close();
  }

  private void createRecommendationIndex(String esIndex) throws IOException {
    log.info("Creating elastic search index: {}", esIndex);
//    try {
//      elasticSearchOperations.createIndex(esIndex);
//      elasticSearchOperations.putMapping(RecommendationView.class);
//    } catch (IndexNotFoundException e) {
//      log.info("Could not create index: {}", esIndex);
//    }
    RestHighLevelClient client = new RestHighLevelClient(
        RestClient.builder(
            new HttpHost("localhost", 9200, "http"),
            new HttpHost("localhost", 9201, "http")));

    CreateIndexRequest request = new CreateIndexRequest(esIndex);
    CreateIndexResponse createIndexResponse = client.indices().create(
        request, RequestOptions.DEFAULT);
  }
}
