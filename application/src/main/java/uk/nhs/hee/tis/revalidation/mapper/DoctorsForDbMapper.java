/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
package uk.nhs.hee.tis.revalidation.mapper;

import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;

@Mapper(componentModel = "spring")
public interface DoctorsForDbMapper {

  @Mapping(source = "designatedBodyCode", target = "connectionStatus", qualifiedByName = "desgnatedBodyToConnectionStatus")
  @Mapping(source = "designatedBodyCode", target = "designatedBody")
  TraineeInfoDto toTraineeInfoDto(DoctorsForDB doctor);

  @Named("desgnatedBodyToConnectionStatus")
  default String designatedBodyToConnectionStatus(String designatedBody) {
    return StringUtils.hasLength(designatedBody) ? "Yes" : "No";
  }

  @Mapping(target = "submissionDate", expression = "java("
      + "uk.nhs.hee.tis.revalidation.util.DateUtil"
      + ".convertGmcDateToLocalDate(dto.getSubmissionDate()))")
  @Mapping(target = "dateAdded", expression = "java("
      + "uk.nhs.hee.tis.revalidation.util.DateUtil.convertGmcDateToLocalDate(dto.getDateAdded()))")
  @Mapping(target = "underNotice", expression = "java("
      + "uk.nhs.hee.tis.revalidation.entity.UnderNotice.fromString(dto.getUnderNotice()))")
  @Mapping(target = "lastUpdatedDate", expression = "java(java.time.LocalDate.now())")
  @Mapping(target = "gmcLastUpdatedDateTime", source = "gmcLastUpdatedDateTime",
      qualifiedByName = "localDateTimeFromString")
  @Mapping(target = "existsInGmc", constant = "true")
  @Mapping(target = "doctorStatus", constant = "NOT_STARTED")
  DoctorsForDB toEntity(DoctorsForDbDto dto);

  @Named("localDateTimeFromString")
  default LocalDateTime localDateTimeFromString(String gmcLastUpdatedDateTime) {
    if (StringUtils.hasLength(gmcLastUpdatedDateTime)) {
      return LocalDateTime.parse(gmcLastUpdatedDateTime);
    } else {
      return null;
    }
  }
}
