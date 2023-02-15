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

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.convertDateInGmcFormat;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.revalidation.RevalidationApplication;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;

@Slf4j
@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = RevalidationApplication.class)
@TestPropertySource("classpath:application-test.yml")
@ActiveProfiles("test")
class DoctorsForDBServiceIT extends BaseIT {

  @Autowired
  private DoctorsForDBService service;

  @Autowired
  private DoctorsForDBRepository repository;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(service, "pageSize", 20);
    repository.deleteAll();
    setupData();
  }

  @DisplayName("Trainee doctors information should be sorted by submission date in desc order")
  @Test
  void shouldReturnDataSortBySubmissionDateInDescOrder() throws Exception {
    subDate1 = now().minusDays(5);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(8);
    subDate4 = now().minusDays(1);
    subDate5 = now().minusDays(3);

    doc1.setSubmissionDate(subDate1);
    doc2.setSubmissionDate(subDate2);
    doc3.setSubmissionDate(subDate3);
    doc4.setSubmissionDate(subDate4);
    doc5.setSubmissionDate(subDate5);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status4.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus1));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status2.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus2));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef5));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName5));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName5));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate5));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate5));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un5.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction5));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status5.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus5));

    assertThat(traineeInfo.get(3).getGmcReferenceNumber(), is(this.gmcRef1));
    assertThat(traineeInfo.get(3).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(3).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(3).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(3).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(3).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(3).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(3).getDoctorStatus(), is(status1.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus1));

    assertThat(traineeInfo.get(4).getGmcReferenceNumber(), is(gmcRef3));
    assertThat(traineeInfo.get(4).getDoctorFirstName(), is(fName3));
    assertThat(traineeInfo.get(4).getDoctorLastName(), is(lName3));
    assertThat(traineeInfo.get(4).getSubmissionDate(), is(subDate3));
    assertThat(traineeInfo.get(4).getDateAdded(), is(addedDate3));
    assertThat(traineeInfo.get(4).getUnderNotice(), is(un3.name()));
    assertThat(traineeInfo.get(4).getSanction(), is(sanction3));
    assertThat(traineeInfo.get(4).getDoctorStatus(), is(status3.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus3));
  }

  @DisplayName("Trainee doctors information should be sorted by first name in asc order")
  @Test
  void shouldReturnDataSortByFirstNameInAscOrder() throws Exception {
    fName1 = "Zolo";
    fName2 = "Andy";
    fName3 = "Mark";

    doc1.setDoctorFirstName(fName1);
    doc2.setDoctorFirstName(fName2);
    doc3.setDoctorFirstName(fName3);
    repository.saveAll(List.of(doc1, doc2, doc3));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("doctorFirstName")
        .sortOrder("asc")
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(3L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status2.name()));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef3));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName3));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName3));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate3));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate3));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un3.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction3));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status3.name()));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status1.name()));

  }

  @DisplayName("Trainee doctors information should be sorted by last name in desc order")
  @Test
  void shouldReturnDataSortByLastNameInDescOrder() throws Exception {
    lName1 = "John";
    lName2 = "Adam";
    lName3 = "Webber";

    doc1.setDoctorLastName(lName1);
    doc2.setDoctorLastName(lName2);
    doc3.setDoctorLastName(lName3);

    repository.saveAll(List.of(doc1, doc2, doc3));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("doctorLastName")
        .sortOrder("desc")
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(3L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef3));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName3));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName3));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate3));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate3));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un3.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction3));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status3.name()));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status1.name()));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status2.name()));
  }

  @DisplayName("Trainee doctors information should be sorted by submission date in desc order")
  @Test
  void shouldReturnDataWithTotalCountAndUnderNoticeCount() throws Exception {
    un1 = UnderNotice.YES;
    un2 = UnderNotice.NO;
    un3 = UnderNotice.YES;
    un4 = UnderNotice.ON_HOLD;
    un5 = UnderNotice.NO;

    doc1.setUnderNotice(un1);
    doc2.setUnderNotice(un2);
    doc3.setUnderNotice(un3);
    doc4.setUnderNotice(un4);
    doc5.setUnderNotice(un5);

    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getCountUnderNotice(), is(3L));
  }

  @DisplayName("Get Under Notice Trainee doctors information also sorted by submission date in desc order")
  @Test
  void shouldReturnUnderNoticeDoctorsSortBySubmissionDate() throws Exception {

    subDate1 = now().minusDays(5);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(8);
    subDate4 = now().minusDays(1);
    subDate5 = now().minusDays(3);

    un1 = UnderNotice.YES;
    un2 = UnderNotice.YES;
    un3 = UnderNotice.NO;
    un4 = UnderNotice.ON_HOLD;
    un5 = UnderNotice.YES;

    doc1.setSubmissionDate(subDate1);
    doc2.setSubmissionDate(subDate2);
    doc3.setSubmissionDate(subDate3);
    doc4.setSubmissionDate(subDate4);
    doc5.setSubmissionDate(subDate5);

    doc1.setUnderNotice(un1);
    doc2.setUnderNotice(un2);
    doc3.setUnderNotice(un3);
    doc4.setUnderNotice(un4);
    doc5.setUnderNotice(un5);

    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .underNotice(true)
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getCountUnderNotice(), is(4L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status4.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus4));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status2.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus2));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef5));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName5));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName5));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate5));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate5));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un5.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction5));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status5.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus5));

    assertThat(traineeInfo.get(3).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(3).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(3).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(3).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(3).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(3).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(3).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(3).getDoctorStatus(), is(status1.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus1));
  }

  @Test
  void shouldReturnUnderNoticeDoctorsFilterByDesignatedBodies() throws Exception {

    subDate1 = now().minusDays(5);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(8);
    subDate4 = now().minusDays(1);
    subDate5 = now().minusDays(3);

    un1 = UnderNotice.YES;
    un2 = UnderNotice.YES;
    un3 = UnderNotice.NO;
    un4 = UnderNotice.ON_HOLD;
    un5 = UnderNotice.YES;

    doc1.setSubmissionDate(subDate1);
    doc2.setSubmissionDate(subDate2);
    doc3.setSubmissionDate(subDate3);
    doc4.setSubmissionDate(subDate4);
    doc5.setSubmissionDate(subDate5);

    doc1.setUnderNotice(un1);
    doc2.setUnderNotice(un2);
    doc3.setUnderNotice(un3);
    doc4.setUnderNotice(un4);
    doc5.setUnderNotice(un5);

    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .underNotice(true)
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getCountUnderNotice(), is(4L));
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
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus2));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status1.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus1));
  }

  @DisplayName("Trainee doctors information should be paginated and sorted by submission date in desc order")
  @Test
  void shouldReturnTraineeInfoInPaginatedForm() throws Exception {
    subDate1 = now().minusDays(5);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(8);
    subDate4 = now().minusDays(1);
    subDate5 = now().minusDays(3);

    doc1.setSubmissionDate(subDate1);
    doc2.setSubmissionDate(subDate2);
    doc3.setSubmissionDate(subDate3);
    doc4.setSubmissionDate(subDate4);
    doc5.setSubmissionDate(subDate5);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .pageNumber(0)
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    ReflectionTestUtils.setField(service, "pageSize", 2);
    //fetch record for first page
    var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTraineeInfo(), hasSize(2));
    assertThat(doctorDTO.getTotalPages(), is(3L));

    var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName4));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName4));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate4));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate4));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un4.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction4));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status4.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus4));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status2.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus4));

    requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .pageNumber(1)
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    //fetch record for second page
    doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTraineeInfo(), hasSize(2));

    traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef5));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName5));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName5));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate5));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate5));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un5.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction5));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status5.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus5));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status1.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus1));

    requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .pageNumber(2)
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();

    //fetch record for third page
    doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTraineeInfo(), hasSize(1));

    traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef3));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName3));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName3));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate3));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate3));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un3.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction3));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status3.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus3));
  }

  @DisplayName("Trainee doctors information should be not return any data if page number is not correct")
  @Test
  void shouldReturnNoDataWhenPassInvalidPageNumber() {
    subDate1 = now().minusDays(5);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(8);
    subDate4 = now().minusDays(1);
    subDate5 = now().minusDays(3);

    doc1.setSubmissionDate(subDate1);
    doc2.setSubmissionDate(subDate2);
    doc3.setSubmissionDate(subDate3);
    doc4.setSubmissionDate(subDate4);
    doc5.setSubmissionDate(subDate5);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .pageNumber(6)
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8", "1-AIIDVS"))
        .build();
    ReflectionTestUtils.setField(service, "pageSize", 2);
    var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTraineeInfo(), hasSize(0));
    assertThat(doctorDTO.getTotalPages(), is(3L));
  }

  @Test
  void shouldUpdateAdminForTrainee() {
    final var adminEmail1 = faker.internet().emailAddress();
    final var adminEmail2 = faker.internet().emailAddress();
    final var ta1 = TraineeAdminDto.builder().admin(adminEmail1).gmcNumber(gmcRef1).build();
    final var ta2 = TraineeAdminDto.builder().admin(adminEmail2).gmcNumber(gmcRef2).build();
    repository.saveAll(List.of(doc1, doc2));
    service.updateTraineeAdmin(List.of(ta1, ta2));
    var doctorsForDB = repository.findById(gmcRef1);

    assertThat(doctorsForDB.isPresent(), is(true));
    assertThat(doctorsForDB.get().getAdmin(), is(adminEmail1));

    doctorsForDB = repository.findById(gmcRef2);

    assertThat(doctorsForDB.isPresent(), is(true));
    assertThat(doctorsForDB.get().getAdmin(), is(adminEmail2));
  }

  @Test
  void shouldKeepAdminForTraineeWhenNoChange() {
    repository.saveAll(List.of(doc1, doc2));
    service.updateTraineeAdmin(List.of());
    var doctorsForDB = repository.findById(gmcRef1);

    assertThat(doctorsForDB.isPresent(), is(true));
    assertThat(doctorsForDB.get().getAdmin(), is(admin));
  }

  @Test
  void shouldRetainAdminValue() {
    final var adminEmail = faker.internet().emailAddress();
    final var doctorsForDbDto1 = DoctorsForDbDto.builder()
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(fName1)
        .doctorLastName(lName1)
        .submissionDate(convertDateInGmcFormat(subDate1))
        .underNotice(un1.value())
        .dateAdded(convertDateInGmcFormat(addedDate1))
        .sanction(sanction1)
        .designatedBodyCode(desBody1)
        .build();
    service.updateTrainee(doctorsForDbDto1);
    var doctorsForDB = repository.findById(gmcRef1).get();
    assertThat(doctorsForDB.getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.getAdmin(), is(nullValue()));

    final var ta1 = TraineeAdminDto.builder().admin(adminEmail).gmcNumber(gmcRef1).build();
    service.updateTraineeAdmin(List.of(ta1));

    doctorsForDB = repository.findById(gmcRef1).get();
    assertThat(doctorsForDB.getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.getAdmin(), is(adminEmail));

    service.updateTrainee(doctorsForDbDto1);
    doctorsForDB = repository.findById(gmcRef1).get();
    assertThat(doctorsForDB.getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.getAdmin(), is(adminEmail));
  }

  @Test
  void shouldReturnDataSortBySubmissionDateInDescOrderFilterByDesignatedBody()
      throws Exception {
    subDate1 = now().minusDays(5);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(8);
    subDate4 = now().minusDays(1);
    subDate5 = now().minusDays(3);

    doc1.setSubmissionDate(subDate1);
    doc2.setSubmissionDate(subDate2);
    doc3.setSubmissionDate(subDate3);
    doc4.setSubmissionDate(subDate4);
    doc5.setSubmissionDate(subDate5);
    repository.saveAll(List.of(doc1, doc2, doc3, doc4, doc5));

    final var requestDTO = TraineeRequestDto.builder()
        .sortColumn("submissionDate")
        .sortOrder("desc")
        .searchQuery("")
        .dbcs(List.of("1-AIIDR8"))
        .build();
    final var doctorDTO = service.getAllTraineeDoctorDetails(requestDTO, List.of());
    assertThat(doctorDTO.getCountTotal(), is(5L));
    assertThat(doctorDTO.getTotalResults(), is(3L));

    final var traineeInfo = doctorDTO.getTraineeInfo();

    assertThat(traineeInfo.get(0).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(traineeInfo.get(0).getDoctorFirstName(), is(fName2));
    assertThat(traineeInfo.get(0).getDoctorLastName(), is(lName2));
    assertThat(traineeInfo.get(0).getSubmissionDate(), is(subDate2));
    assertThat(traineeInfo.get(0).getDateAdded(), is(addedDate2));
    assertThat(traineeInfo.get(0).getUnderNotice(), is(un2.name()));
    assertThat(traineeInfo.get(0).getSanction(), is(sanction2));
    assertThat(traineeInfo.get(0).getDoctorStatus(), is(status2.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus2));

    assertThat(traineeInfo.get(1).getGmcReferenceNumber(), is(this.gmcRef1));
    assertThat(traineeInfo.get(1).getDoctorFirstName(), is(fName1));
    assertThat(traineeInfo.get(1).getDoctorLastName(), is(lName1));
    assertThat(traineeInfo.get(1).getSubmissionDate(), is(subDate1));
    assertThat(traineeInfo.get(1).getDateAdded(), is(addedDate1));
    assertThat(traineeInfo.get(1).getUnderNotice(), is(un1.name()));
    assertThat(traineeInfo.get(1).getSanction(), is(sanction1));
    assertThat(traineeInfo.get(1).getDoctorStatus(), is(status1.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus1));

    assertThat(traineeInfo.get(2).getGmcReferenceNumber(), is(gmcRef3));
    assertThat(traineeInfo.get(2).getDoctorFirstName(), is(fName3));
    assertThat(traineeInfo.get(2).getDoctorLastName(), is(lName3));
    assertThat(traineeInfo.get(2).getSubmissionDate(), is(subDate3));
    assertThat(traineeInfo.get(2).getDateAdded(), is(addedDate3));
    assertThat(traineeInfo.get(2).getUnderNotice(), is(un3.name()));
    assertThat(traineeInfo.get(2).getSanction(), is(sanction3));
    assertThat(traineeInfo.get(2).getDoctorStatus(), is(status3.name()));
    assertThat(traineeInfo.get(0).getTisConnectionStatus(), is(tisConnectionStatus3));
  }

}
