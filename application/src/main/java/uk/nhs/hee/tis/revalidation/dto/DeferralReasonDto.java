package uk.nhs.hee.tis.revalidation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(description = "List of deferral reasons and sub reasons")
public class DeferralReasonDto {

    private String code;
    private String reason;
    private List<DeferralReasonDto> subReasons;
}