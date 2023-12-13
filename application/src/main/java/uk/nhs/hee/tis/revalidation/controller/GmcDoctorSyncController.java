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

package uk.nhs.hee.tis.revalidation.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.nhs.hee.tis.revalidation.service.GmcDoctorConnectionSyncService;

@Slf4j
@RestController
@RequestMapping("/api/v1/sqs")
public class GmcDoctorSyncController {

  private final GmcDoctorConnectionSyncService gmcDoctorConnectionSyncService;

  public GmcDoctorSyncController(final GmcDoctorConnectionSyncService gmcDoctorConnectionSyncService) {
    this.gmcDoctorConnectionSyncService = gmcDoctorConnectionSyncService;
  }

  /**
   * This endpoint is used to start a gmc sync manually.
   *
   * @return OK Response if this succeeds within a defined time
   */
  @GetMapping("/send-doctor")
  public ResponseEntity<Void> startGmcSync() {
    gmcDoctorConnectionSyncService.receiveMessage("gmcSyncStart");
    return ResponseEntity.ok().build();
  }

}