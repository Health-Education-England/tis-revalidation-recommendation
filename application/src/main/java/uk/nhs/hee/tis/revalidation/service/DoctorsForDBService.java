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
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.ON_HOLD;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.YES;
import static uk.nhs.hee.tis.revalidation.entity.UnderNotice.NO;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.IterableUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.DesignatedBodyDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@Slf4j
@Transactional
@Service
public class DoctorsForDBService {

  @Value("${app.reval.pagination.pageSize}")
  private int pageSize;

  private String hiddenPrefix = "last-";

  private DoctorsForDBRepository doctorsRepository;

  private RecommendationService recommendationService;

  public DoctorsForDBService(
    DoctorsForDBRepository doctorsForDBRepository,
    RecommendationService recommendationService
  ) {
    this.doctorsRepository = doctorsForDBRepository;
    this.recommendationService = recommendationService;
  }

  public TraineeSummaryDto getAllTraineeDoctorDetails(final TraineeRequestDto requestDTO,
      final List<String> hiddenGmcIds) {
    final var paginatedDoctors = getSortedAndFilteredDoctorsByPageNumber(requestDTO, hiddenGmcIds);
    final var doctorsList = paginatedDoctors.get().collect(toList());
    final var traineeDoctors = doctorsList.stream().map(d ->
        convert(d)).collect(toList());

    return TraineeSummaryDto.builder()
        .traineeInfo(traineeDoctors)
        .countTotal(getCountAll())
        .countUnderNotice(getCountUnderNotice())
        .totalPages(paginatedDoctors.getTotalPages())
        .totalResults(paginatedDoctors.getTotalElements())
        .build();
  }

  public void updateTrainee(final DoctorsForDbDto gmcDoctor) {
    final var doctorsForDB = DoctorsForDB.convert(gmcDoctor);
    final var doctor = doctorsRepository.findById(gmcDoctor.getGmcReferenceNumber());
    if (doctor.isPresent() ) {
      doctorsForDB.setAdmin(doctor.get().getAdmin());
      if(gmcDoctor.getUnderNotice().equals(NO.value())) {
        doctorsForDB.setDoctorStatus(RecommendationStatus.COMPLETED);
      }
      else {
        doctorsForDB.setDoctorStatus(
          recommendationService.getRecommendationStatusForTrainee(gmcDoctor.getGmcReferenceNumber())
        );
      }
    }
    else {
      doctorsForDB.setDoctorStatus(RecommendationStatus.NOT_STARTED);
    }
    doctorsRepository.save(doctorsForDB);
  }

  public void updateTraineeAdmin(final List<TraineeAdminDto> traineeAdmins) {
    traineeAdmins.stream().forEach(traineeAdmin -> {
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
    final var designatedBodyCode =
        doctorsForDB.isPresent() ? doctorsForDB.get().getDesignatedBodyCode() : null;
    return DesignatedBodyDto.builder().designatedBodyCode(designatedBodyCode).build();
  }

  public void removeDesignatedBodyCode(final ConnectionMessageDto message) {
    final var doctorsForDBOptional = doctorsRepository.findById(message.getGmcId());
    if (doctorsForDBOptional.isPresent()) {
      log.info("Updating designated body code from doctors for DB");
      final var doctorsForDB = doctorsForDBOptional.get();
      doctorsForDB.setDesignatedBodyCode(message.getDesignatedBodyCode());
      doctorsRepository.save(doctorsForDB);
    } else {
      log.info("No doctor found to update designated body code");
    }
  }

  public TraineeSummaryDto getDoctorsByGmcIds(final List<String> gmcIds) {
    final Iterable<DoctorsForDB> doctorsForDb = doctorsRepository.findAllById(gmcIds);
    final var doctorsForDBS = IterableUtils.toList(doctorsForDb);
    final var traineeInfoDtos = doctorsForDBS.stream().map(d -> convert(d)).collect(toList());
    return TraineeSummaryDto.builder().countTotal(traineeInfoDtos.size())
        .totalResults(traineeInfoDtos.size()).traineeInfo(traineeInfoDtos).build();
  }

  public void hideAllDoctors() {
    List<DoctorsForDB> doctors = doctorsRepository.findAll();
    doctors.stream().forEach(doctor -> {
      doctor.setDesignatedBodyCode(
          getHiddenDesignatedBodyCode(doctor.getDesignatedBodyCode())
      );
      doctorsRepository.save(doctor);
    });
  }

  private TraineeInfoDto convert(final DoctorsForDB doctorsForDB) {
    final var traineeInfoDTOBuilder = TraineeInfoDto.builder()
        .gmcReferenceNumber(doctorsForDB.getGmcReferenceNumber())
        .doctorFirstName(doctorsForDB.getDoctorFirstName())
        .doctorLastName(doctorsForDB.getDoctorLastName())
        .submissionDate(doctorsForDB.getSubmissionDate())
        .designatedBody(doctorsForDB.getDesignatedBodyCode())
        .dateAdded(doctorsForDB.getDateAdded())
        .underNotice(doctorsForDB.getUnderNotice().name())
        .sanction(doctorsForDB.getSanction())
        .doctorStatus(doctorsForDB.getDoctorStatus().name()) //TODO update with legacy statuses
        .lastUpdatedDate(doctorsForDB.getLastUpdatedDate())
        .admin(doctorsForDB.getAdmin())
        .connectionStatus(getConnectionStatus(doctorsForDB.getDesignatedBodyCode()));

    return traineeInfoDTOBuilder.build();

  }

  private Page<DoctorsForDB> getSortedAndFilteredDoctorsByPageNumber(
      final TraineeRequestDto requestDTO, final List<String> hiddenGmcIds) {
    final var hiddenGmcIdsNotNull = (hiddenGmcIds == null) ? new ArrayList<String>() : hiddenGmcIds;
    final var direction = "asc".equalsIgnoreCase(requestDTO.getSortOrder()) ? ASC : DESC;
    final var pageableAndSortable = of(requestDTO.getPageNumber(), pageSize,
        by(direction, requestDTO.getSortColumn()));
    if (requestDTO.isUnderNotice()) {
      return doctorsRepository
          .findByUnderNotice(pageableAndSortable, requestDTO.getSearchQuery(), requestDTO.getDbcs(),
              YES);
    }

    return doctorsRepository
        .findAll(pageableAndSortable, requestDTO.getSearchQuery(), requestDTO.getDbcs(),
            hiddenGmcIdsNotNull);
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

  private String getHiddenDesignatedBodyCode(String dbCode) {
    if(dbCode != null && !dbCode.startsWith(hiddenPrefix)){
      return hiddenPrefix + dbCode;
    } else {
      return dbCode;
    }
  }


}
