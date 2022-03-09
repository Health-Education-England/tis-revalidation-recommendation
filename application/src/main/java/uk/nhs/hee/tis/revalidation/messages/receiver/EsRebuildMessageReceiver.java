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

package uk.nhs.hee.tis.revalidation.messages.receiver;

import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.service.MasterElasticSearchService;

@Slf4j
@Component
public class EsRebuildMessageReceiver implements MessageReceiver<String> {

  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  private MasterElasticSearchService masterElasticSearchService;

  /**
   * Class to handle connection update messages
   *
   * @param elasticSearchIndexUpdateHelper
   * @param masterElasticSearchService
   */
  public EsRebuildMessageReceiver(
      ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper,
      MasterElasticSearchService masterElasticSearchService
  ) {
    this.elasticSearchIndexUpdateHelper = elasticSearchIndexUpdateHelper;
    this.masterElasticSearchService = masterElasticSearchService;
  }

  /**
   * Handles Elastic Search indexes rebuild messages
   *
   * @param message a String for ES rebuild job "getMaster" signal
   */
  @Override
  public void handleMessage(final String message) throws IOException {
    if (message != null && message.equals("getMaster")) {

      //Delete and create elastic search index
      elasticSearchIndexUpdateHelper.clearIndex("recommendationindex");

      final List<MasterDoctorViewDto> masterList = masterElasticSearchService.findAllScroll();
      log.info("Found {} records from ES Master index. ", masterList.size());

      for (MasterDoctorViewDto masterDoctorViewDto : masterList) {
        elasticSearchIndexUpdateHelper.updateElasticSearchIndex(masterDoctorViewDto);
      }
      log.info("ES indexes update completed.");
    }
  }
}
