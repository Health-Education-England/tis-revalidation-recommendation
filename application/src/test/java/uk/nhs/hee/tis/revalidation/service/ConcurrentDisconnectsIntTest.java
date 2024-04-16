/*
 * The MIT License (MIT)
 *
 * Copyright 2024 Crown Copyright (Health Education England)
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
package uk.nhs.hee.tis.revalidation.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGeneratorFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.event.DoctorsForDbCollectedEvent;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@Slf4j
@Disabled("Used for verifying the concetpt so hasn't been configured for running as part of CI.")
@DataMongoTest
class ConcurrentDisconnectsIntTest {

  // Designated Body names are also used in the test data `doctorFirstName` field as a convenience.
  public static final String DB1_NAME = "Won";
  public static final String DB2_NAME = "Too";

  @Autowired
  DoctorsForDBRepository repository;
  private DoctorsForDbCollectedEvent db1Event;
  private DoctorsForDbCollectedEvent db2Event;

  private List<String> expectedDb1Doctors;
  private List<String> expectedDb2Doctors;
  private List<String> expectedNoDbDoctors;
  private final Faker faker = Faker.instance();
  AtomicInteger gmcNumberGenerator = new AtomicInteger(100000);

  DoctorsForDBService testObj;

  @Test
  void shouldRemoveDesignatedBodyFromDoctorsSafely()
      throws ExecutionException, InterruptedException {
    // Run 2 designated body updates in parallel
    log.info("Starting the test");
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
    Future<?> db1future = executor.submit(
        () -> testObj.handleDoctorsForDbCollectedEvent(db1Event), null);
    Future<?> db2future = executor.schedule(
        () -> testObj.handleDoctorsForDbCollectedEvent(db2Event), 500, TimeUnit.MILLISECONDS);

    // Block until tasks have finished
    db1future.get();
    log.info("Designated Body 1 completed.");
    db2future.get();
    log.info("Designated Body 2 completed.");

    List<String> actualGmcNumbers = repository.findByDesignatedBodyCode(DB1_NAME).stream()
        .map(DoctorsForDB::getGmcReferenceNumber).toList();
    assertThat(actualGmcNumbers, containsInAnyOrder(expectedDb1Doctors.toArray()));

    actualGmcNumbers = repository.findByDesignatedBodyCode(DB2_NAME).stream()
        .map(DoctorsForDB::getGmcReferenceNumber).toList();
    assertThat(actualGmcNumbers, containsInAnyOrder(expectedDb2Doctors.toArray()));

    actualGmcNumbers = repository.findByDesignatedBodyCode(null).stream()
        .map(DoctorsForDB::getGmcReferenceNumber).toList();
    assertThat(actualGmcNumbers, containsInAnyOrder(expectedNoDbDoctors.toArray()));

  }

  @BeforeEach
  void setData() {
    LocalDateTime setupDateTime = LocalDateTime.now();
    testObj = new DoctorsForDBService(repository, null, null, null, null, null);
    ArrayList<DoctorsForDB> db1Doctors = new ArrayList<>();
    ArrayList<DoctorsForDB> db2Doctors = new ArrayList<>();
    expectedNoDbDoctors = new ArrayList<>();

    repository.deleteAll();
    LocalDateTime staleRecordDateTime = setupDateTime.minus(
        RandomGeneratorFactory.getDefault().create()
            .nextInt(1, 20000), ChronoUnit.MINUTES);

    for (int i = 0; i < 100; i++) {
      DoctorsForDB d = DoctorsForDB.builder().designatedBodyCode(DB1_NAME).doctorFirstName(DB1_NAME)
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      db1Doctors.add(d);

      d = DoctorsForDB.builder().designatedBodyCode(DB1_NAME).doctorFirstName(DB2_NAME)
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      d.setDesignatedBodyCode(DB2_NAME);
      db2Doctors.add(d);

      d = DoctorsForDB.builder().designatedBodyCode(DB1_NAME)
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      d.setDesignatedBodyCode(null);
      expectedNoDbDoctors.add(d.getGmcReferenceNumber());

      d = DoctorsForDB.builder().designatedBodyCode(DB2_NAME).doctorFirstName(DB1_NAME)
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      d.setDesignatedBodyCode(DB1_NAME);
      db1Doctors.add(d);

      d = DoctorsForDB.builder().designatedBodyCode(DB2_NAME).doctorFirstName(DB2_NAME)
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      db2Doctors.add(d);

      d = DoctorsForDB.builder().designatedBodyCode(DB2_NAME)
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      d.setDesignatedBodyCode(null);
      expectedNoDbDoctors.add(d.getGmcReferenceNumber());

      d = DoctorsForDB.builder().doctorFirstName(DB1_NAME).doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      d.setDesignatedBodyCode(DB1_NAME);
      db1Doctors.add(d);

      d = DoctorsForDB.builder().doctorFirstName(DB2_NAME).doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      d.setDesignatedBodyCode(DB2_NAME);
      db2Doctors.add(d);

      d = DoctorsForDB.builder().doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(staleRecordDateTime).build();
      repository.save(d);
      expectedNoDbDoctors.add(d.getGmcReferenceNumber());
    }

    LocalDateTime updateTime = setupDateTime.plusSeconds(1);
    db1Doctors.forEach(d -> d.setGmcLastUpdatedDateTime(updateTime));
    db2Doctors.forEach(d -> d.setGmcLastUpdatedDateTime(updateTime));
    expectedDb1Doctors = db1Doctors.stream().map(DoctorsForDB::getGmcReferenceNumber).toList();
    expectedDb2Doctors = db2Doctors.stream().map(DoctorsForDB::getGmcReferenceNumber).toList();

    db1Event = new DoctorsForDbCollectedEvent(DB1_NAME, setupDateTime, db1Doctors);
    db2Event = new DoctorsForDbCollectedEvent(DB2_NAME, setupDateTime, db2Doctors);
  }
}
