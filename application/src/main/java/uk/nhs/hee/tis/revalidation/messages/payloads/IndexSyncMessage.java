package uk.nhs.hee.tis.revalidation.messages.payloads;

import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ApiModel(description = "Message wrapper for index sync messages")
public class IndexSyncMessage<T> {
  private T payload;
  private Boolean syncEnd;
}
