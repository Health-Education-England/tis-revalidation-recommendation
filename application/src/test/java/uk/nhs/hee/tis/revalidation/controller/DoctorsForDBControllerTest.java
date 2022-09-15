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

package uk.nhs.hee.tis.revalidation.controller;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.List.of;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.ASC;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.AUTOCOMPLETE_FIELD;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.DESC;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.DESIGNATED_BODY_CODES;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.EMPTY_STRING;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.INPUT;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.PAGE_NUMBER;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.PAGE_NUMBER_VALUE;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.SEARCH_QUERY;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.SORT_COLUMN;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.SORT_ORDER;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.SUBMISSION_DATE;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.UNDER_NOTICE;
import static uk.nhs.hee.tis.revalidation.controller.DoctorsForDBController.UNDER_NOTICE_VALUE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.nhs.hee.tis.revalidation.dto.DesignatedBodyDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminUpdateDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;
import uk.nhs.hee.tis.revalidation.service.RecommendationElasticSearchService;

@ExtendWith(MockitoExtension.class)
@WebMvcTest(DoctorsForDBController.class)
class DoctorsForDBControllerTest {

  private static final String DOCTORS_API_URL = "/api/v1/doctors";
  private static final String UNHIDDEN_DOCTORS_API_URL = "/api/v1/doctors/unhidden";
  private static final String DOCTORS_API_URL_BY_GMC_ID = "/api/v1/doctors/gmcIds";
  private static final String UPDATE_ADMIN = "/assign-admin";
  private static final String GET_DESIGNATED_BODY = "/designated-body";
  private static final String GET_AUTOCOMPLETE = "/api/v1/doctors/autocomplete";


  private final Faker faker = new Faker();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper mapper;

  @MockBean
  private DoctorsForDBService doctorsForDBService;

  @MockBean
  private RecommendationElasticSearchService recommendationElasticSearchService;

  private String gmcRef1, gmcRef2;
  private String firstName1, firstName2;
  private String lastName1, lastName2;
  private LocalDate submissionDate1, submissionDate2;
  private LocalDate dateAdded1, dateAdded2;
  private UnderNotice underNotice1, underNotice2;
  private String sanction1, sanction2;
  private RecommendationStatus doctorStatus1, doctorStatus2;
  private String admin;
  private String designatedBody1, designatedBody2;
  private String connectionStatus1;
  private String connectionStatus2;

  @BeforeEach
  public void setup() {
    gmcRef1 = faker.number().digits(8);
    gmcRef2 = faker.number().digits(8);
    firstName1 = faker.name().firstName();
    firstName2 = faker.name().firstName();
    lastName1 = faker.name().lastName();
    lastName2 = faker.name().lastName();
    submissionDate1 = now();
    submissionDate2 = now();
    dateAdded1 = now().minusDays(5);
    dateAdded2 = now().minusDays(5);
    underNotice1 = UnderNotice.YES;
    underNotice2 = UnderNotice.ON_HOLD;
    sanction1 = faker.lorem().characters(2);
    sanction2 = faker.lorem().characters(2);
    doctorStatus1 = RecommendationStatus.STARTED;
    doctorStatus2 = RecommendationStatus.SUBMITTED_TO_GMC;
    admin = faker.internet().emailAddress();
    designatedBody1 = faker.lorem().characters(8);
    designatedBody2 = faker.lorem().characters(8);
    connectionStatus1 = faker.lorem().characters(3);
    connectionStatus2 = faker.lorem().characters(3);
  }

  @Test
  void shouldReturnTraineeDoctorsInformation() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    final var requestDTO = TraineeRequestDto.builder().sortOrder(ASC).sortColumn(SUBMISSION_DATE)
        .searchQuery(EMPTY_STRING).dbcs(List.of(designatedBody1, designatedBody2)).build();
    when(doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of())).thenReturn(
        gmcDoctorDTO);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    this.mockMvc.perform(
            get("/api/v1/doctors").param(SORT_ORDER, ASC).param(SORT_COLUMN, SUBMISSION_DATE)
                .param(UNDER_NOTICE, UNDER_NOTICE_VALUE).param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
                .param(SEARCH_QUERY, EMPTY_STRING).param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(gmcDoctorDTO)));
  }

  @Test
  void shouldReturnUnhiddenTraineeDoctorsInformation() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    final var requestDTO = TraineeRequestDto.builder().sortOrder(ASC).sortColumn(SUBMISSION_DATE)
        .searchQuery(EMPTY_STRING).dbcs(List.of(designatedBody1, designatedBody2)).build();
    when(doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of(gmcRef1))).thenReturn(
        gmcDoctorDTO);
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    final var url = format("%s/%s", UNHIDDEN_DOCTORS_API_URL, gmcRef1);
    this.mockMvc.perform(get(url).param(SORT_ORDER, ASC).param(SORT_COLUMN, SUBMISSION_DATE)
            .param(UNDER_NOTICE, UNDER_NOTICE_VALUE).param(PAGE_NUMBER, PAGE_NUMBER_VALUE)
            .param(SEARCH_QUERY, EMPTY_STRING).param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.traineeInfo.[*].gmcReferenceNumber").value(hasItem(gmcRef2)));
  }

  @Test
  void shouldReturnDataWhenSortOrderAndSortColumnAreEmpty() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    final var requestDTO = TraineeRequestDto.builder().sortOrder(ASC).sortColumn(SUBMISSION_DATE)
        .searchQuery(EMPTY_STRING).dbcs(List.of(designatedBody1, designatedBody2)).build();
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    when(doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of())).thenReturn(
        gmcDoctorDTO);
    this.mockMvc.perform(get("/api/v1/doctors").param(SORT_ORDER, "").param(SORT_COLUMN, "")
            .param(DESIGNATED_BODY_CODES, dbcString)).andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(gmcDoctorDTO)));
  }

  @Test
  void shouldReturnDataWhenSortOrderAndSortColumnAreInvalid() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    final var requestDTO = TraineeRequestDto.builder().sortOrder(DESC).sortColumn(SUBMISSION_DATE)
        .searchQuery(EMPTY_STRING).dbcs(List.of(designatedBody1, designatedBody2)).build();
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    when(doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of())).thenReturn(
        gmcDoctorDTO);
    this.mockMvc.perform(get(DOCTORS_API_URL).param(SORT_ORDER, "aa").param(SORT_COLUMN, "date")
            .param(DESIGNATED_BODY_CODES, dbcString)).andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(gmcDoctorDTO)));
  }

  @Test
  void shouldReturnUnderNoticeTraineeDoctorsInformation() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    final var requestDTO = TraineeRequestDto.builder().sortOrder(ASC).sortColumn(SUBMISSION_DATE)
        .underNotice(true).searchQuery(EMPTY_STRING).dbcs(List.of(designatedBody1, designatedBody2))
        .build();
    final var dbcString = String.format("%s,%s", designatedBody1, designatedBody2);
    when(doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of())).thenReturn(
        gmcDoctorDTO);
    this.mockMvc.perform(
            get(DOCTORS_API_URL).param(SORT_ORDER, ASC).param(SORT_COLUMN, SUBMISSION_DATE)
                .param(UNDER_NOTICE, String.valueOf(true)).param(DESIGNATED_BODY_CODES, dbcString))
        .andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(gmcDoctorDTO)));
  }

  @Test
  void shouldSetDbcsToAllWhenItsEmpty() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    final var requestDTO = TraineeRequestDto.builder().sortOrder(ASC).sortColumn(SUBMISSION_DATE)
        .underNotice(true).searchQuery(EMPTY_STRING).build();
    when(doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of())).thenReturn(
        gmcDoctorDTO);
    this.mockMvc.perform(
        get(DOCTORS_API_URL).param(SORT_ORDER, ASC).param(SORT_COLUMN, SUBMISSION_DATE)
            .param(UNDER_NOTICE, String.valueOf(true))).andExpect(status().isOk());
  }


  @Test
  void shouldUpdateAdminForTrainee() throws Exception {
    final var url = format("%s/%s", DOCTORS_API_URL, UPDATE_ADMIN);
    final var ta1 = TraineeAdminDto.builder().gmcNumber(gmcRef1).admin(admin).build();
    final var ta2 = TraineeAdminDto.builder().gmcNumber(gmcRef2).admin(admin).build();
    final var traineeAdminUpdateDto = TraineeAdminUpdateDto.builder().traineeAdmins(of(ta1, ta2))
        .build();
    this.mockMvc.perform(
        post(url).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(traineeAdminUpdateDto))).andExpect(status().isOk());
  }

  @Test
  void shouldReturnDesignatedBodyCode() throws Exception {

    final var designatedBodyDto = DesignatedBodyDto.builder().designatedBodyCode(designatedBody1)
        .build();
    when(doctorsForDBService.getDesignatedBodyCode(gmcRef1)).thenReturn(designatedBodyDto);
    final var url = format("%s%s/%s", DOCTORS_API_URL, GET_DESIGNATED_BODY, gmcRef1);
    this.mockMvc.perform(get(url))
        .andExpect(content().json(mapper.writeValueAsString(designatedBodyDto)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnDoctorsByGmcId() throws Exception {
    final var gmcDoctorDTO = prepareGmcDoctor();
    when(doctorsForDBService.getDoctorsByGmcIds(List.of(gmcRef1))).thenReturn(gmcDoctorDTO);
    final var url = format("%s/%s", DOCTORS_API_URL_BY_GMC_ID, gmcRef1);
    this.mockMvc.perform(get(url))
        .andExpect(content().json(mapper.writeValueAsString(gmcDoctorDTO)))
        .andExpect(status().isOk());
  }

  @Test
  void shouldReturnAutocompleteResultsForProgrammeName() throws Exception {
    final var fieldNameParam = "programmeName";
    final var inputParam = "general prac";
    final var dbcsParam = List.of("1-AIIDWI");
    var result = List.of("General Practice Salford and Trafford", "General Practice York");

    var returnedValue = when(
        recommendationElasticSearchService.getAutocompleteResults(fieldNameParam, inputParam,
            dbcsParam)).thenReturn(result);

    this.mockMvc.perform(
            get(GET_AUTOCOMPLETE).param(AUTOCOMPLETE_FIELD, fieldNameParam).param(INPUT, inputParam)
                .param(DESIGNATED_BODY_CODES, new String[]{"1-AIIDWI"})).andExpect(status().isOk())
        .andExpect(content().json(mapper.writeValueAsString(result)));
  }

  private TraineeSummaryDto prepareGmcDoctor() {
    final var doctorsForDB = buildDoctorsForDBList();
    return TraineeSummaryDto.builder().traineeInfo(doctorsForDB).countTotal(doctorsForDB.size())
        .countUnderNotice(1l).build();
  }

  private List<TraineeInfoDto> buildDoctorsForDBList() {
    final var doctor1 = TraineeInfoDto.builder().gmcReferenceNumber(gmcRef1)
        .doctorFirstName(firstName1).doctorLastName(lastName1).submissionDate(submissionDate1)
        .dateAdded(dateAdded1).underNotice(underNotice1.value()).sanction(sanction1)
        .doctorStatus(doctorStatus1.name()).connectionStatus(connectionStatus1).build();

    final var doctor2 = TraineeInfoDto.builder().gmcReferenceNumber(gmcRef2)
        .doctorFirstName(firstName2).doctorLastName(lastName2).submissionDate(submissionDate2)
        .dateAdded(dateAdded2).underNotice(underNotice2.value()).sanction(sanction2)
        .doctorStatus(doctorStatus2.name()).connectionStatus(connectionStatus2).build();
    return of(doctor1, doctor2);
  }
}