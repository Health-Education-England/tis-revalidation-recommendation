package uk.nhs.hee.tis.revalidation.controller;

import static uk.nhs.hee.tis.revalidation.entity.RecommendationGmcOutcome.APPROVED;

import io.awspring.cloud.messaging.core.QueueMessagingTemplate;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;

@Slf4j
@RestController
@Api("/api/test")
@RequestMapping("/api/test")
public class TestController {

  @Value("${cloud.aws.end-point.uri}")
  private String sqsEndPoint;
  private final QueueMessagingTemplate queueMessagingTemplate;

  public TestController(QueueMessagingTemplate queueMessagingTemplate) {
    this.queueMessagingTemplate = queueMessagingTemplate;
  }

  @ApiOperation(value = "test", notes = "test", response = ResponseEntity.class)
  @ApiResponses(value = {
      @ApiResponse(code = 200, message = "test", response = ResponseEntity.class)})
  @GetMapping("/sqs")
  public ResponseEntity<String> sendTestSqsMessage() {
    DoctorsForDB doctorsForDB = DoctorsForDB.builder()
        .gmcReferenceNumber("101")
        .doctorFirstName("AAA")
        .doctorLastName("BBB")
        .submissionDate(LocalDate.now())
        .dateAdded(LocalDate.now())
        .underNotice(UnderNotice.NO)
        .sanction("sanc")
        .doctorStatus(RecommendationStatus.NOT_STARTED)
        .lastUpdatedDate(LocalDate.now())
        .designatedBodyCode("PQR")
        .admin("Reval Admin")
        .existsInGmc(true).build();

    final var summary = RevalidationSummaryDto.builder()
        .doctor(doctorsForDB)
        .gmcOutcome(APPROVED.getOutcome())
        .build();
    final var message = IndexSyncMessage.builder()
        .payload(summary)
        .syncEnd(false)
        .build();
    queueMessagingTemplate.convertAndSend(sqsEndPoint, message);

    return ResponseEntity.ok()
        .body("message published");
  }

}
