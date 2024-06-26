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

import static org.springframework.data.domain.PageRequest.of;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.domain.Sort.by;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.NO;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;

import java.time.LocalDateTime;
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
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.RecommendationView;
import uk.nhs.hee.tis.revalidation.event.DoctorsForDbCollectedEvent;
import uk.nhs.hee.tis.revalidation.mapper.DoctorsForDbMapper;
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

  private final DoctorsForDbMapper doctorsForDbMapper;

  public DoctorsForDBService(DoctorsForDBRepository doctorsForDBRepository,
      RecommendationService recommendationService,
      RecommendationElasticSearchRepository recommendationElasticSearchRepository,
      RecommendationElasticSearchService recommendationElasticSearchService,
      RecommendationViewMapper recommendationViewMapper, DoctorsForDbMapper doctorsForDbMapper) {
    this.doctorsRepository = doctorsForDBRepository;
    this.recommendationService = recommendationService;
    this.recommendationElasticSearchRepository = recommendationElasticSearchRepository;
    this.recommendationElasticSearchService = recommendationElasticSearchService;
    this.recommendationViewMapper = recommendationViewMapper;
    this.doctorsForDbMapper = doctorsForDbMapper;
  }

  public TraineeSummaryDto getAllTraineeDoctorDetails(final TraineeRequestDto requestDTO,
      final List<String> hiddenGmcIds) {
    final var paginatedDoctors = getSortedAndFilteredDoctorsByPageNumber(requestDTO, hiddenGmcIds);
    final var doctorsList = paginatedDoctors.get()
        .map(recommendationViewMapper::toTraineeInfoDto).toList();

    return TraineeSummaryDto.builder().traineeInfo(doctorsList).countTotal(getCountAll())
        .countUnderNotice(getCountUnderNotice()).totalPages(paginatedDoctors.getTotalPages())
        .totalResults(paginatedDoctors.getTotalElements()).build();
  }

  /**
   * During nightly sync job, update DoctorsForDB data after we get data from GMC.
   *
   * @param gmcDoctor doctor dto from GMC
   */
  public void updateTrainee(final DoctorsForDbDto gmcDoctor) {
    // Set default lastUpdatedDate, existsInGmc and doctorStatus when mapping dto to entity.
    final var doctorsForDB = doctorsForDbMapper.toEntity(gmcDoctor, true,
        RecommendationStatus.NOT_STARTED);
    final var doctor = doctorsRepository.findById(gmcDoctor.getGmcReferenceNumber());
    if (doctor.isPresent()) {
      doctorsForDB.setAdmin(doctor.get().getAdmin());
      if (NO.value().equals(gmcDoctor.getUnderNotice())) {
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

  public void updateDoctorConnection(final ConnectionMessageDto message) {
    final var doctorsForDBOptional = doctorsRepository.findById(message.getGmcId());
    if (doctorsForDBOptional.isPresent()) {
      log.info("Updating designated body code from doctors for DB");
      final var dbc = message.getDesignatedBodyCode();
      final var disconnection = dbc == null;
      final var doctorsForDb = doctorsForDBOptional.get();
      doctorsForDb.setDesignatedBodyCode(dbc);
      doctorsForDb.setExistsInGmc(!disconnection);
      if (disconnection) {
        doctorsForDb.setUnderNotice(null);
      }
      doctorsForDb.setSubmissionDate(message.getSubmissionDate());
      doctorsForDb.setGmcLastUpdatedDateTime(message.getGmcLastUpdatedDateTime());
      doctorsRepository.save(doctorsForDb);
    } else {
      log.info("No doctor found to update designated body code");
    }
  }

  /**
   * Handle DoctorsForDbCollectedEvent as part of GMC Overnight Sync. Doctors in the event are
   * updated and any doctors which previously part of the Designated Body are removed.
   *
   * @param doctorsForDbCollectedEvent event that signifies that all doctors for a given DB have
   *                                   been collected
   */
  public void handleDoctorsForDbCollectedEvent(
      final DoctorsForDbCollectedEvent doctorsForDbCollectedEvent) {
    final String designatedBodyCode = doctorsForDbCollectedEvent.designatedBodyCode();
    final LocalDateTime requestDateTime = doctorsForDbCollectedEvent.requestDateTime();

    // Designated bodies have less than 8,000 doctors, well within max payload and transaction size
    doctorsForDbCollectedEvent.doctors().forEach(this::updateTrainee);

    List<DoctorsForDB> staleDoctors = doctorsRepository.findByDesignatedBodyCodeAndGmcLastUpdatedDateTimeBefore(
        designatedBodyCode, requestDateTime);
    staleDoctors.forEach(d -> {
      // To minimise concurrency issues in large collections, get the latest
      var optionalSavedDoctor = doctorsRepository.findById(d.getGmcReferenceNumber());
      optionalSavedDoctor.ifPresentOrElse(
          savedDoctor -> {
            if (designatedBodyCode.equals(savedDoctor.getDesignatedBodyCode())
                && requestDateTime.isAfter(savedDoctor.getGmcLastUpdatedDateTime())) {
              savedDoctor.setExistsInGmc(false);
              savedDoctor.setDesignatedBodyCode(null);
              savedDoctor.setGmcLastUpdatedDateTime(requestDateTime);
              doctorsRepository.save(savedDoctor);
            } else {
              log.debug("Close one.  Doctor [{}] modified between updates and being disconnected.",
                  savedDoctor.getGmcReferenceNumber());
            }
          },
          () -> log.warn("Ignoring 'stale' record no longer found for GMC Number: [{}]",
              d.getGmcReferenceNumber())
      );
    });
  }

  public TraineeSummaryDto getDoctorsByGmcIds(final List<String> gmcIds) {
    final Iterable<DoctorsForDB> doctorsForDb = doctorsRepository.findAllById(gmcIds);
    final var doctorsForDbs = IterableUtils.toList(doctorsForDb);
    final var traineeInfoDtos = doctorsForDbs.stream().map(doctorsForDbMapper::toTraineeInfoDto)
        .toList();
    return TraineeSummaryDto.builder().countTotal(traineeInfoDtos.size())
        .totalResults(traineeInfoDtos.size()).traineeInfo(traineeInfoDtos).build();
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
}
