package uk.nhs.hee.tis.revalidation.service;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.messages.publisher.GmcDoctorsForDbSyncStartPublisher;

@Slf4j
@Service
public class GmcDoctorNightlySyncService {

  private GmcDoctorsForDbSyncStartPublisher gmcDoctorsForDbSyncStartPublisher;

  private DoctorsForDBService doctorsForDBService;

  public GmcDoctorNightlySyncService(
      DoctorsForDBService doctorsForDBService,
      GmcDoctorsForDbSyncStartPublisher gmcDoctorsForDbSyncStartPublisher
  ) {
    this.gmcDoctorsForDbSyncStartPublisher = gmcDoctorsForDbSyncStartPublisher;
    this.doctorsForDBService = doctorsForDBService;
  }

  @Scheduled(cron="${app.gmc.nightlySyncStart.cronExpression}")
  @SchedulerLock(name = "GmcNightlySyncJob")
  public void startNightlyGmcDoctorSync() {
    this.doctorsForDBService.hideAllDoctors();
    log.info("All doctors are hidden now");
    this.gmcDoctorsForDbSyncStartPublisher.publishNightlySyncStartMessage();
    log.info("Start message has been sent to start gmc sync");
  }

}
