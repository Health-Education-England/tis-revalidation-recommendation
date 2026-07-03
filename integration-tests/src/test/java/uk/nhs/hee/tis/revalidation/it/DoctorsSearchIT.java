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

package uk.nhs.hee.tis.revalidation.it;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.nhs.hee.tis.revalidation.RevalidationApplication;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;

@Slf4j
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = RevalidationApplication.class)
@TestPropertySource("classpath:application-test.yml")
@ActiveProfiles("test")
class DoctorsSearchIT extends BaseIT {

  @Autowired
  private DoctorsForDBService service;

  @Autowired
  private DoctorsForDBRepository repository;

  @BeforeEach
  public void setup() {
    repository.deleteAll();
    setupData();
  }

  @Test
  void shouldSearchForDoctorsByGmcReferenceNumber() throws Exception {

    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery(doc3.getGmcReferenceNumber())
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(1L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef3));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName3));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName3));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate3));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate3));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un3.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction3));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status3.name()));
  }

  @Test
  void shouldSearchForDoctorsByDoctorFirstName() throws Exception {

    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery(doc2.getDoctorFirstName())
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(1L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status2.name()));
  }

  @Test
  void shouldSearchForDoctorsByDoctorLastName() throws Exception {

    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery(doc5.getDoctorLastName())
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(1L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef5));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName5));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName5));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate5));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate5));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un5.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction5));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status5.name()));
  }

  @Test
  void shouldSearchForFirstOrLastName() throws Exception {
    fName2 = "smith";
    lName4 = "smith";
    doc2.setDoctorFirstName(fName2);
    doc4.setDoctorLastName(lName4);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery("smith")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(2L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status2.name()));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status4.name()));
  }

  @Test
  void shouldSearchForFirstOrLastNameOrGmcNumber() throws Exception {
    fName1 = "wAqar";
    fName2 = "saqab";
    lName4 = "wAQAs";
    gmcRef5 = "2345aqa9";
    doc1.setDoctorFirstName(fName1);
    doc2.setDoctorFirstName(fName2);
    doc4.setDoctorLastName(lName4);
    doc5.setGmcReferenceNumber(gmcRef5);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery("aqa")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(4L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status1.name()));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status2.name()));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status4.name()));

    assertThat(traineeInfo.get(3).getGmcReferenceNumber(), is(gmcRef5));
    assertThat(traineeInfo.get(3).getDoctorFirstName(), is(fName5));
    assertThat(traineeInfo.get(3).getDoctorLastName(), is(lName5));
    assertThat(traineeInfo.get(3).getSubmissionDate(), is(subDate5));
    assertThat(traineeInfo.get(3).getDateAdded(), is(addedDate5));
    assertThat(traineeInfo.get(3).getUnderNotice(), is(un5.name()));
    assertThat(traineeInfo.get(3).getSanction(), is(sanction5));
    assertThat(traineeInfo.get(3).getDoctorStatus(), is(status5.name()));
  }

  @Test
  void shouldSearchUnderNoticeDoctorsForFirstOrLastNameOrGmcNumber() throws Exception {
    un1 = UnderNotice.YES;
    un2 = UnderNotice.YES;
    un3 = UnderNotice.NO;
    un4 = UnderNotice.ON_HOLD;
    un5 = UnderNotice.YES;

    doc1.setUnderNotice(un1);
    doc2.setUnderNotice(un2);
    doc3.setUnderNotice(un3);
    doc4.setUnderNotice(un4);
    doc5.setUnderNotice(un5);

    fName1 = "wAqar";
    fName2 = "saqab";
    gmcRef3 = "AQabbbbb";
    lName4 = "wAQAs";

    doc1.setDoctorFirstName(fName1);
    doc2.setDoctorFirstName(fName2);
    doc3.setGmcReferenceNumber(gmcRef3);
    doc4.setDoctorLastName(lName4);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .underNotice(true)
        .searchQuery("aqa")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getCountUnderNotice(), is(4L));
    assertThat(doctorDTO.getTotalResults(), is(3L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status1.name()));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status2.name()));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status4.name()));

  }

  @Test
  void shouldSearchUnderNoticeDoctorsForFirstOrLastName() throws Exception {
    un1 = UnderNotice.NO;
    un2 = UnderNotice.YES;
    un3 = UnderNotice.NO;
    un4 = UnderNotice.ON_HOLD;
    un5 = UnderNotice.NO;

    doc1.setUnderNotice(un1);
    doc2.setUnderNotice(un2);
    doc3.setUnderNotice(un3);
    doc4.setUnderNotice(un4);
    doc5.setUnderNotice(un5);

    fName2 = "smith";
    lName4 = "Smith";
    fName5 = "smith";
    doc2.setDoctorFirstName(fName2);
    doc4.setDoctorLastName(lName4);
    doc5.setDoctorFirstName(fName5);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .underNotice(true)
        .searchQuery("smith")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO);
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(2L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status2.name()));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status4.name()));
  }
}
