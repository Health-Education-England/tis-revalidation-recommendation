package uk.nhs.hee.tis.revalidation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.service.GmcDoctorNightlySyncService;

@Slf4j
@RestController
@RequestMapping("/api/admin")
public class AdminController {

  private final GmcDoctorNightlySyncService gmcDoctorNightlySyncService;

  public AdminController(GmcDoctorNightlySyncService gmcDoctorNightlySyncService) {
    this.gmcDoctorNightlySyncService = gmcDoctorNightlySyncService;
  }

  @PostMapping("/trigger-doctor-sync")
  public ResponseEntity<String> startSync() {
    log.info("Calling admin controller");
    gmcDoctorNightlySyncService.startNightlyGmcDoctorSync();
    return ResponseEntity.ok().body("Successful");
  }
}