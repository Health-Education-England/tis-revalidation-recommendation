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

package uk.nhs.hee.tis.revalidation.messages.publisher;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GmcSyncMessagePublisherTest {

  private final Faker faker = new Faker();

  @InjectMocks
  GmcSyncMessagePublisher<String> gmcSyncMessagePublisher;

  @Mock
  RabbitTemplate rabbitTemplate;

  @Captor
  ArgumentCaptor<String> messageCaptor;

  @Captor
  ArgumentCaptor<String> exchangeNameCaptor;

  @Captor
  ArgumentCaptor<String> routingKeyNameCaptor;

  private String gmcNumber = faker.number().digits(7);
  private String gmcRecommendationId = faker.number().digits(3);
  private String recommendationId = faker.number().digits(3);
  private String designatedBody = faker.lorem().characters(5);

  private String recommendationStatusCheckDto = "start";

  @Test
  void shouldPublishToRabbitMq() {

    String routingKeyName = "routingKeyName";
    String exchangeName = "exchangeName";

    ReflectionTestUtils.setField(
        gmcSyncMessagePublisher, "routingKey", routingKeyName
    );
    ReflectionTestUtils.setField(
        gmcSyncMessagePublisher, "exchange", exchangeName
    );

    gmcSyncMessagePublisher.publishToBroker(recommendationStatusCheckDto);

    verify(rabbitTemplate).convertAndSend(
        exchangeNameCaptor.capture(),
        routingKeyNameCaptor.capture(),
        messageCaptor.capture()
    );

    assertThat(messageCaptor.getValue(), is("start"));
    assertThat(routingKeyNameCaptor.getValue(), is(routingKeyName));
    assertThat(exchangeNameCaptor.getValue(), is(exchangeName));
  }
}

