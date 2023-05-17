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
import static java.util.stream.Collectors.toList;
import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.NO;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.DesignatedBodyDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.mapper.RecommendationViewMapper;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.repository.RecommendationElasticSearchRepository;

@Slf4j
@Transactional
@Service
public class DoctorsForDBService {

  @Value("${app.reval.pagination.pageSize}")
  private int pageSize;

  private final DoctorsForDBRepository doctorsRepository;

  private final RecommendationService recommendationService;

  private final RecommendationElasticSearchService recommendationElasticSearchService;

  private final RecommendationElasticSearchRepository recommendationElasticSearchRepository;

  private final RecommendationViewMapper recommendationViewMapper;

  public DoctorsForDBService(DoctorsForDBRepository doctorsForDBRepository,
      RecommendationService recommendationService,
      RecommendationElasticSearchRepository recommendationElasticSearchRepository,
      RecommendationElasticSearchService recommendationElasticSearchService,
      RecommendationViewMapper recommendationViewMapper) {
    this.doctorsRepository = doctorsForDBRepository;
    this.recommendationService = recommendationService;
    this.recommendationElasticSearchRepository = recommendationElasticSearchRepository;
    this.recommendationElasticSearchService = recommendationElasticSearchService;
    this.recommendationViewMapper = recommendationViewMapper;
  }

  public TraineeSummaryDto getAllTraineeDoctorDetails(final TraineeRequestDto requestDTO,
      final List<String> hiddenGmcIds) {
    final var paginatedDoctors = getSortedAndFilteredDoctorsByPageNumber(requestDTO, hiddenGmcIds);
    final var doctorsList = paginatedDoctors.get().collect(toList());
    final var traineeDoctors = doctorsList.stream().map(recommendationViewMapper::toTraineeInfoDto)
        .collect(toList());

    return TraineeSummaryDto.builder().traineeInfo(traineeDoctors).countTotal(getCountAll())
        .countUnderNotice(getCountUnderNotice()).totalPages(paginatedDoctors.getTotalPages())
        .totalResults(paginatedDoctors.getTotalElements()).build();
  }

  public void updateTrainee(final DoctorsForDbDto gmcDoctor) {
    final var doctorsForDB = DoctorsForDB.convert(gmcDoctor);
    final var doctor = doctorsRepository.findById(gmcDoctor.getGmcReferenceNumber());
    if (doctor.isPresent()) {
      doctorsForDB.setAdmin(doctor.get().getAdmin());
      if (gmcDoctor.getUnderNotice().equals(NO.value())) {
        doctorsForDB.setDoctorStatus(RecommendationStatus.COMPLETED);
      } else {
        doctorsForDB.setDoctorStatus(recommendationService.getRecommendationStatusForTrainee(
            gmcDoctor.getGmcReferenceNumber()));
      }
    } else {
      doctorsForDB.setDoctorStatus(RecommendationStatus.NOT_STARTED);
    }
    doctorsRepository.save(doctorsForDB);
  }

  public void updateTraineeAdmin(final List<TraineeAdminDto> traineeAdmins) {
    traineeAdmins.forEach(traineeAdmin -> {
      final var doctor = doctorsRepository.findById(traineeAdmin.getGmcNumber());
      if (doctor.isPresent()) {
        final var doctorsForDB = doctor.get();
        doctorsForDB.setAdmin(traineeAdmin.getAdmin());
        doctorsForDB.setLastUpdatedDate(now());
        doctorsRepository.save(doctorsForDB);
      }
    });
  }

  public DesignatedBodyDto getDesignatedBodyCode(final String gmcId) {
    final var doctorsForDB = doctorsRepository.findById(gmcId);
    final var designatedBodyCode = doctorsForDB.map(DoctorsForDB::getDesignatedBodyCode)
        .orElse(null);
    return DesignatedBodyDto.builder().designatedBodyCode(designatedBodyCode).build();
  }

  public void updateDesignatedBodyCode(final ConnectionMessageDto message) {
    final var doctorsForDBOptional = doctorsRepository.findById(message.getGmcId());
    if (doctorsForDBOptional.isPresent()) {
      log.info("Updating designated body code from doctors for DB");
      final var dbc = message.getDesignatedBodyCode();
      final var doctorsForDB = doctorsForDBOptional.get();
      doctorsForDB.setDesignatedBodyCode(dbc);
      doctorsForDB.setExistsInGmc(dbc == null);
      doctorsRepository.save(doctorsForDB);
    } else {
      log.info("No doctor found to update designated body code");
    }
  }

  public TraineeSummaryDto getDoctorsByGmcIds(final List<String> gmcIds) {
    final Iterable<DoctorsForDB> doctorsForDb = doctorsRepository.findAllById(gmcIds);
    final var doctorsForDBS = IterableUtils.toList(doctorsForDb);
    final var traineeInfoDtos = doctorsForDBS.stream().map(this::convert).collect(toList());
    return TraineeSummaryDto.builder().countTotal(traineeInfoDtos.size())
        .totalResults(traineeInfoDtos.size()).traineeInfo(traineeInfoDtos).build();
  }

  public void hideAllDoctors() {
    List<DoctorsForDB> doctors = doctorsRepository.findAll();
    doctors.forEach(doctor -> {
      doctor.setExistsInGmc(false);
      doctor.setDesignatedBodyCode(null);
      doctorsRepository.save(doctor);
    });
  }

  private TraineeInfoDto convert(final DoctorsForDB doctorsForDB) {
    final var traineeInfoDTOBuilder = TraineeInfoDto.builder()
        .gmcReferenceNumber(doctorsForDB.getGmcReferenceNumber())
        .doctorFirstName(doctorsForDB.getDoctorFirstName())
        .doctorLastName(doctorsForDB.getDoctorLastName())
        .submissionDate(doctorsForDB.getSubmissionDate())
        .designatedBody(doctorsForDB.getDesignatedBodyCode()).dateAdded(doctorsForDB.getDateAdded())
        .underNotice(doctorsForDB.getUnderNotice().name()).sanction(doctorsForDB.getSanction())
        .doctorStatus(doctorsForDB.getDoctorStatus().name()) //TODO update with legacy statuses
        .lastUpdatedDate(doctorsForDB.getLastUpdatedDate()).admin(doctorsForDB.getAdmin())
        .connectionStatus(getConnectionStatus(doctorsForDB.getDesignatedBodyCode()));

    return traineeInfoDTOBuilder.build();

  }

  private Page<RecommendationView> getSortedAndFilteredDoctorsByPageNumber(
      final TraineeRequestDto requestDTO, final List<String> hiddenGmcIds) {
    final var hiddenGmcIdsNotNull = (hiddenGmcIds == null) ? new ArrayList<String>() : hiddenGmcIds;

    List<Order> orders = new ArrayList<>();
    final var customSortColumn = requestDTO.getSortColumn();
    Order customOrder = new Order("asc".equalsIgnoreCase(requestDTO.getSortOrder()) ? ASC : DESC,
        customSortColumn);
    orders.add(customOrder);
    if (!customSortColumn.equalsIgnoreCase("doctorLastName")) {
      Order lastNameOrder = new Order(Sort.Direction.ASC, "doctorLastName");
      orders.add(lastNameOrder);
    }
    final var pageableAndSortable = of(requestDTO.getPageNumber(), pageSize, by(orders));
    final var designatedBodyCodes = recommendationElasticSearchService.formatDesignatedBodyCodesForElasticsearchQuery(
        requestDTO.getDbcs());

    final String programmeName = requestDTO.getProgrammeName();
    final String gmcStatus = requestDTO.getGmcStatus();
    final String tisStatus = requestDTO.getTisStatus();
    final String admin = requestDTO.getAdmin();
    if (requestDTO.isUnderNotice()) {

      return recommendationElasticSearchRepository.findByUnderNotice(
          requestDTO.getSearchQuery().toLowerCase(), designatedBodyCodes, programmeName, gmcStatus,
          tisStatus, admin, pageableAndSortable);
    }

    return recommendationElasticSearchRepository.findAll(requestDTO.getSearchQuery().toLowerCase(),
        designatedBodyCodes, hiddenGmcIdsNotNull, programmeName, gmcStatus, tisStatus, admin,
        pageableAndSortable);
  }

  //TODO: explore to implement cache
  private long getCountAll() {
    return doctorsRepository.count();
  }

  //TODO: explore to implement cache
  private long getCountUnderNotice() {
    return doctorsRepository.countByUnderNoticeIn(YES);
  }

  private String getConnectionStatus(final String designatedBody) {
    return (designatedBody == null || designatedBody.equals("")) ? "No" : "Yes";
  }
}
