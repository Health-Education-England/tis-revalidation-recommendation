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

package uk.nhs.hee.tis.revalidation.entity;


import static java.time.LocalDate.now;
import static uk.nhs.hee.tis.revalidation.entity.RecommendationStatus.NOT_STARTED;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.convertGmcDateToLocalDate;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import io.swagger.annotations.ApiModel;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.lang.Nullable;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "doctorsForDB")
@ApiModel(description = "Trainee doctors's core profile data")
public class DoctorsForDB {

  @Id
  private String gmcReferenceNumber;
  private String doctorFirstName;
  private String doctorLastName;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate submissionDate;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate dateAdded;
  private UnderNotice underNotice;
  private String sanction;
  private RecommendationStatus doctorStatus;
  @JsonDeserialize(using = LocalDateDeserializer.class)
  @JsonSerialize(using = LocalDateSerializer.class)
  private LocalDate lastUpdatedDate;
  private String designatedBodyCode;
  private String admin;
  private Boolean existsInGmc;

  public static final  DoctorsForDB convert(final DoctorsForDbDto doctorsForDBDTO) {
    return DoctorsForDB.builder()
        .gmcReferenceNumber(doctorsForDBDTO.getGmcReferenceNumber())
        .doctorFirstName(doctorsForDBDTO.getDoctorFirstName())
        .doctorLastName(doctorsForDBDTO.getDoctorLastName())
        .submissionDate(convertGmcDateToLocalDate(doctorsForDBDTO.getSubmissionDate()))
        .dateAdded(convertGmcDateToLocalDate(doctorsForDBDTO.getDateAdded()))
        .underNotice(UnderNotice.fromString(doctorsForDBDTO.getUnderNotice()))
        .sanction(doctorsForDBDTO.getSanction())
        .doctorStatus(NOT_STARTED)
        .designatedBodyCode(doctorsForDBDTO.getDesignatedBodyCode())
        .lastUpdatedDate(now())
        .existsInGmc(true)
        .build();
  }
}
