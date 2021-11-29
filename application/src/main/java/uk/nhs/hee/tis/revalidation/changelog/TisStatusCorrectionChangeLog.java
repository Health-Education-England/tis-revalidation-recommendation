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

package uk.nhs.hee.tis.revalidation.changelog;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;

import java.time.LocalDate;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;
import uk.nhs.hee.tis.revalidation.service.RecommendationService;
import uk.nhs.hee.tis.revalidation.util.DateUtil;

@ChangeLog(order = "002")
@Slf4j
public class TisStatusCorrectionChangeLog {

    @ChangeSet(order = "001", id = "insertCorrectTisStatuses", author = "")
    public void correctTisStatuses(
            DoctorsForDBRepository doctorsForDBRepository,
            RecommendationService recommendationService
    ) {
       DateUtil.convertDateInGmcFormat(LocalDate.now());
        List<DoctorsForDB> doctors = doctorsForDBRepository.findAll();
        doctors.forEach(doctor -> {
            doctor.setDoctorStatus(
                recommendationService.getRecommendationStatusForTrainee(doctor.getGmcReferenceNumber())
            );
            doctorsForDBRepository.save(doctor);
        });
    }
}