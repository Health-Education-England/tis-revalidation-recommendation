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

package uk.nhs.hee.tis.revalidation.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import uk.nhs.hee.tis.revalidation.dto.RevalidationSummaryDto;
import uk.nhs.hee.tis.revalidation.dto.TraineeRecommendationRecordDto;
import uk.nhs.hee.tis.revalidation.entity.DoctorsForDB;
import uk.nhs.hee.tis.revalidation.messages.payloads.IndexSyncMessage;
import uk.nhs.hee.tis.revalidation.repository.DoctorsForDBRepository;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class GmcDoctorConnectionSyncServiceTest {

  public static final String GMC_SYNC_START = "gmcSyncStart";
  @Captor
  private ArgumentCaptor<SendMessageBatchRequest> batchCaptor;
  @Captor
  private ArgumentCaptor<SendMessageRequest> singleCaptor;
  @Captor
  private ArgumentCaptor<IndexSyncMessage<RevalidationSummaryDto>> objectCaptor;

  @InjectMocks
  private GmcDoctorConnectionSyncService gmcDoctorConnectionSyncService;
  @Mock
  private SqsAsyncClient sqsClient;
  @Mock
  private DoctorsForDBRepository doctorsForDBRepository;
  @Mock
  private RecommendationService recommendationService;
  @Mock
  private ObjectMapper objectMapper;

  private List<DoctorsForDB> doctorsForDBList;
  private DoctorsForDB doctor1;
  private TraineeRecommendationRecordDto recommendation1;
  private final String gmcOutcome1 = "APPROVED";
  private final String gmcRef1 = "1111111";
  private final String admin1 = "admin";
  private final String sqsEndpoint = "/endpoint";

  @BeforeEach
  void setup() {
    buildDoctorsList();
    buildRecommendations();
    ReflectionTestUtils.setField(gmcDoctorConnectionSyncService, "sqsEndPoint", sqsEndpoint);
  }

  @Test
  void shouldRetrieveAllDoctors() {
    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));
    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(doctorsForDBRepository).findAll();
  }

  @Test
  void shouldNotRetrieveDoctorsIfNullMessageSupplied() {
    gmcDoctorConnectionSyncService.receiveMessage(null);

    verify(doctorsForDBRepository, never()).findAll();
  }

  @Test
  void shouldNotRetrieveDoctorsIfIncorrectMessageSupplied() {
    gmcDoctorConnectionSyncService.receiveMessage("anyString");

    verify(doctorsForDBRepository, never()).findAll();
  }

  @Test
  void shouldSendRetrievedDoctorsToSqs() throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    when(objectMapper.writeValueAsString(objectCaptor.capture()))
        .thenReturn("fooey")
        .thenReturn("end");

    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));
    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(sqsClient).sendMessageBatch(batchCaptor.capture());
    verify(sqsClient).sendMessage(singleCaptor.capture());

    final List<IndexSyncMessage<RevalidationSummaryDto>> messageObjects = objectCaptor.getAllValues();
    IndexSyncMessage<RevalidationSummaryDto> messageObject = messageObjects.get(0);
    assertThat(messageObject.getSyncEnd(), is(false));
    assertThat(messageObject.getPayload().getGmcOutcome(), is(gmcOutcome1));
    assertThat(messageObject.getPayload().getDoctor().getGmcReferenceNumber(), is(gmcRef1));
    assertThat(messageObject.getPayload().getDoctor().getAdmin(), is(admin1));

    List<SendMessageBatchRequestEntry> entries = batchCaptor.getValue().entries();
    assertThat(entries.size(), is(1));
    assertThat(entries.get(0).messageBody(), is("fooey"));
    assertThat(singleCaptor.getValue().messageBody(), is("end"));
  }

  @Test
  void shouldBatchMessages() throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(List.of(doctor1, doctor1));
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenReturn("fooey")
        .thenReturn("fooey2")
        .thenReturn("end");

    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));
    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(sqsClient).sendMessageBatch(batchCaptor.capture());
    verify(sqsClient).sendMessage(singleCaptor.capture());

    List<SendMessageBatchRequestEntry> entries = batchCaptor.getValue().entries();
    assertThat(entries.size(), is(2));
    assertThat(entries.get(0).messageBody(), is("fooey"));
    assertThat(entries.get(1).messageBody(), is("fooey2"));
    assertThat(singleCaptor.getValue().messageBody(), is("end"));
  }

  @Test
  void shouldNotSendEmptyBatches() throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    JsonProcessingException expected = new JsonProcessingException("Expected") {
    };
    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenThrow(expected)
        .thenReturn("end");

    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(sqsClient, never()).sendMessageBatch(any(SendMessageBatchRequest.class));
    verify(sqsClient).sendMessage(singleCaptor.capture());
    assertThat(singleCaptor.getValue().messageBody(), is("end"));
  }

  @Test
  void shouldSkipOverUnmarshalledDoctor() throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(List.of(doctor1, doctor1));
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    JsonProcessingException expected = new JsonProcessingException("Expected") {
    };
    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenThrow(expected)
        .thenReturn("fooey2")
        .thenReturn("end");

    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));
    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(sqsClient).sendMessageBatch(batchCaptor.capture());
    verify(sqsClient).sendMessage(singleCaptor.capture());

    List<SendMessageBatchRequestEntry> entries = batchCaptor.getValue().entries();
    assertThat(entries.size(), is(1));
    assertThat(entries.get(0).messageBody(), is("fooey2"));
    assertThat(singleCaptor.getValue().messageBody(), is("end"));
  }

  @Test
  void shouldCatchExceptionMarshallingSyncEndFlag(CapturedOutput output) throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    JsonProcessingException expected = new JsonProcessingException("Expected") {
    };
    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenReturn("fooey")
        .thenThrow(expected);

    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageBatchResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    verify(sqsClient).sendMessageBatch(batchCaptor.capture());
    verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));

    List<SendMessageBatchRequestEntry> entries = batchCaptor.getValue().entries();
    assertThat(entries.size(), is(1));
    assertThat(entries.get(0).messageBody(), is("fooey"));
    assertThat(output.getOut(), containsString(
        "Unable to convert 'syncEnd' message. Downstream services need notification."));
  }

  @Test
  void shouldLogErrorWhenBatchSendFails(CapturedOutput output) throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenReturn("fooey")
        .thenReturn("end");

    CompletableFuture<SendMessageBatchResponse> failedFuture =
        new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Batch send failed"));
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(failedFuture);

    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    assertThat(output.getOut(), containsString("Failed to send batch for doctors"));
  }

  @Test
  void shouldLogErrorWhenSomeMessagesFail(CapturedOutput output) throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);
    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenReturn("fooey")
        .thenReturn("end");

    BatchResultErrorEntry errorEntry = BatchResultErrorEntry.builder().id(gmcRef1)
        .message("failure").build();
    SendMessageBatchResponse response = SendMessageBatchResponse.builder().failed(errorEntry)
        .build();
    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(response));

    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    assertThat(output.getOut(), containsString("Failed to send message for doctor " + gmcRef1));
  }

  @Test
  void shouldLogErrorWhenSendSyncEndFails(CapturedOutput output) throws Exception {
    when(doctorsForDBRepository.findAll()).thenReturn(doctorsForDBList);
    when(recommendationService.getLatestRecommendation(gmcRef1)).thenReturn(recommendation1);

    when(objectMapper.writeValueAsString(any(IndexSyncMessage.class)))
        .thenReturn("fooey")
        .thenReturn("end");

    when(sqsClient.sendMessageBatch(any(SendMessageBatchRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(
            SendMessageBatchResponse.builder().build()));

    CompletableFuture<SendMessageResponse> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("syncEnd send failed"));
    when(sqsClient.sendMessage(any(SendMessageRequest.class)))
        .thenReturn(failedFuture);

    gmcDoctorConnectionSyncService.receiveMessage(GMC_SYNC_START);

    assertThat(output.getOut(),
        containsString("Failed to send final syncEnd message to SQS for queue"));
  }

  private void buildDoctorsList() {
    doctor1 = DoctorsForDB.builder()
        .gmcReferenceNumber(gmcRef1)
        .admin(admin1)
        .build();
    doctorsForDBList = List.of(doctor1);
  }

  private void buildRecommendations() {
    recommendation1 = TraineeRecommendationRecordDto.builder()
        .gmcNumber(gmcRef1)
        .gmcOutcome(gmcOutcome1)
        .build();
  }
}
