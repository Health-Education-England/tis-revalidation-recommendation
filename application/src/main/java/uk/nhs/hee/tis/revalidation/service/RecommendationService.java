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

import uk.nhs.hee.tis.revalidation.dto.RecommendationStatusCheckDto;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.Recommendation;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;

import java.util.List;
import java.util.Map;

public interface RecommendationService {
    //get trainee information with current and legacy recommendations
    TraineeRecommendationDto getTraineeInfo(String gmcId);

    //save a new recommendation
    Recommendation saveRecommendation(TraineeRecommendationRecordDto recordDTO);

    //update an existing recommendation
    Recommendation updateRecommendation(TraineeRecommendationRecordDto recordDTO);

    //submit a recommendation to gmc
    boolean submitRecommendation(String recommendationId, String gmcNumber,
                                         RoUserProfileDto userProfileDto);

    //get latest recommendations of a trainee
    TraineeRecommendationRecordDto getLatestRecommendation(String gmcId);

    //get latest recommendations of a list of trainees
    Map<String, TraineeRecommendationRecordDto> getLatestRecommendations(
            List<String> gmcIds);

    RecommendationStatus getRecommendationStatusForTrainee(String gmcId);

    // get a list of recommendation status Dto
    List<RecommendationStatusCheckDto> getRecommendationStatusCheckDtos();

    boolean checkIfPastCompletedRecommendation(Recommendation recommendation,
        DoctorsForDB doctor);
}
