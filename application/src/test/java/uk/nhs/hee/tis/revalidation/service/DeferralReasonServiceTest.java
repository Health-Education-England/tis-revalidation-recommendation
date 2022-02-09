/*
 * The MIT License (MIT)
 *
 * Copyright 2022 Crown Copyright (Health Education England)
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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import uk.nhs.hee.tis.revalidation.entity.DeferralReason;
import uk.nhs.hee.tis.revalidation.exception.RecommendationException;
import uk.nhs.hee.tis.revalidation.repository.DeferralReasonRepository;


@ExtendWith(MockitoExtension.class)
class DeferralReasonServiceTest {

  @InjectMocks
  DeferralReasonService deferralReasonService;

  @Mock
  DeferralReasonRepository deferralReasonRepository;

  private String code = "1";
  private String reason = "1";
  private String abbr = "1";
  private String subCode = "2";
  private String subReason = "2";
  private String subAbbr = "2";

  @Test
  void shouldGetDeferralReasonByCode() {
    final var deferral = buildTestDeferralReason();
    when(deferralReasonRepository.findById(code)).thenReturn(Optional.of(deferral));

    deferralReasonService.getDeferralReasonByCode(deferral.getCode());
    verify(deferralReasonRepository).findById(code);

  }

  @Test
  void shouldThrowExceptionWhenGetDeferralReasonByCodeIsEmpty() {
    doReturn(Optional.empty()).when(deferralReasonRepository).findById(code);

    assertThrows(RecommendationException.class, () -> {
      deferralReasonService.getDeferralReasonByCode(code);
    });
  }

  @Test
  void shouldGetDeferralSubReasonByCode() {
    final var deferral = buildTestDeferralReason();
    when(deferralReasonRepository.findById(code)).thenReturn(Optional.of(deferral));

    final var result = deferralReasonService
        .getDeferralSubReasonByReasonCodeAndReasonSubCode(code, subCode);

    verify(deferralReasonRepository).findById(code);
    assertThat(result.getReason(), is(subReason));
  }

  private DeferralReason buildTestDeferralReason() {
    final var deferralReason = DeferralReason.builder()
        .code(code)
        .reason(reason)
        .abbr(abbr)
        .build();

    final var deferralSubReason = DeferralReason.builder()
        .code(subCode)
        .reason(subReason)
        .abbr(subAbbr)
        .build();

    deferralReason.setDeferralSubReasons(List.of(deferralSubReason));
    return deferralReason;
  }
}
