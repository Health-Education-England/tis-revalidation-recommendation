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

package uk.nhs.hee.tis.revalidation.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;

@Repository
public interface RecommendationElasticSearchRepository
    extends ElasticsearchRepository<RecommendationView, String> {

  @Query(
      "{\"bool\":{\"filter\":[{\"match\":{\"underNotice\":\"YES\"}},{\"match\":"
          + "{\"designatedBody\":\"?1\"}},{\"match\":{\"existsInGmc\":\"true\"}},{\"bool\":"
          + "{\"should\":[{\"wildcard\":{\"doctorFirstName\":{\"value\":\"?0*\"}}},{\"wildcard\":"
          + "{\"doctorLastName\":{\"value\":\"?0*\"}}},{\"wildcard\":{\"gmcReferenceNumber\":"
          + "{\"value\":\"?0*\"}}}]}},{\"match_phrase\":{\"programmeName\":{\"query\":\"?2\","
          + "\"zero_terms_query\":\"all\"}}}]}}"
  )
  Page<RecommendationView> findByUnderNotice(final String searchQuery,
      final String dbcs, String programmeName, final Pageable pageable);

  @Query(
      "{\"bool\":{\"must_not\":{\"match\":{\"gmcReferenceNumber\":\"?2\"}},\"filter\":"
          + "[{\"match\":{\"designatedBody\":\"?1\"}},{\"match\":{\"existsInGmc\":\"true\"}},"
          + "{\"bool\":{\"should\":[{\"wildcard\":{\"doctorFirstName\":{\"value\":\"?0*\"}}},"
          + "{\"wildcard\":{\"doctorLastName\":{\"value\":\"?0*\"}}},{\"wildcard\":"
          + "{\"gmcReferenceNumber\":{\"value\":\"?0*\"}}}]}},{\"match_phrase\":{\"programmeName\":"
          + "{\"query\":\"?3\",\"zero_terms_query\":\"all\"}}}]}}"
  )
  Page<RecommendationView> findAll(final String searchQuery,
      final String dbcs, List<String> hiddenGmcIds, String programmeName, final Pageable pageable);

  List<RecommendationView> findByGmcReferenceNumber(String gmcReferenceNumber);

}
