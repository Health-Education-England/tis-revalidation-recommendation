package uk.nhs.hee.tis.revalidation.dto;

import java.time.LocalDateTime;

//It will be eventually record type not class
public class DoctorsForDbCollectedEvent {

  private String gmcId;
  private String designatedBodyCode;
  private LocalDateTime requestDateTime;


  public DoctorsForDbCollectedEvent(String gmcId, String designatedBodyCode,
      LocalDateTime requestDateTime) {
    this.gmcId = gmcId;
    this.designatedBodyCode = designatedBodyCode;
    this.requestDateTime = requestDateTime;
  }

  public String getGmcId() {
    return gmcId;
  }

  public void setGmcId(String gmcId) {
    this.gmcId = gmcId;
  }

  public String getDesignatedBodyCode() {
    return designatedBodyCode;
  }

  public void setDesignatedBodyCode(String designatedBodyCode) {
    this.designatedBodyCode = designatedBodyCode;
  }

  public LocalDateTime getRequestDateTime() {
    return requestDateTime;
  }

  public void setRequestDateTime(LocalDateTime requestDateTime) {
    this.requestDateTime = requestDateTime;
  }
}
