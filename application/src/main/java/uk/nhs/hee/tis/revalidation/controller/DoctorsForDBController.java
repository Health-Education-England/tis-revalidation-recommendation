package uk.nhs.hee.tis.revalidation.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.dto.DesignatedBodyDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeAdminUpdateDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRequestDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeSummaryDto;
import uk.nhs.hee.tis.revalidation.service.DoctorsForDBService;

@Slf4j
@RestController
@Api("/api/v1/doctors")
@RequestMapping("/api/v1/doctors")
public class DoctorsForDBController {

  protected static final String SORT_COLUMN = "sortColumn";
  protected static final String SORT_ORDER = "sortOrder";
  protected static final String SUBMISSION_DATE = "submissionDate";
  protected static final String DESC = "desc";
  protected static final String ASC = "asc";
  protected static final String UNDER_NOTICE = "underNotice";
  protected static final String UNDER_NOTICE_VALUE = "false";
  protected static final String PAGE_NUMBER = "pageNumber";
  protected static final String PAGE_NUMBER_VALUE = "0";
  protected static final String SEARCH_QUERY = "searchQuery";
  protected static final String EMPTY_STRING = "";
  protected static final String DESIGNATED_BODY_CODES = "dbcs";

  @Value("${app.validation.sort.fields}")
  private List<String> sortFields;

  @Value("${app.validation.sort.order}")
  private List<String> sortOrder;

  @Value("${app.gmc.designatedBodies}")
  private List<String> designatedBodies;

  @Autowired
  private DoctorsForDBService doctorsForDBService;

  @ApiOperation(value = "All trainee doctors information", notes = "It will return all the information about trainee doctors", response = TraineeSummaryDto.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee gmc all doctors data", response = TraineeSummaryDto.class)})
  @GetMapping
  public ResponseEntity<TraineeSummaryDto> getTraineeDoctorsInformation(
      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = DESC, required = false) final String sortOrder,
      @RequestParam(name = UNDER_NOTICE, defaultValue = UNDER_NOTICE_VALUE, required = false) final boolean underNotice,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE, required = false) final int pageNumber,
      @RequestParam(name = DESIGNATED_BODY_CODES, required = false) final List<String> dbcs,
      @RequestParam(name = SEARCH_QUERY, defaultValue = EMPTY_STRING, required = false) final String searchQuery) {
    final var traineeRequestDTO = TraineeRequestDto.builder()
        .sortColumn(sortColumn)
        .sortOrder(sortOrder)
        .underNotice(underNotice)
        .pageNumber(pageNumber)
        .dbcs(dbcs)
        .searchQuery(searchQuery)
        .build();

    validate(traineeRequestDTO);

    final var allTraineeDoctorDetails = doctorsForDBService
        .getAllTraineeDoctorDetails(traineeRequestDTO, List.of());
    return ResponseEntity.ok().body(allTraineeDoctorDetails);
  }

  @ApiOperation(value = "All trainee doctors information which is not in the gmcId list", notes = "It will return all the information about trainee doctors who are not in the gmcId list ", response = TraineeSummaryDto.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee gmc doctors data", response = TraineeSummaryDto.class)})
  @GetMapping(value = {"/unhidden/{gmcIds}", "/unhidden/"})
  public ResponseEntity<TraineeSummaryDto> getTraineeDoctorsInformationHideGmcIds(
      @PathVariable(required = false) final List<String> gmcIds,
      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = DESC, required = false) final String sortOrder,
      @RequestParam(name = UNDER_NOTICE, defaultValue = UNDER_NOTICE_VALUE, required = false) final boolean underNotice,
      @RequestParam(name = PAGE_NUMBER, defaultValue = PAGE_NUMBER_VALUE, required = false) final int pageNumber,
      @RequestParam(name = DESIGNATED_BODY_CODES, required = false) final List<String> dbcs,
      @RequestParam(name = SEARCH_QUERY, defaultValue = EMPTY_STRING, required = false) final String searchQuery) {
    final var traineeRequestDTO = TraineeRequestDto.builder()
        .sortColumn(sortColumn)
        .sortOrder(sortOrder)
        .underNotice(underNotice)
        .pageNumber(pageNumber)
        .dbcs(dbcs)
        .searchQuery(searchQuery)
        .build();

    validate(traineeRequestDTO);

    final var allTraineeDoctorDetails = doctorsForDBService
        .getAllTraineeDoctorDetails(traineeRequestDTO, gmcIds);
    return ResponseEntity.ok().body(allTraineeDoctorDetails);
  }

  @ApiOperation(value = "Update admin for trainee", notes = "It will update admin to recommend trainee", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee's admin has been updated", response = ResponseEntity.class)})
  @PostMapping("/assign-admin")
  public ResponseEntity<String> updateAdmin(@RequestBody final TraineeAdminUpdateDto traineeAdmins) {

    doctorsForDBService.updateTraineeAdmin(traineeAdmins.getTraineeAdmins());
    return ResponseEntity.ok("success");
  }

  @ApiOperation(value = "Get doctor DB Code", notes = "It will return doctor db code", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Doctor's designated body code", response = ResponseEntity.class)})
  @GetMapping("/designated-body/{gmcId}")
  public ResponseEntity<DesignatedBodyDto> getDesignatedBodyCode(
      @PathVariable("gmcId") final String gmcId) {
    log.info("Receive request to get designatedBodyCode for user: {}", gmcId);
    final var designatedBody = doctorsForDBService
        .getDesignatedBodyCode(gmcId);
    return ResponseEntity.ok().body(designatedBody);
  }

  @ApiOperation(value = "Get doctors by Gmc Ids", notes = "It will return doctors", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Doctor's by gmcIds", response = ResponseEntity.class)})
  @GetMapping(value = {"/gmcIds", "/gmcIds/{gmcIds}"})
  public ResponseEntity<TraineeSummaryDto> getDoctors(
      @PathVariable(required = false) final List<String> gmcIds) {
    log.info("Receive request to get designatedBodyCode for user: {}", gmcIds);
    if (Objects.nonNull(gmcIds)) {
      final var doctors = doctorsForDBService
          .getDoctorsByGmcIds(gmcIds);
      return ResponseEntity.ok().body(doctors);
    }
    return ResponseEntity.ok().body(TraineeSummaryDto.builder().build());
  }

  //TODO: find a better way like separate validator
  private void validate(final TraineeRequestDto requestDTO) {
    if (!sortFields.contains(requestDTO.getSortColumn())) {
      log.warn("Invalid sort column name provided: {}, revert to default column: {}",
          requestDTO.getSortColumn(), SUBMISSION_DATE);
      requestDTO.setSortColumn(SUBMISSION_DATE);
    }

    if (!sortOrder.contains(requestDTO.getSortOrder())) {
      log.warn("Invalid sort order provided: {}, revert to default order: {}",
          requestDTO.getSortOrder(), DESC);
      requestDTO.setSortOrder(DESC);
    }

    if (requestDTO.getDbcs() == null || requestDTO.getDbcs().isEmpty()) {
      log.warn("Designated body code should not be empty.");
      requestDTO.setDbcs(designatedBodies);
    }
  }
}
