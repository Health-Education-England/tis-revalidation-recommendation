/*
 * The MIT License (MIT)
 *
 * Copyright 2023 Crown Copyright (Health Education England)
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
package uk.nhs.hee.tis.revalidation.mapper;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javafaker.Faker;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeInfoDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;

class DoctorsForDbMapperTest {

  public static final String CONNECTION_STATUS_YES = "Yes";
  private final Faker faker = new Faker();
  private final String dbc = faker.lorem().characters(6);
  private final String gmcNumber = faker.number().digits(7);
  private final String firstName = faker.name().firstName();
  private final String lastName = faker.name().lastName();
  private final LocalDate submissionDate = now().minusDays(1);
  private final LocalDate dateAdded = now().minusMonths(1);
  private final UnderNotice underNotice = UnderNotice.NO;
  private final String sanction = faker.lorem().characters(2);
  private final LocalDateTime gmcLastUpdatedDateTime = LocalDateTime.now();
  DoctorsForDbMapper testObj;

  @BeforeEach
  void setup() {
    testObj = new DoctorsForDbMapperImpl();
  }

  @Test
  void shouldReturnNullForNullInput() {
    TraineeInfoDto actual = testObj.toTraineeInfoDto(null);
    assertThat(actual, nullValue());
  }

  @Test
  void shouldReturnDtoForEmptyDoctor() {
    TraineeInfoDto actual = testObj.toTraineeInfoDto(new DoctorsForDB());
    // Result has a connectionStatus. This is defined by the absence of a designated body code.
    assertThat(actual, is(notNullValue()));
  }

  @Test
  void shouldReturnFullyPopulatedTraineeInfoForDoctor() {
    DoctorsForDB doctorsForDB = DoctorsForDB.builder()
        .doctorFirstName("first")
        .doctorLastName("last")
        .gmcReferenceNumber("gmtRef")
        .sanction("sanction")
        .underNotice(UnderNotice.ON_HOLD)
        .dateAdded(LocalDate.EPOCH)
        .submissionDate(LocalDate.MAX)
        .doctorStatus(RecommendationStatus.DRAFT)
        .admin("K")
        .lastUpdatedDate(LocalDate.of(2023, 8, 31))
        .designatedBodyCode("1-DS")
        .existsInGmc(true)
        .build();

    TraineeInfoDto expected = TraineeInfoDto.builder()
        .doctorFirstName("first")
        .doctorLastName("last")
        .gmcReferenceNumber("gmtRef")
        .sanction("sanction")
        .underNotice(UnderNotice.ON_HOLD.name())
        .dateAdded(LocalDate.EPOCH)
        .submissionDate(LocalDate.MAX)
        .doctorStatus(RecommendationStatus.DRAFT.name())
        .admin("K")
        .lastUpdatedDate(LocalDate.of(2023, 8, 31))
        .designatedBody("1-DS")
        .connectionStatus(CONNECTION_STATUS_YES)
        .build();
    TraineeInfoDto actual = testObj.toTraineeInfoDto(doctorsForDB);

    assertThat(actual, is(equalTo(expected)));
  }

  @ParameterizedTest()
  @NullAndEmptySource
  void shoudReturnNoFor(String dbc) {
    assertThat(testObj.designatedBodyToConnectionStatus(dbc), is("No"));
  }

  @Test
  void shouldReturnYesForString() {
    assertThat(testObj.designatedBodyToConnectionStatus("Oooh"), is(CONNECTION_STATUS_YES));
  }

  @Test
  void shouldMapDoctorsForDbDtoToEntity() {
    DoctorsForDbDto doctorsForDbDto = DoctorsForDbDto.builder()
        .doctorFirstName(firstName)
        .doctorLastName(lastName)
        .gmcReferenceNumber(gmcNumber)
        .sanction(sanction)
        .underNotice(underNotice.toString())
        .dateAdded(dateAdded.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .submissionDate(submissionDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        .gmcLastUpdatedDateTime(gmcLastUpdatedDateTime)
        .designatedBodyCode(dbc)
        .build();

    LocalDate now = LocalDate.now();
    DoctorsForDB doctorsForDb = testObj.toEntity(doctorsForDbDto, now, true,
        RecommendationStatus.NOT_STARTED);
    assertEquals(gmcNumber, doctorsForDb.getGmcReferenceNumber());
    assertEquals(firstName, doctorsForDb.getDoctorFirstName());
    assertEquals(lastName, doctorsForDb.getDoctorLastName());
    assertEquals(sanction, doctorsForDb.getSanction());
    assertEquals(dbc, doctorsForDb.getDesignatedBodyCode());
    assertEquals(submissionDate, doctorsForDb.getSubmissionDate());
    assertEquals(dateAdded, doctorsForDb.getDateAdded());
    assertEquals(underNotice, doctorsForDb.getUnderNotice());
    assertEquals(gmcLastUpdatedDateTime, doctorsForDb.getGmcLastUpdatedDateTime());
    assertTrue(doctorsForDb.getExistsInGmc());
    assertEquals(RecommendationStatus.NOT_STARTED, doctorsForDb.getDoctorStatus());
    assertEquals(now, doctorsForDb.getLastUpdatedDate());
  }
}
