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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
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
import uk.nhs.hee.tis.revalidation.service.RecommendationElasticSearchService;

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
  protected static final String AUTOCOMPLETE_FIELD = "fieldName";
  protected static final String INPUT = "input";

  @Value("${app.validation.sort.fields}")
  private List<String> sortFields;

  @Value("${app.validation.sort.order}")
  private List<String> sortOrder;

  @Value("${app.gmc.designatedBodies}")
  private List<String> designatedBodies;

  private DoctorsForDBService doctorsForDBService;

  private RecommendationElasticSearchService recommendationElasticSearchService;

  public DoctorsForDBController(
      DoctorsForDBService doctorsForDBService,
      RecommendationElasticSearchService recommendationElasticSearchService) {
    this.doctorsForDBService = doctorsForDBService;
    this.recommendationElasticSearchService = recommendationElasticSearchService;
  }

  @ApiOperation(value = "All trainee doctors information", notes = "It will return all the information about trainee doctors", response = TraineeSummaryDto.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee gmc all doctors data", response = TraineeSummaryDto.class)})
  @GetMapping
  public ResponseEntity<TraineeSummaryDto> getTraineeDoctorsInformation(
      @RequestParam(name = SORT_COLUMN, defaultValue = SUBMISSION_DATE, required = false) final String sortColumn,
      @RequestParam(name = SORT_ORDER, defaultValue = ASC, required = false) final String sortOrder,
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
  public ResponseEntity<Void> updateAdmin(@RequestBody final TraineeAdminUpdateDto traineeAdmins) {

    doctorsForDBService.updateTraineeAdmin(traineeAdmins.getTraineeAdmins());
    return ResponseEntity.ok().build();
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

  @ApiOperation(value = "Get autocomplete value for field", notes = "It will return matching field values", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Suggested field values", response = ResponseEntity.class)})
  @GetMapping(value = {"/autocomplete"})
  public ResponseEntity<List<String>> getAutocompleteProgrammeName(
      @NonNull @RequestParam(name = AUTOCOMPLETE_FIELD) final String fieldName,
      @RequestParam(name = INPUT) final String input,
      @RequestParam(name = DESIGNATED_BODY_CODES) final List<String> dbcs
  ) {
    log.info("Receive request to get autocomplete value for programme name: {}", fieldName);
    return ResponseEntity.ok().body(recommendationElasticSearchService.getAutocompleteResults(fieldName, input, dbcs));
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
