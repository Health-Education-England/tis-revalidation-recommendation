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
import static org.hamcrest.Matchers.is;

import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator.OfInt;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGeneratorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.event.DoctorsForDbCollectedEvent;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@DataMongoTest
class ConcurrentDisconnectsIntTest {

  @Autowired
  DoctorsForDBRepository repository;
  private DoctorsForDbCollectedEvent db1Event;


  private DoctorsForDbCollectedEvent db2Event;
  private List<DoctorsForDB> db1Doctors;
  private List<DoctorsForDB> db2Doctors;
  private List<DoctorsForDB> unconnectedDoctors;
  private final Faker faker = Faker.instance();
  AtomicInteger gmcNumberGenerator = new AtomicInteger(100000);

  DoctorsForDBService testObj;

  @Test
  void shouldRemoveDesignatedBodyFromDoctorsSafely()
      throws ExecutionException, InterruptedException {
    Future<Void> db1future = new FutureTask<>(
        () -> testObj.handleDoctorsForDbCollectedEvent(db1Event), null);
    Future<Void> db2future = new FutureTask<>(
        () -> {
          try {
            // Sleep to introduce some overlap
            Thread.sleep(500);
          } catch (InterruptedException ignored) {
          }
          testObj.handleDoctorsForDbCollectedEvent(db2Event);
        }, null);

    db1future.get();
    db2future.get();
    //Assert the doctors are in the expected designated bodies
    db1Doctors.forEach(d -> assertThat(d + " did not have the expected designated body",
        repository.findById(d.getGmcReferenceNumber()).get().getDesignatedBodyCode(),
        is(d.getDesignatedBodyCode())));
    db2Doctors.forEach(d -> assertThat(d + " did not have the expected designated body",
        repository.findById(d.getGmcReferenceNumber()).get().getDesignatedBodyCode(),
        is(d.getDesignatedBodyCode())));
    unconnectedDoctors.forEach(d -> assertThat(d + " did not have the expected designated body",
        repository.findById(d.getGmcReferenceNumber()).get().getDesignatedBodyCode(),
        is(d.getDesignatedBodyCode())));

  }

  @BeforeEach
  void setData() {
    LocalDateTime setupDateTime = LocalDateTime.now();
    testObj = new DoctorsForDBService(repository, null, null, null, null, null);
    db1Doctors = new ArrayList<>();
    db2Doctors = new ArrayList<>();
    unconnectedDoctors = new ArrayList<>();
    db1Event = new DoctorsForDbCollectedEvent("Won", setupDateTime, db1Doctors);
    db2Event = new DoctorsForDbCollectedEvent("Too", setupDateTime, db2Doctors);

    //TODO: Refactor
    //Add a bunch of docs to designated bodies
    //Save them
    //Shuffle them
    //Call the handler
    repository.deleteAll();
    OfInt random = RandomGeneratorFactory.getDefault().create()
        .ints(1, 20000).iterator();

    for (int i = 0; i < 1000; i++) {
      DoctorsForDB d = DoctorsForDB.builder().designatedBodyCode("Won").doctorFirstName("Won")
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      db1Doctors.add(d);
      d = DoctorsForDB.builder().designatedBodyCode("Won").doctorFirstName("Two")
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      d.setDesignatedBodyCode("Two");
      db2Doctors.add(d);
      d = DoctorsForDB.builder().designatedBodyCode("Won").doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      d.setDesignatedBodyCode(null);
      unconnectedDoctors.add(d);

      d = DoctorsForDB.builder().designatedBodyCode("Too").doctorFirstName("Won")
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      d.setDesignatedBodyCode("Won");
      db1Doctors.add(d);
      d = DoctorsForDB.builder().designatedBodyCode("Too").doctorFirstName("Two")
          .doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      db2Doctors.add(d);
      d = DoctorsForDB.builder().designatedBodyCode("Too").doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      d.setDesignatedBodyCode(null);
      unconnectedDoctors.add(d);

      d = DoctorsForDB.builder().doctorFirstName("Won").doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      d.setDesignatedBodyCode("Won");
      db1Doctors.add(d);
      d = DoctorsForDB.builder().doctorFirstName("Two").doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      d.setDesignatedBodyCode("Two");
      db2Doctors.add(d);
      d = DoctorsForDB.builder().doctorLastName(faker.name().lastName())
          .gmcReferenceNumber(Integer.toString(gmcNumberGenerator.getAndIncrement()))
          .gmcLastUpdatedDateTime(
              setupDateTime.minus(random.next(), ChronoUnit.MINUTES)).build();
      repository.save(d);
      unconnectedDoctors.add(d);
    }
    LocalDateTime updateTime = setupDateTime.plusSeconds(1);
    db1Doctors.forEach(d -> d.setGmcLastUpdatedDateTime(updateTime));
    db2Doctors.forEach(d -> d.setGmcLastUpdatedDateTime(updateTime));
  }
}
