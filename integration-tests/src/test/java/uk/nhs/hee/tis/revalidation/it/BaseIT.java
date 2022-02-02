/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Crown Copyright (Health Education England)
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

package uk.nhs.hee.tis.revalidation.it;

import static java.time.LocalDate.now;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import uk.nhs.hee.tis.revalidation.dto.RoUserProfileDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeCoreDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.entity.RecommendationStatus;
import uk.nhs.hee.tis.revalidation.entity.UnderNotice;

public class BaseIT {

  protected final Faker faker = new Faker();

  @Autowired
  public ObjectMapper objectMapper;

  protected DoctorsForDB doc1, doc2, doc3, doc4, doc5;
  protected TraineeCoreDto coreDTO1, coreDTO2, coreDTO3, coreDTO4, coreDTO5;
  protected String gmcRef1, gmcRef2, gmcRef3, gmcRef4, gmcRef5;
  protected String fName1, fName2, fName3, fName4, fName5;
  protected String lName1, lName2, lName3, lName4, lName5;
  protected LocalDate subDate1, subDate2, subDate3, subDate4, subDate5;
  protected LocalDate addedDate1, addedDate2, addedDate3, addedDate4, addedDate5;
  protected UnderNotice un1, un2, un3, un4, un5;
  protected String sanction1, sanction2, sanction3, sanction4, sanction5;
  protected String desBody1, desBody2, desBody3, desBody4, desBody5;
  protected RecommendationStatus status1, status2, status3, status4, status5;
  protected LocalDate curriculumEndDate1, curriculumEndDate2, curriculumEndDate3, curriculumEndDate4, curriculumEndDate5;
  protected String progName1, progName2, progName3, progName4, progName5;
  protected String memType1, memType2, memType3, memType4, memType5;
  protected String grade1, grade2, grade3, grade4, grade5;
  protected String connectionStatus1, connectionStatus2, connectionStatus3, connectionStatus4, connectionStatus5;
  protected String admin;
  protected String roFirstName;
  protected String roLastName;
  protected String roPhoneNumber;
  protected String roEmailAddress;
  protected String roUserName;
  protected RoUserProfileDto userProfileDto;

  protected void setupData() {
    gmcRef1 = faker.number().digits(7);
    gmcRef2 = faker.number().digits(7);
    gmcRef3 = faker.number().digits(7);
    gmcRef4 = faker.number().digits(7);
    gmcRef5 = faker.number().digits(7);

    fName1 = faker.name().firstName();
    fName2 = faker.name().firstName();
    fName3 = faker.name().firstName();
    fName4 = faker.name().firstName();
    fName5 = faker.name().firstName();

    lName1 = faker.name().lastName();
    lName2 = faker.name().lastName();
    lName3 = faker.name().lastName();
    lName4 = faker.name().lastName();
    lName5 = faker.name().lastName();

    subDate1 = now().minusDays(1);
    subDate2 = now().minusDays(2);
    subDate3 = now().minusDays(3);
    subDate4 = now().minusDays(4);
    subDate5 = now().minusDays(5);

    addedDate1 = now().minusDays(5);
    addedDate2 = now().minusDays(5);
    addedDate3 = now().minusDays(5);
    addedDate4 = now().minusDays(5);
    addedDate5 = now().minusDays(5);

    un1 = faker.options().option(UnderNotice.class);
    un2 = faker.options().option(UnderNotice.class);
    un3 = faker.options().option(UnderNotice.class);
    un4 = faker.options().option(UnderNotice.class);
    un5 = faker.options().option(UnderNotice.class);

    sanction1 = faker.lorem().characters(2);
    sanction2 = faker.lorem().characters(2);
    sanction3 = faker.lorem().characters(2);
    sanction4 = faker.lorem().characters(2);
    sanction5 = faker.lorem().characters(2);

    desBody1 = "1-AIIDR8";
    desBody2 = "1-AIIDR8";
    desBody3 = "1-AIIDR8";
    desBody4 = "1-AIIDVS";
    desBody5 = "1-AIIDVS";

    status1 = faker.options().option(RecommendationStatus.class);
    status2 = faker.options().option(RecommendationStatus.class);
    status3 = faker.options().option(RecommendationStatus.class);
    status4 = faker.options().option(RecommendationStatus.class);
    status5 = faker.options().option(RecommendationStatus.class);

    curriculumEndDate1 = now();
    curriculumEndDate2 = now();
    curriculumEndDate3 = now();
    curriculumEndDate4 = now();
    curriculumEndDate5 = now();

    progName1 = faker.lorem().sentence(3);
    progName2 = faker.lorem().sentence(3);
    progName3 = faker.lorem().sentence(3);
    progName4 = faker.lorem().sentence(3);
    progName5 = faker.lorem().sentence(3);

    memType1 = faker.lorem().characters(8);
    memType2 = faker.lorem().characters(8);
    memType3 = faker.lorem().characters(8);
    memType4 = faker.lorem().characters(8);
    memType5 = faker.lorem().characters(8);

    grade1 = faker.lorem().characters(5);
    grade2 = faker.lorem().characters(5);
    grade3 = faker.lorem().characters(5);
    grade4 = faker.lorem().characters(5);
    grade5 = faker.lorem().characters(5);

    connectionStatus1 = "Yes";
    connectionStatus2 = "Yes";
    connectionStatus3 = "Yes";
    connectionStatus4 = "Yes";
    connectionStatus5 = "Yes";

    roFirstName = faker.name().firstName();
    roLastName = faker.name().lastName();
    roUserName = faker.name().username();
    roEmailAddress = faker.internet().emailAddress();
    roPhoneNumber = faker.phoneNumber().phoneNumber();

    admin = faker.internet().emailAddress();

    doc1 = new DoctorsForDB(gmcRef1, fName1, lName1, subDate1, addedDate1, un1, sanction1, status1,
        now(), desBody1, admin, null);
    doc2 = new DoctorsForDB(gmcRef2, fName2, lName2, subDate2, addedDate2, un2, sanction2, status2,
        now(), desBody2, admin, null);
    doc3 = new DoctorsForDB(gmcRef3, fName3, lName3, subDate3, addedDate3, un3, sanction3, status3,
        now(), desBody3, admin, null);
    doc4 = new DoctorsForDB(gmcRef4, fName4, lName4, subDate4, addedDate4, un4, sanction4, status4,
        now(), desBody4, admin, null);
    doc5 = new DoctorsForDB(gmcRef5, fName5, lName5, subDate5, addedDate5, un5, sanction5, status5,
        now(), desBody5, admin, null);

    coreDTO1 = new TraineeCoreDto(gmcRef1, curriculumEndDate1, memType1, progName1, grade1);
    coreDTO2 = new TraineeCoreDto(gmcRef2, curriculumEndDate2, memType2, progName2, grade2);
    coreDTO3 = new TraineeCoreDto(gmcRef3, curriculumEndDate3, memType3, progName3, grade3);
    coreDTO4 = new TraineeCoreDto(gmcRef4, curriculumEndDate4, memType4, progName4, grade4);
    coreDTO5 = new TraineeCoreDto(gmcRef5, curriculumEndDate5, memType5, progName5, grade5);
    userProfileDto = RoUserProfileDto.builder()
        .firstName(roFirstName)
        .lastName(roLastName)
        .emailAddress(roEmailAddress)
        .phoneNumber(roPhoneNumber)
        .userName(roUserName)
        .build();
  }
}
