/*
 * The MIT License (MIT)
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

package uk.nhs.hee.tis.revalidation.mapper;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.nhs.hee.tis.revalidation.entity.MasterDoctorView;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;

@ExtendWith(MockitoExtension.class)
class RecommendationViewMapperTest {

  @InjectMocks
  RecommendationViewMapperImpl recommendationViewMapper;

  @Test
  void shouldMapMasterDoctorViewDtoToRecommendationView() {
    MasterDoctorView dataToSave = new MasterDoctorView();
    dataToSave.setId("1");
    dataToSave.setTcsPersonId(1000l);
    LocalDate submissionDate_new = LocalDate.now();
    dataToSave.setSubmissionDate(submissionDate_new);
    dataToSave.setProgrammeName("programmeName_new");
    dataToSave.setMembershipType("membershipType_new");
    dataToSave.setDesignatedBody("designatedBody_new");
    dataToSave.setUnderNotice("underNotice_new");

    RecommendationView result = recommendationViewMapper.mapMasterDoctorViewToRecommendationView(
        dataToSave);

    assertThat(result.getId(), is("1"));
    assertThat(result.getTcsPersonId(), is(1000l));
    assertThat(result.getSubmissionDate(), is(submissionDate_new));
    assertThat(result.getProgrammeName(), is("programmeName_new"));
    assertThat(result.getMembershipType(), is("membershipType_new"));
    assertThat(result.getDesignatedBody(), is("designatedBody_new"));
    assertThat(result.getUnderNotice(), is("underNotice_new"));
  }
}