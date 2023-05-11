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

package uk.nhs.hee.tis.revalidation.service;

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.data.domain.Sort.by;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.NO;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.util.ReflectionTestUtils;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.mapper.RecommendationViewMapper;
import uk.nhs.hee.tis.revalidation.mapper.RecommendationViewMapperImpl;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationElasticSearchRepository;


@ExtendWith(MockitoExtension.class)
class DoctorsForDBServiceTest {

  private final Faker faker = new Faker();

  @InjectMocks
  private DoctorsForDBService doctorsForDBService;

  @Mock
  private DoctorsForDBRepository repository;

  @Mock
  private RecommendationService recommendationService;

  @Mock
  private RecommendationElasticSearchService recommendationElasticSearchService;

  @Mock
  private RecommendationElasticSearchRepository recommendationElasticSearchRepository;

  @Spy
  private RecommendationViewMapper recommendationViewMapper = new RecommendationViewMapperImpl();

  @Captor
  ArgumentCaptor<DoctorsForDB> doctorCaptor;

  @Mock
  private Page page;

  private DoctorsForDB doc1, doc2, doc3, doc4, doc5;
  private RecommendationView rv1, rv2, rv3, rv4, rv5;
  private DoctorsForDbDto docDto1, docDto2;
  private String gmcRef1, gmcRef2, gmcRef3, gmcRef4, gmcRef5;
  private String fname1, fname2, fname3, fname4, fname5;
  private String lname1, lname2, lname3, lname4, lname5;
  private LocalDate subDate1, subDate2, subDate3, subDate4, subDate5;
  private LocalDate addedDate1, addedDate2, addedDate3, addedDate4, addedDate5;
  private UnderNotice un1, un2, un3, un4, un5;
  private String sanction1, sanction2, sanction3, sanction4, sanction5;
  private RecommendationStatus status1, status2, status3, status4, status5;
  private String designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5;
  private String admin1, admin2, admin3, admin4, admin5;
  private String connectionStatus1, connectionStatus2, connectionStatus3, connectionStatus4, connectionStatus5;
  private String programmeName;
  private String outcome1;

  @BeforeEach
  public void setup() {
    ReflectionTestUtils.setField(doctorsForDBService, "pageSize", 20);
    setupData();
  }

  @Test
  void shouldReturnListOfAllDoctors() {
    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.DESC, "submissionDate");
    orders.add(customOrder);
    Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(lastNameOrder);

    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List
        .of(designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findAll("", formattedDbcs, List.of(), programmeName, outcome1, status1.name(), admin1, pageableAndSortable))
        .thenReturn(page);
    when(recommendationElasticSearchService.formatDesignatedBodyCodesForElasticsearchQuery(dbcs))
        .thenReturn(formattedDbcs);

    when(page.get()).thenReturn(Stream.of(rv1, rv2, rv3, rv4, rv5));
    when(page.getTotalPages()).thenReturn(1);
    when(repository.countByUnderNoticeIn(YES)).thenReturn(2L);
    when(repository.count()).thenReturn(5L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("desc")
        .sortColumn("submissionDate")
        .pageNumber(1)
        .searchQuery("")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();

    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of());

    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(5L));
    assertThat(allDoctors.getCountUnderNotice(), is(2L));
    assertThat(allDoctors.getTotalPages(), is(1L));
    assertThat(doctorsForDB, hasSize(5));
    String[] refs = {gmcRef1, gmcRef2, gmcRef3, gmcRef4, gmcRef5};
    String[] f_names = {fname1, fname2, fname3, fname4, fname5};
    String[] l_names = {lname1, lname2, lname3, lname4, lname5};
    LocalDate[] subDates = {subDate1, subDate2, subDate3, subDate4, subDate5};
    UnderNotice[] underNotices = {un1, un2, un3, un4, un5};
    RecommendationStatus[] statuses = {status1, status2, status3, status4, status5};
    for (int i = 0; i < doctorsForDB.size(); i++) {
      TraineeInfoDto doc = doctorsForDB.get(i);    assertThat(doc.getGmcReferenceNumber(), is(refs[i]));
      assertThat(doc.getDoctorFirstName(), is(f_names[i]));
      assertThat(doc.getDoctorLastName(), is(l_names[i]));
      assertThat(doc.getSubmissionDate(), is(subDates[i]));
      assertThat(doc.getUnderNotice(), is(underNotices[i].name()));
      assertThat(doc.getDoctorStatus(), is(statuses[i].name()));
    }
  }

  @Test
  void shouldReturnListOfDoctorsAttachedToASpecificDbc() {
    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.DESC, "submissionDate");
    orders.add(customOrder);
    Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(lastNameOrder);

    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List.of(designatedBody1);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findAll("", formattedDbcs, List.of(), programmeName, outcome1, status1.name(), admin1, pageableAndSortable)).thenReturn(
        page);
    when(recommendationElasticSearchService
        .formatDesignatedBodyCodesForElasticsearchQuery(dbcs)
    ).thenReturn(formattedDbcs);
    when(page.get()).thenReturn(Stream.of(rv1));
    when(page.getTotalPages()).thenReturn(1);
    when(repository.countByUnderNoticeIn(YES)).thenReturn(2L);
    when(repository.count()).thenReturn(5L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("desc")
        .sortColumn("submissionDate")
        .pageNumber(1)
        .searchQuery("")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();

    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of());

    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(5L));
    assertThat(allDoctors.getCountUnderNotice(), is(2L));
    assertThat(allDoctors.getTotalPages(), is(1L));
    assertThat(doctorsForDB, hasSize(1));

    assertThat(doctorsForDB.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.get(0).getDoctorFirstName(), is(fname1));
    assertThat(doctorsForDB.get(0).getDoctorLastName(), is(lname1));
    assertThat(doctorsForDB.get(0).getSubmissionDate(), is(subDate1));
    assertThat(doctorsForDB.get(0).getUnderNotice(), is(un1.name()));
    assertThat(doctorsForDB.get(0).getDoctorStatus(), is(status1.name()));
  }

  @Test
  void shouldReturnListOfUnderNoticeDoctors() {

    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.DESC, "submissionDate");
    orders.add(customOrder);
    Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(lastNameOrder);
    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List
        .of(designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findByUnderNotice("", formattedDbcs, programmeName, outcome1, status1.name(), admin1, pageableAndSortable)).thenReturn(page);
    when(recommendationElasticSearchService
        .formatDesignatedBodyCodesForElasticsearchQuery(dbcs)
    ).thenReturn(formattedDbcs);
    when(page.get()).thenReturn(Stream.of(rv1, rv2));
    when(page.getTotalPages()).thenReturn(1);
    when(repository.countByUnderNoticeIn(YES)).thenReturn(2L);
    when(repository.count()).thenReturn(5L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("desc")
        .sortColumn("submissionDate")
        .underNotice(true)
        .pageNumber(1)
        .searchQuery("")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();
    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of());
    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(5L));
    assertThat(allDoctors.getCountUnderNotice(), is(2L));
    assertThat(allDoctors.getTotalPages(), is(1L));
    assertThat(doctorsForDB, hasSize(2));

    assertThat(doctorsForDB.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.get(0).getDoctorFirstName(), is(fname1));
    assertThat(doctorsForDB.get(0).getDoctorLastName(), is(lname1));
    assertThat(doctorsForDB.get(0).getSubmissionDate(), is(subDate1));
    assertThat(doctorsForDB.get(0).getUnderNotice(), is(un1.name()));
    assertThat(doctorsForDB.get(0).getDoctorStatus(), is(status1.name()));

    assertThat(doctorsForDB.get(1).getGmcReferenceNumber(), is(gmcRef2));
    assertThat(doctorsForDB.get(1).getDoctorFirstName(), is(fname2));
    assertThat(doctorsForDB.get(1).getDoctorLastName(), is(lname2));
    assertThat(doctorsForDB.get(1).getSubmissionDate(), is(subDate2));
    assertThat(doctorsForDB.get(1).getUnderNotice(), is(un2.name()));
    assertThat(doctorsForDB.get(1).getDoctorStatus(), is(status2.name()));

  }

  @Test
  void shouldReturnEmptyListOfDoctorsWhenNoRecordFound() {
    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.DESC, "submissionDate");
    orders.add(customOrder);
    Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(lastNameOrder);

    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List
        .of(designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findAll("", formattedDbcs, List.of(), programmeName, outcome1, status1.name(), admin1, pageableAndSortable)).thenReturn(
        page);
    when(recommendationElasticSearchService
        .formatDesignatedBodyCodesForElasticsearchQuery(dbcs)
    ).thenReturn(formattedDbcs);
    when(page.get()).thenReturn(Stream.of());
    when(repository.countByUnderNoticeIn(YES)).thenReturn(0L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("desc")
        .sortColumn("submissionDate")
        .pageNumber(1)
        .searchQuery("")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();
    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of());
    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(0L));
    assertThat(allDoctors.getCountUnderNotice(), is(0L));
    assertThat(allDoctors.getTotalPages(), is(0L));
    assertThat(doctorsForDB, hasSize(0));
  }

  @Test
  void shouldReturnListOfAllDoctorsWhoMatchSearchQuery() {
    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.DESC, "submissionDate");
    orders.add(customOrder);
    Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(lastNameOrder);

    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List
        .of(designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findAll("query", formattedDbcs, List.of(), programmeName, outcome1, status1.name(), admin1, pageableAndSortable))
        .thenReturn(page);
    when(recommendationElasticSearchService
        .formatDesignatedBodyCodesForElasticsearchQuery(dbcs)
    ).thenReturn(formattedDbcs);
    when(page.get()).thenReturn(Stream.of(rv1, rv4));
    when(page.getTotalPages()).thenReturn(1);
    when(page.getTotalElements()).thenReturn(2L);
    when(repository.countByUnderNoticeIn(YES)).thenReturn(2L);
    when(repository.count()).thenReturn(5L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("desc")
        .sortColumn("submissionDate")
        .pageNumber(1)
        .searchQuery("query")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();
    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, List.of());
    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(5L));
    assertThat(allDoctors.getCountUnderNotice(), is(2L));
    assertThat(allDoctors.getTotalPages(), is(1L));
    assertThat(allDoctors.getTotalResults(), is(2L));
    assertThat(doctorsForDB, hasSize(2));

    assertThat(doctorsForDB.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.get(0).getDoctorFirstName(), is(fname1));
    assertThat(doctorsForDB.get(0).getDoctorLastName(), is(lname1));
    assertThat(doctorsForDB.get(0).getSubmissionDate(), is(subDate1));
    assertThat(doctorsForDB.get(0).getUnderNotice(), is(un1.name()));
    assertThat(doctorsForDB.get(0).getDoctorStatus(), is(status1.name()));

    assertThat(doctorsForDB.get(1).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(doctorsForDB.get(1).getDoctorFirstName(), is(fname4));
    assertThat(doctorsForDB.get(1).getDoctorLastName(), is(lname4));
    assertThat(doctorsForDB.get(1).getSubmissionDate(), is(subDate4));
    assertThat(doctorsForDB.get(1).getUnderNotice(), is(un4.name()));
    assertThat(doctorsForDB.get(1).getDoctorStatus(), is(status4.name()));

  }

  @Test
  void shouldNotFailIfGmcIdNull() {
    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.DESC, "submissionDate");
    orders.add(customOrder);
    Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(lastNameOrder);

    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List
        .of(designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findAll("query", formattedDbcs, List.of(), programmeName, outcome1, status1.name(), admin1, pageableAndSortable))
        .thenReturn(page);
    when(recommendationElasticSearchService.formatDesignatedBodyCodesForElasticsearchQuery(dbcs))
        .thenReturn(formattedDbcs);
    when(page.get()).thenReturn(Stream.of(rv1, rv4));
    when(page.getTotalPages()).thenReturn(1);
    when(page.getTotalElements()).thenReturn(2L);
    when(repository.countByUnderNoticeIn(YES)).thenReturn(2L);
    when(repository.count()).thenReturn(5L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("desc")
        .sortColumn("submissionDate")
        .pageNumber(1)
        .searchQuery("query")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();
    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, null);
    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(5L));
    assertThat(allDoctors.getCountUnderNotice(), is(2L));
    assertThat(allDoctors.getTotalPages(), is(1L));
    assertThat(allDoctors.getTotalResults(), is(2L));
    assertThat(doctorsForDB, hasSize(2));

    assertThat(doctorsForDB.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.get(0).getDoctorFirstName(), is(fname1));
    assertThat(doctorsForDB.get(0).getDoctorLastName(), is(lname1));
    assertThat(doctorsForDB.get(0).getSubmissionDate(), is(subDate1));
    assertThat(doctorsForDB.get(0).getUnderNotice(), is(un1.name()));
    assertThat(doctorsForDB.get(0).getDoctorStatus(), is(status1.name()));

    assertThat(doctorsForDB.get(1).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(doctorsForDB.get(1).getDoctorFirstName(), is(fname4));
    assertThat(doctorsForDB.get(1).getDoctorLastName(), is(lname4));
    assertThat(doctorsForDB.get(1).getSubmissionDate(), is(subDate4));
    assertThat(doctorsForDB.get(1).getUnderNotice(), is(un4.name()));
    assertThat(doctorsForDB.get(1).getDoctorStatus(), is(status4.name()));

  }

  @Test
  void shouldNotApplySecondarySortIfSortByLastName() {
    List<Order> orders = new ArrayList<>();
    Order customOrder = new Order(Sort.Direction.ASC, "doctorLastName");
    orders.add(customOrder);

    final Pageable pageableAndSortable = PageRequest.of(1, 20, by(orders));
    List<String> dbcs = List
        .of(designatedBody1, designatedBody2, designatedBody3, designatedBody4, designatedBody5);
    String formattedDbcs = String.join(" ", dbcs);
    when(recommendationElasticSearchRepository
        .findAll("query", formattedDbcs, List.of(), programmeName, outcome1, status1.name(), admin1, pageableAndSortable))
        .thenReturn(page);
    when(recommendationElasticSearchService
        .formatDesignatedBodyCodesForElasticsearchQuery(dbcs)
    ).thenReturn(formattedDbcs);
    when(page.get()).thenReturn(Stream.of(rv1, rv4));
    when(page.getTotalPages()).thenReturn(1);
    when(page.getTotalElements()).thenReturn(2L);
    when(repository.countByUnderNoticeIn(YES)).thenReturn(2L);
    when(repository.count()).thenReturn(5L);
    final var requestDTO = TraineeRequestDto.builder()
        .sortOrder("asc")
        .sortColumn("doctorLastName")
        .pageNumber(1)
        .searchQuery("query")
        .dbcs(dbcs)
        .programmeName(programmeName)
        .gmcStatus(outcome1)
        .tisStatus(status1.name())
        .admin(admin1)
        .build();
    final var allDoctors = doctorsForDBService.getAllTraineeDoctorDetails(requestDTO, null);
    final var doctorsForDB = allDoctors.getTraineeInfo();
    assertThat(allDoctors.getCountTotal(), is(5L));
    assertThat(allDoctors.getCountUnderNotice(), is(2L));
    assertThat(allDoctors.getTotalPages(), is(1L));
    assertThat(allDoctors.getTotalResults(), is(2L));
    assertThat(doctorsForDB, hasSize(2));

    assertThat(doctorsForDB.get(0).getGmcReferenceNumber(), is(gmcRef1));
    assertThat(doctorsForDB.get(0).getDoctorFirstName(), is(fname1));
    assertThat(doctorsForDB.get(0).getDoctorLastName(), is(lname1));
    assertThat(doctorsForDB.get(0).getSubmissionDate(), is(subDate1));
    assertThat(doctorsForDB.get(0).getUnderNotice(), is(un1.name()));
    assertThat(doctorsForDB.get(0).getDoctorStatus(), is(status1.name()));

    assertThat(doctorsForDB.get(1).getGmcReferenceNumber(), is(gmcRef4));
    assertThat(doctorsForDB.get(1).getDoctorFirstName(), is(fname4));
    assertThat(doctorsForDB.get(1).getDoctorLastName(), is(lname4));
    assertThat(doctorsForDB.get(1).getSubmissionDate(), is(subDate4));
    assertThat(doctorsForDB.get(1).getUnderNotice(), is(un4.name()));
    assertThat(doctorsForDB.get(1).getDoctorStatus(), is(status4.name()));

  }

  @Test
  void shouldUpdateAdmin() {
    final String newAdmin1 = faker.internet().emailAddress();
    final String newAdmin2 = faker.internet().emailAddress();
    final String newAdmin3 = faker.internet().emailAddress();
    final String newAdmin4 = faker.internet().emailAddress();
    final String newAdmin5 = faker.internet().emailAddress();
    final var ta1 = TraineeAdminDto.builder().admin(newAdmin1).gmcNumber(gmcRef1).build();
    final var ta2 = TraineeAdminDto.builder().admin(newAdmin2).gmcNumber(gmcRef2).build();
    final var ta3 = TraineeAdminDto.builder().admin(newAdmin3).gmcNumber(gmcRef3).build();
    final var ta4 = TraineeAdminDto.builder().admin(newAdmin4).gmcNumber(gmcRef4).build();
    final var ta5 = TraineeAdminDto.builder().admin(newAdmin5).gmcNumber(gmcRef5).build();
    when(repository.findById(gmcRef1)).thenReturn(Optional.of(doc1));
    when(repository.findById(gmcRef2)).thenReturn(Optional.of(doc2));
    when(repository.findById(gmcRef3)).thenReturn(Optional.of(doc3));
    when(repository.findById(gmcRef4)).thenReturn(Optional.of(doc4));
    when(repository.findById(gmcRef5)).thenReturn(Optional.of(doc5));
    doctorsForDBService.updateTraineeAdmin(List.of(ta1, ta2, ta3, ta4, ta5));
    verify(repository, times(5)).save(any());
  }

  @Test
  void shouldUpdateDesignatedBodyCode() {
    when(repository.findById(gmcRef1)).thenReturn(Optional.of(doc1));
    final var message = ConnectionMessageDto.builder().gmcId(gmcRef1).build();
    doctorsForDBService.removeDesignatedBodyCode(message);

    verify(repository).save(doc1);
  }

  @Test
  void shouldNotUpdateDesignatedBodyCodeWhenNoDoctorFound() {
    when(repository.findById(gmcRef1)).thenReturn(Optional.empty());
    final var message = ConnectionMessageDto.builder().gmcId(gmcRef1).build();
    doctorsForDBService.removeDesignatedBodyCode(message);

    verify(repository, times(0)).save(doc1);
  }

  @Test
  void shouldGetDesignatedBodyCode() {
    when(repository.findById(gmcRef1)).thenReturn(Optional.of(doc1));
    final var designatedBody = doctorsForDBService.getDesignatedBodyCode(gmcRef1);
    assertThat(designatedBody.getDesignatedBodyCode(), is(doc1.getDesignatedBodyCode()));
  }

  @Test
  void shouldGetTisStatusForDoctorOnUpdate() {
    when(repository.findById(gmcRef1)).thenReturn(Optional.of(doc1));
    doctorsForDBService.updateTrainee(docDto1);
    verify(recommendationService).getRecommendationStatusForTrainee(gmcRef1);
  }

  @Test
  void shouldSetTisStatusToCompletedForDoctorOnUpdateIfNotUnderNotice() {

    when(repository.findById(gmcRef1)).thenReturn(Optional.of(doc1));
    doctorsForDBService.updateTrainee(docDto2);
    verify(repository).save(doctorCaptor.capture());
    assertThat(doctorCaptor.getValue().getDoctorStatus(), is(RecommendationStatus.COMPLETED));
  }

  @Test
  void shouldHideAllDoctorsBySettingFlagToFalseAndDBCToNull() {
    when(repository.findAll()).thenReturn(List.of(doc1));
    doctorsForDBService.hideAllDoctors();
    verify(repository).save(doctorCaptor.capture());
    DoctorsForDB doctor = doctorCaptor.getValue();
    assertThat(doctor.getExistsInGmc(), is(false));
    assertThat(doctor.getDesignatedBodyCode(), nullValue());
  }

  @Test
  void shouldGetConvertedRecordsForGmcIds() {
    List<String> ids = List.of(gmcRef1, gmcRef2);
    List<DoctorsForDB> iterableDocs = List.of(doc1, doc2, doc3);
    when(repository.findAllById(ids)).thenReturn(iterableDocs);

    TraineeSummaryDto actualSummary = doctorsForDBService.getDoctorsByGmcIds(ids);

    assertThat(actualSummary.getTotalResults(), is(3L));
    assertThat(actualSummary.getCountTotal(), is(3L));
    List<TraineeInfoDto> actualDocs = actualSummary.getTraineeInfo();
    for (int i = 0; i < iterableDocs.size(); i++) {
      assertSubsetOfConvertedFields(actualDocs.get(i), iterableDocs.get(i));
    }
  }

  private static void assertSubsetOfConvertedFields(TraineeInfoDto actual, DoctorsForDB expected) {
    assertThat(actual.getGmcReferenceNumber(), is(expected.getGmcReferenceNumber()));
    assertThat(actual.getDoctorFirstName(), is(expected.getDoctorFirstName()));
    assertThat(actual.getDoctorLastName(), is(expected.getDoctorLastName()));
    assertThat(actual.getSubmissionDate(), is(expected.getSubmissionDate()));
    assertThat(actual.getDesignatedBody(), is(expected.getDesignatedBodyCode()));
    assertThat(actual.getDateAdded(), is(expected.getDateAdded()));
    assertThat(actual.getUnderNotice(), is(expected.getUnderNotice().name()));
    assertThat(actual.getSanction(), is(expected.getSanction()));
    assertThat(actual.getLastUpdatedDate(), is(expected.getLastUpdatedDate()));
    assertThat(actual.getAdmin(), is(expected.getAdmin()));
  }

  private void setupData() {
    gmcRef1 = faker.number().digits(8);
    gmcRef2 = faker.number().digits(8);
    gmcRef3 = faker.number().digits(8);
    gmcRef4 = faker.number().digits(8);
    gmcRef5 = faker.number().digits(8);

    fname1 = faker.name().firstName();
    fname2 = faker.name().firstName();
    fname3 = faker.name().firstName();
    fname4 = faker.name().firstName();
    fname5 = faker.name().firstName();

    lname1 = faker.name().lastName();
    lname2 = faker.name().lastName();
    lname3 = faker.name().lastName();
    lname4 = faker.name().lastName();
    lname5 = faker.name().lastName();

    subDate1 = now();
    subDate2 = now();
    subDate3 = now();
    subDate4 = now();
    subDate5 = now();

    addedDate1 = now().minusDays(5);
    addedDate2 = now().minusDays(5);
    addedDate3 = now().minusDays(5);
    addedDate4 = now().minusDays(5);
    addedDate5 = now().minusDays(5);

    un1 = faker.options().option(UnderNotice.class);
    un2 = faker.options().option(UnderNotice.class);
    un3 = faker.options().option(UnderNotice.class);
    un4 = faker.options().option(UnderNotice.class);
    un5 = faker.options().option(UnderNotice.class);

    sanction1 = faker.lorem().characters(2);
    sanction2 = faker.lorem().characters(2);
    sanction3 = faker.lorem().characters(2);
    sanction4 = faker.lorem().characters(2);
    sanction5 = faker.lorem().characters(2);

    status1 = RecommendationStatus.NOT_STARTED;
    status2 = RecommendationStatus.NOT_STARTED;
    status3 = RecommendationStatus.NOT_STARTED;
    status4 = RecommendationStatus.NOT_STARTED;
    status5 = RecommendationStatus.NOT_STARTED;

    designatedBody1 = faker.lorem().characters(8);
    designatedBody2 = faker.lorem().characters(8);
    designatedBody3 = faker.lorem().characters(8);
    designatedBody4 = faker.lorem().characters(8);
    designatedBody5 = faker.lorem().characters(8);

    admin1 = faker.internet().emailAddress();
    admin2 = faker.internet().emailAddress();
    admin3 = faker.internet().emailAddress();
    admin4 = faker.internet().emailAddress();
    admin5 = faker.internet().emailAddress();

    connectionStatus1 = "Yes";
    connectionStatus2 = "Yes";
    connectionStatus3 = "Yes";
    connectionStatus4 = "Yes";
    connectionStatus5 = "Yes";

    outcome1 = String.valueOf(RecommendationGmcOutcome.UNDER_REVIEW);

    doc1 = new DoctorsForDB(gmcRef1, fname1, lname1, subDate1, addedDate1, un1, sanction1, status1,
        now(), designatedBody1, admin1, true);
    doc2 = new DoctorsForDB(gmcRef2, fname2, lname2, subDate2, addedDate2, un2, sanction2, status2,
        now(), designatedBody2, admin2, true);
    doc3 = new DoctorsForDB(gmcRef3, fname3, lname3, subDate3, addedDate3, un3, sanction3, status3,
        now(), designatedBody3, admin3, true);
    doc4 = new DoctorsForDB(gmcRef4, fname4, lname4, subDate4, addedDate4, un4, sanction4, status4,
        now(), designatedBody4, admin4, true);
    doc5 = new DoctorsForDB(gmcRef5, fname5, lname5, subDate5, addedDate5, un5, sanction5, status5,
        now(), designatedBody5, admin5, true);

    rv1 = RecommendationView.builder()
        .gmcReferenceNumber(gmcRef1)
        .doctorFirstName(fname1)
        .doctorLastName(lname1)
        .submissionDate(subDate1)
        .underNotice(un1.name())
        .tisStatus(status1.name())
        .designatedBody(designatedBody1)
        .admin(admin1)
        .existsInGmc(true)
        .build();
    rv2 = RecommendationView.builder()
        .gmcReferenceNumber(gmcRef2)
        .doctorFirstName(fname2)
        .doctorLastName(lname2)
        .submissionDate(subDate2)
        .underNotice(un2.name())
        .tisStatus(status2.name())
        .designatedBody(designatedBody2)
        .admin(admin2)
        .existsInGmc(true)
        .build();
    rv3 = RecommendationView.builder()
        .gmcReferenceNumber(gmcRef3)
        .doctorFirstName(fname3)
        .doctorLastName(lname3)
        .submissionDate(subDate3)
        .underNotice(un3.name())
        .tisStatus(status3.name())
        .designatedBody(designatedBody3)
        .admin(admin3)
        .existsInGmc(true)
        .build();
    rv4 = RecommendationView.builder()
        .gmcReferenceNumber(gmcRef4)
        .doctorFirstName(fname4)
        .doctorLastName(lname4)
        .submissionDate(subDate4)
        .underNotice(un4.name())
        .tisStatus(status4.name())
        .designatedBody(designatedBody4)
        .admin(admin4)
        .existsInGmc(true)
        .build();
    rv5 = RecommendationView.builder()
        .gmcReferenceNumber(gmcRef5)
        .doctorFirstName(fname5)
        .doctorLastName(lname5)
        .submissionDate(subDate5)
        .underNotice(un5.name())
        .tisStatus(status5.name())
        .designatedBody(designatedBody5)
        .admin(admin5)
        .existsInGmc(true)
        .build();

    docDto1 = new DoctorsForDbDto();
    docDto1.setGmcReferenceNumber(gmcRef1);
    docDto1.setUnderNotice(YES.value());

    docDto2 = new DoctorsForDbDto();
    docDto2.setGmcReferenceNumber(gmcRef1);
    docDto2.setUnderNotice(NO.value());

    programmeName = Faker.instance().funnyName().name();
  }
}