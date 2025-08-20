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

import java.time.LocalDate;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.springframework.util.StringUtils;
import uk.nhs.hee.tis.revalidation.dto.ConnectionMessageDto;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;

@Mapper(componentModel = "spring")
public interface DoctorsForDbMapper {

  @Mapping(source = "designatedBodyCode", target = "connectionStatus",
      qualifiedByName = "desgnatedBodyToConnectionStatus")
  @Mapping(source = "designatedBodyCode", target = "designatedBody")
  TraineeInfoDto toTraineeInfoDto(DoctorsForDB doctor);

  @Named("desgnatedBodyToConnectionStatus")
  default String designatedBodyToConnectionStatus(String designatedBody) {
    return StringUtils.hasLength(designatedBody) ? "Yes" : "No";
  }

  DoctorsForDbDto toDto(DoctorsForDB d);

  @Mapping(target = "underNotice", expression = "java("
      + "uk.nhs.hee.tis.revalidation.entity.UnderNotice.fromString(dto.getUnderNotice()))")
  @Mapping(target = "dateAdded", dateFormat = "dd/MM/yyyy")
  @Mapping(target = "submissionDate", dateFormat = "dd/MM/yyyy")
  @Mapping(target = "lastUpdatedDate", ignore = true)
  DoctorsForDB toEntity(DoctorsForDbDto dto, boolean existsInGmc,
      RecommendationStatus doctorStatus);

  @Mapping(source = "gmcId", target = "gmcReferenceNumber")
  DoctorsForDB updateEntity(ConnectionMessageDto dto, @MappingTarget DoctorsForDB target);

  @AfterMapping
  default void supplementFields(@MappingTarget DoctorsForDB entity) {
    if (entity.getDateAdded() == null) {
      entity.setDateAdded(LocalDate.now());
    }
  }
}
