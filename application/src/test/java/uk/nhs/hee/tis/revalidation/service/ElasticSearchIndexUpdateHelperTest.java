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
import static org.mockito.Mockito.verify;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;

@ExtendWith(MockitoExtension.class)
class ElasticSearchIndexUpdateHelperTest {

  @InjectMocks
  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;

  @Mock
  private RecommendationElasticSearchService recommendationElasticSearchService;

  @Mock
  private RestHighLevelClient highLevelClient;

  private MasterDoctorViewDto masterDoctorViewDto;
  private RecommendationView recommendationView;
  private Faker faker = new Faker();
  private String gmcRef;
  private String firstName;
  private String lastName;
  private LocalDate curriculumEndDate;
  private String designatedBody;
  private String programmeName;
  private String gmcStatus;
  private String admin;
  private LocalDate submissionDate;
  private String tisStatus;
  private String membershipType;
  private LocalDate lastUpdatedDate;
  private String underNotice;

  /**
   * Set up data for testing.
   */
  @BeforeEach
  public void setup() {
    gmcRef = faker.number().digits(8);
    firstName = faker.name().firstName();
    lastName = faker.name().lastName();
    curriculumEndDate = now();
    designatedBody = faker.lorem().characters(8);
    programmeName = faker.lorem().characters(20);
    gmcStatus = faker.lorem().characters(20);
    admin = faker.lorem().characters(20);
    submissionDate = now();
    tisStatus = faker.lorem().characters(20);
    membershipType = faker.lorem().characters(20);
    lastUpdatedDate = now();
    underNotice = faker.lorem().characters(20);

    masterDoctorViewDto = MasterDoctorViewDto.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .curriculumEndDate(curriculumEndDate)
        .programmeName(programmeName)
        .designatedBody(designatedBody)
        .gmcStatus(gmcStatus)
        .admin(admin)
        .submissionDate(submissionDate)
        .tisStatus(tisStatus)
        .membershipType(membershipType)
        .lastUpdatedDate(lastUpdatedDate)
        .underNotice(underNotice)
        .build();

    recommendationView = RecommendationView.builder()
        .tcsPersonId((long) 111)
        .gmcReferenceNumber(gmcRef)
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .curriculumEndDate(curriculumEndDate)
        .programmeName(programmeName)
        .designatedBody(designatedBody)
        .gmcStatus(gmcStatus)
        .admin(admin)
        .build();
  }
}
