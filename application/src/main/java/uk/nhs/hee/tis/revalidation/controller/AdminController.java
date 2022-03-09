package uk.nhs.hee.tis.revalidation.controller;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.messages.RabbitMessageListener;
import uk.nhs.hee.tis.revalidation.service.GmcDoctorNightlySyncService;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final GmcDoctorNightlySyncService gmcDoctorNightlySyncService;

  private final RabbitMessageListener rabbitMessageListener;

  public AdminController(GmcDoctorNightlySyncService gmcDoctorNightlySyncService,
      RabbitMessageListener rabbitMessageListener) {
    this.gmcDoctorNightlySyncService = gmcDoctorNightlySyncService;
    this.rabbitMessageListener = rabbitMessageListener;
  }

  @PostMapping("/trigger-doctor-sync")
  public ResponseEntity<String> startSync() {
    log.info("Calling admin controller");
    gmcDoctorNightlySyncService.startNightlyGmcDoctorSync();
    return ResponseEntity.ok().body("Successful");
  }

  @GetMapping("/es-rebuild")
  public ResponseEntity<Void> esRebuildGetMaster() throws IOException {
    rabbitMessageListener.receiveMessageGetMaster("getMaster");
    return ResponseEntity.ok().build();
  }
}