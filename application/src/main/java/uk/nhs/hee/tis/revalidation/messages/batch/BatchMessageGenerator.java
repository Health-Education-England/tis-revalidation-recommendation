package uk.nhs.hee.tis.revalidation.messages.batch;

import java.util.List;

public interface BatchMessageGenerator<T, U> {

  public T generateBatchMessages(List<U> payloads);

}
