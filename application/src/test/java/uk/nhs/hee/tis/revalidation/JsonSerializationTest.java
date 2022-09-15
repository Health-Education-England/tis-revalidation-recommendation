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

package uk.nhs.hee.tis.revalidation;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.nhs.hee.tis.revalidation.dto.DoctorsForDbDto;

class JsonSerializationTest {

  private final ObjectMapper mapper = (new RevalidationApplication()).objectMapper();

  @Test
  void testDateSerialization() throws JsonProcessingException {
    assertThat(mapper, is(notNullValue()));

    final var doctor = DoctorsForDbDto.builder()
        .doctorFirstName("first")
        .doctorLastName("last")
        .gmcReferenceNumber("gmtRef")
        .sanction("sanction")
        .underNotice("under notice")
        .dateAdded("04/07/2017")
        .submissionDate("04/07/2017").build();

    final var json = mapper.writeValueAsString(doctor);

    assertThat(json,
        is("{\"gmcReferenceNumber\":\"gmtRef\",\"doctorFirstName\":\"first\",\"doctorLastName\":\"last\",\"submissionDate\":\"04/07/2017\",\"dateAdded\":\"04/07/2017\",\"underNotice\":\"under notice\",\"sanction\":\"sanction\",\"designatedBodyCode\":null}"));
  }

}
