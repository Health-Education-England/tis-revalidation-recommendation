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

import static java.time.LocalDate.now;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.dto.MasterDoctorViewDto;
import uk.nhs.hee.tis.revalidation.service.ElasticSearchIndexUpdateHelper;
import uk.nhs.hee.tis.revalidation.service.MasterElasticSearchService;

@ExtendWith(MockitoExtension.class)
class EsRebuildMessageReceiverTest {

  MasterDoctorViewDto masterDoctorViewDto;
  @InjectMocks
  private EsRebuildMessageReceiver esRebuildMessageReceiver;
  @Mock
  private ElasticSearchIndexUpdateHelper elasticSearchIndexUpdateHelper;
  @Mock
  private MasterElasticSearchService masterElasticSearchService;
  private Faker faker = new Faker();
  private String gmcRef;
  private String firstName;
  private String lastName;
  private LocalDate curriculumEndDate;
  private String designatedBody;
  private String programmeName;
  private String gmcStatus;
  private String admin;
  private List<MasterDoctorViewDto> masterDoctorViewDtos = new ArrayList<>();

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
        .build();
    masterDoctorViewDtos.add(masterDoctorViewDto);
  }

  @Test
  void shouldUpdateRecommendationIndexOnReceiveMessageGetMaster() throws IOException {
    when(masterElasticSearchService.findAllScroll()).thenReturn(masterDoctorViewDtos);
    esRebuildMessageReceiver.handleMessage("getMaster");
    verify(elasticSearchIndexUpdateHelper, times(1))
        .updateElasticSearchIndex(masterDoctorViewDtos.get(0));
  }

  @Test
  void shouldNotUpdateRecommendationIndexOnReceiveMessageGetMasterIfNull() throws IOException {
    esRebuildMessageReceiver.handleMessage(null);
    verify(elasticSearchIndexUpdateHelper, never())
        .updateElasticSearchIndex(masterDoctorViewDtos.get(0));
  }

  @Test
  void shouldNotUpdateRecommendationIndexOnReceiveMessageGetMasterIfNotMatch() throws IOException {
    esRebuildMessageReceiver.handleMessage("randomString");
    verify(elasticSearchIndexUpdateHelper, never())
        .updateElasticSearchIndex(masterDoctorViewDtos.get(0));
  }
}
