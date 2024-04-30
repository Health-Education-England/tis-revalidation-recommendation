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

package uk.nhs.hee.tis.revalidation.entity;

public enum GmcResponseCode {

  SUCCESS("0", "Success"),
  MISSING_OR_INVALID_GMC_REF_NUMBER("70", "Missing / Invalid Doctor GMC reference number"),
  MISSING_OR_INVALID_DESIGNATED_BODY("80", "Missing / Invalid Designated Body code"),
  YOUR_ACCOUNT_DOES_NOT_HAVE_ACCESS_TO_DB("81", "Your account does not have access to this DB"),
  MISSING_DOCTOR_SURNAME("82", "Missing Doctor surname"),
  MISSING_OR_INVALID_RO_GMC_REF_NUMBER("84",
      "Missing / Invalid Responsible Officer GMC reference number"),
  MISSING_RO_SURNAME("85", "Missing Responsible Officer’s surname"),
  INVALID_RECOMMENDATION("87", "Invalid Recommendation"),
  INVALID_RECOMMENDATION_REASON("88", "Invalid Recommendation Reason"),
  INVALID_OR_UNEXPECTED_DEFER_DATE("89", "Invalid / Unexpected Defer Date"),
  DOCTOR_NOT_SUBJECT_TO_REVALIDATION("90", "Doctor not subject to revalidation"),
  OPEN_RECOMMENDATION_EXISTS("91", "Open recommendation exists"),
  DOCTOR_NOT_UNDER_NOTICE("92", "Doctor not under Notice"),
  DOCTOR_REVALIDATION_ON_HOLD("93", "Doctors revalidation is \"On Hold\""),
  INVALID_RECOMMENDATION_SUBREASON("94", "Invalid Recommendation Subreason"),
  RO_NOT_LICENSED("95", "Responsible Officer is not licensed"),
  MISSING_RO("96", "Missing Responsible Officer"),
  MISSING_RO_RECOMMENDATION_SUBREASONS("97",
      "Missing RO Recommendation Subreasons"),
  INTERNAL_ERROR("98", "Internal error"),
  INVALID_CREDENTIALS("99", "Invalid Credentials (user name / password / IP address)"),
  MISSING_INTERNAL_USER("110", "Missing Internal User"),
  MISSING_OR_INVALID_REASON_CODE("120", "Missing / Invalid Change Code (Reason)"),
  INVALID_RECOMMENDATION_ID("130", "Invalid Recommendation ID"),
  DOCTOR_NOT_ASSOCIATED_WITH_DESIGNATED_BODY("140",
      "Doctor not associated with your Designated Body"),
  MISSING_INTERNAL_USER_EMAIL("150",
      "Missing Internal User email address"),
  DOCTOR_DEFERRAL_ON_EARLY_DATE("161",
      "Deferral cannot be submitted before Revalidation Date - 120 days");

  final String code;
  final String message;

  GmcResponseCode(final String code, final String message) {
    this.code = code;
    this.message = message;
  }

  public static GmcResponseCode fromCode(final String code) {
    for (final var responseCode : GmcResponseCode.values()) {
      if (responseCode.getCode().equals(code)) {
        return responseCode;
      }
    }
    return null;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
