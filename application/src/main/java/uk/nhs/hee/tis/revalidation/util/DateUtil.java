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

import static java.time.LocalDate.parse;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class DateUtil {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final DateTimeFormatter GMC_DATE_FORMATTER = DateTimeFormatter
      .ofPattern("dd/MM/yyyy");

  public static LocalDate formatDateTime(final String date) {
    log.debug("Parsing date time for given date: {}", date);
    if (StringUtils.hasLength(date)) {
      final var dateWithoutTime = date.contains(" ") ? date.substring(0, date.indexOf(" ")) : date;
      return formatDate(dateWithoutTime);
    }

    return null;
  }

  public static LocalDate formatDate(final String date) {
    log.debug("Parsing date for given date: {}", date);
    if (StringUtils.hasLength(date)) {
      return parse(date, DATE_FORMATTER);
    }

    return null;
  }

  public static String parseDate(final LocalDate date) {
    return date != null ? date.toString() : null;
  }

  public static String convertDateInGmcFormat(final LocalDate date) {
    log.debug("Format date to GMC format: {}", date);
    if (date != null) {
      return date.format(GMC_DATE_FORMATTER);
    }

    return null;
  }

  public static LocalDate convertGmcDateToLocalDate(final String date) {
    log.debug("Format date to GMC format: {}", date);
    if (StringUtils.hasLength(date)) {
      return parse(date, GMC_DATE_FORMATTER);
    }

    return null;
  }

}
