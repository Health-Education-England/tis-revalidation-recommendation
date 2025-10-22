package uk.nhs.hee.tis.revalidation.service;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.nhs.hee.tis.revalidation.messages.publisher.GmcSyncMessagePublisher;

@Slf4j
@Service
public class GmcDoctorNightlySyncService {
  private static final String START_MESSAGE = "start";

  private final GmcSyncMessagePublisher gmcSyncMessagePublisher;

  public GmcDoctorNightlySyncService(
      GmcSyncMessagePublisher gmcSyncMessagePublisher
  ) {
    this.gmcSyncMessagePublisher = gmcSyncMessagePublisher;
  }

  @Scheduled(cron = "${app.gmc.nightlySyncStart.cronExpression}")
  @SchedulerLock(name = "GmcNightlySyncJob")
  public void startNightlyGmcDoctorSync() {
    gmcSyncMessagePublisher.publishToBroker(START_MESSAGE);
    log.info("Start message has been sent to start gmc sync");
  }

}
