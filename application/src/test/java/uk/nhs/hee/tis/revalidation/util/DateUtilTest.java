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

package uk.nhs.hee.tis.revalidation.util;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.convertDateInGmcFormat;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDate;
import static uk.nhs.hee.tis.revalidation.util.DateUtil.formatDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;


class DateUtilTest {

  @Test
  void shouldFormatDateTime() {
    final var dateToFormat = "2017-07-19 12:58:02";
    final var localDateTime = formatDateTime(dateToFormat);
    assertThat(localDateTime, is(notNullValue()));
    assertThat(localDateTime.getYear(), is(2017));
    assertThat(localDateTime.getMonthValue(), is(7));
    assertThat(localDateTime.getDayOfMonth(), is(19));
  }

  @Test
  void shouldFormatDateTimeWithoutTime() {
    final var dateToFormat = "2017-07-19";
    final var localDateTime = formatDateTime(dateToFormat);
    assertThat(localDateTime, is(notNullValue()));
    assertThat(localDateTime.getYear(), is(2017));
    assertThat(localDateTime.getMonthValue(), is(7));
    assertThat(localDateTime.getDayOfMonth(), is(19));
  }

  @Test
  void shouldFormatDate() {
    final var dateToFormat = "2017-07-19";
    final var localDate = formatDate(dateToFormat);
    assertThat(localDate, is(notNullValue()));
    assertThat(localDate.getYear(), is(2017));
    assertThat(localDate.getMonthValue(), is(7));
    assertThat(localDate.getDayOfMonth(), is(19));
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldReturnNullWhenDateTimeIsEmpty(String dateToFormat) {
    final var localDateTime = formatDateTime(dateToFormat);
    assertNull(localDateTime);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void shouldReturnNullWhenDateIsEmpty(String dateToFormat) {
    final var localDateTime = formatDate(dateToFormat);
    assertNull(localDateTime);
  }

  @Test
  void shouldFormatDateToGmcFormat() {
    final var dateToFormat = "2017-07-19";
    final var localDateTime = formatDate(dateToFormat);

    final var gmcDate = convertDateInGmcFormat(localDateTime);
    assertNotNull(gmcDate);
    assertThat(gmcDate, is("19/07/2017"));
  }
}