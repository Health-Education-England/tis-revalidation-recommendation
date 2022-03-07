package uk.nhs.hee.tis.revalidation.dto;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Revalidation Summary For Listing Pages")
public class RevalidationSummaryDto {

  private DoctorsForDB doctor;
  private String gmcOutcome;
  @Nullable
  Boolean syncEnd;

}
