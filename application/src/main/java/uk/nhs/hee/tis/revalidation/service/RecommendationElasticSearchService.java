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

import java.util.ArrayList;
import org.elasticsearch.common.util.iterable.Iterables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.repository.RecommendationElasticSearchRepository;

@Service
public class RecommendationElasticSearchService {

  private static final Logger LOG = LoggerFactory
      .getLogger(RecommendationElasticSearchService.class);

  @Autowired
  RecommendationElasticSearchRepository recommendationElasticSearchRepository;

  /**
   * add Recommendation data to elasticsearch Recommendation index.
   *
   * @param dataToSave Recommendation data to go in elasticsearch
   */
  public void addRecommendationViews(RecommendationView dataToSave) {
    try {
      recommendationElasticSearchRepository.save(dataToSave);
    } catch (Exception ex) {
      LOG.info("Exception in `addRecommendationViews` (GmcId: {}; PersonId: {}): {}",
          dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId(), ex);
    }
  }

  /**
   * add new Recommendation trainee to elasticsearch index.
   *
   * @param dataToSave Recommendation trainee to go in elasticsearch
   */
  public void saveRecommendationViews(RecommendationView dataToSave) {
    Iterable<RecommendationView> existingRecords = findRecommendationViewsByGmcNumberPersonId(
        dataToSave);

    // if doctor already exists in ES index, then update the existing record
    if (Iterables.size(existingRecords) > 0) {
      updateRecommendationViews(existingRecords, dataToSave);
    }
    // otherwise, add a new record
    else {
      addRecommendationViews(dataToSave);
    }
  }

  /**
   * update existing Recommendation to elasticsearch index.
   *
   * @param existingRecords existing Recommendation to be updated in elasticsearch
   * @param dataToSave      new Recommendation details to be saved in elasticsearch
   */
  private void updateRecommendationViews(Iterable<RecommendationView> existingRecords,
      RecommendationView dataToSave) {
    existingRecords.forEach(connectedView -> {
      dataToSave.setId(connectedView.getId());
      try {
        recommendationElasticSearchRepository.save(dataToSave);
      } catch (Exception ex) {
        LOG.info("Exception in `updateConnectedViews` (GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId(), ex);
      }
    });
  }

  /**
   * find iterable of RecommendationView from elasticsearch index.
   *
   * @param dataToSave RecommendationView to go in elasticsearch
   */
  private Iterable<RecommendationView> findRecommendationViewsByGmcNumberPersonId(
      RecommendationView dataToSave) {
    Iterable<RecommendationView> result = new ArrayList<>();

    if (dataToSave.getGmcReferenceNumber() != null && dataToSave.getTcsPersonId() != null) {
      try {
        result = recommendationElasticSearchRepository.findByGmcReferenceNumberAndTcsPersonId(
            dataToSave.getGmcReferenceNumber(),
            dataToSave.getTcsPersonId());
      } catch (Exception ex) {
        LOG.info("Exception in `findByGmcReferenceNumberAndTcsPersonId`"
                + "(GmcId: {}; PersonId: {}): {}",
            dataToSave.getGmcReferenceNumber(), dataToSave.getTcsPersonId(), ex);
      }
    } else if (dataToSave.getGmcReferenceNumber() != null
        && dataToSave.getTcsPersonId() == null) {
      try {
        result = recommendationElasticSearchRepository.findByGmcReferenceNumber(
            dataToSave.getGmcReferenceNumber());
      } catch (Exception ex) {
        LOG.info("Exception in `findByGmcReferenceNumber` (GmcId: {}): {}",
            dataToSave.getGmcReferenceNumber(), ex);
      }
    } else if (dataToSave.getGmcReferenceNumber() == null
        && dataToSave.getTcsPersonId() != null) {
      try {
        result = recommendationElasticSearchRepository.findByTcsPersonId(
            dataToSave.getTcsPersonId());
      } catch (Exception ex) {
        LOG.info("Exception in `findByTcsPersonId` (PersonId: {}): {}",
            dataToSave.getTcsPersonId(), ex);
      }
    }
    return result;
  }
}
