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

import static java.util.stream.Collectors.toList;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.service.RecommendationService;
import uk.nhs.hee.tis.revalidation.validator.TraineeRecommendationRecordDTOValidator;

@Slf4j
@RestController
@Api("/api/recommendation")
@RequestMapping("/api/recommendation")
public class RecommendationController {

  @Autowired
  private RecommendationService service;

  @Autowired
  private TraineeRecommendationRecordDTOValidator traineeRecommendationRecordDTOValidator;

  @ApiOperation(value = "Get recommendation details of a trainee", notes = "It will return trainee's recommendation details", response = TraineeInfoDto.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainee recommendation details", response = TraineeInfoDto.class)})
  @GetMapping("/{gmcId}")
  public ResponseEntity<TraineeRecommendationDto> getRecommendation(
      @PathVariable("gmcId") final String gmcId) {
    log.info("Receive request to fetch recommendations for GmcId: {}", gmcId);
    if (Objects.nonNull(gmcId)) {
      final var recommendationDTO = service.getTraineeInfo(gmcId);
      return ResponseEntity.ok().body(recommendationDTO);
    }

    return new ResponseEntity<>(TraineeRecommendationDto.builder().build(), HttpStatus.OK);
  }

  @ApiOperation(value = "Get recommendation details of a list of trainees", notes = "It will return trainees' recommendation details", response = Map.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Trainees' recommendation details", response = Map.class)})
  @GetMapping("/latest/{gmcIds}")
  public ResponseEntity<Map<String, TraineeRecommendationRecordDto>> getRecommendations(
      @PathVariable("gmcIds") final List<String> gmcIds) {
    log.info("Receive request to fetch recommendations for GmcIds: {}", gmcIds);
    final var recommendations = service.getLatestRecommendations(gmcIds);
    return new ResponseEntity<>(recommendations, HttpStatus.OK);
  }

  @ApiOperation(value = "Save new recommendation", notes = "It will allow user to save new recommendation", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "New recommendation is saved", response = ResponseEntity.class)})
  @PostMapping
  public ResponseEntity saveRecommendation(
      @RequestBody final TraineeRecommendationRecordDto traineeRecommendationDTO,
      final BindingResult bindingResult) {

    log.info("recommendation: {}", traineeRecommendationDTO);
    traineeRecommendationRecordDTOValidator.validate(traineeRecommendationDTO, bindingResult);
    if (bindingResult.hasErrors()) {
      return buildErrorResponse(bindingResult);
    }
    service.saveRecommendation(traineeRecommendationDTO);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @ApiOperation(value = "update recommendation", notes = "It will allow user to update recommendation", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "Recommendation is updated", response = ResponseEntity.class)})
  @PutMapping
  public ResponseEntity updateRecommendation(
      @RequestBody final TraineeRecommendationRecordDto traineeRecommendationDTO,
      final BindingResult bindingResult) {

    log.info("recommendation: {}", traineeRecommendationDTO);
    if (!StringUtils.hasLength(traineeRecommendationDTO.getRecommendationId())) {
      return new ResponseEntity<>("Recommendation Id should not be empty", HttpStatus.BAD_REQUEST);
    }

    traineeRecommendationRecordDTOValidator.validate(traineeRecommendationDTO, bindingResult);
    if (bindingResult.hasErrors()) {
      return buildErrorResponse(bindingResult);
    }
    service.updateRecommendation(traineeRecommendationDTO);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @ApiOperation(value = "Submit recommendation to gmc", notes = "It will allow user to submit recommendation to gmc", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "New recommendation is submitted to gmc", response = ResponseEntity.class)})
  @PostMapping("/{gmcId}/submit/{recommendationId}")
  public ResponseEntity<Void> submitRecommendation(@RequestBody RoUserProfileDto userProfileDto,
      @PathVariable("gmcId") String gmcNumber,
      @PathVariable("recommendationId") String recommendationId) {

    log.info("Revalidation officer profile: {}", userProfileDto);
    service.submitRecommendation(recommendationId, gmcNumber, userProfileDto);
    return new ResponseEntity<>(HttpStatus.OK);
  }


  private ResponseEntity<List<String>> buildErrorResponse(final BindingResult bindingResult) {
    final var errors = bindingResult.getAllErrors().stream().map(e -> e.getDefaultMessage())
        .collect(toList());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
  }
}
