/*
 * Copyright 2020 Yelp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yelp.nrtsearch.server.luceneserver.field;

import static org.junit.Assert.assertEquals;

import com.yelp.nrtsearch.server.grpc.AddDocumentRequest;
import com.yelp.nrtsearch.server.grpc.AddDocumentRequest.MultiValuedField;
import com.yelp.nrtsearch.server.grpc.FieldDefRequest;
import com.yelp.nrtsearch.server.grpc.Query;
import com.yelp.nrtsearch.server.grpc.RangeQuery;
import com.yelp.nrtsearch.server.grpc.SearchRequest;
import com.yelp.nrtsearch.server.grpc.SearchResponse;
import com.yelp.nrtsearch.server.luceneserver.ServerTestCase;
import io.grpc.testing.GrpcCleanupRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.ClassRule;
import org.junit.Test;

public class DateTimeFieldDefTest extends ServerTestCase {

  @ClassRule public static final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

  protected List<String> getIndices() {
    return Collections.singletonList(DEFAULT_TEST_INDEX);
  }

  protected FieldDefRequest getIndexDef(String name) throws IOException {
    return getFieldsFromResourceFile("/field/registerFieldsDateTime.json");
  }

  protected void initIndex(String name) throws Exception {
    List<AddDocumentRequest> docs = new ArrayList<>();
    AddDocumentRequest docWithTimestamp1 =
        AddDocumentRequest.newBuilder()
            .setIndexName(name)
            .putFields("doc_id", MultiValuedField.newBuilder().addValue("1").build())
            .putFields(
                "timestamp_epoch_millis",
                MultiValuedField.newBuilder().addValue("1611742000").build())
            .putFields(
                "timestamp_string_format",
                MultiValuedField.newBuilder().addValue("2021-01-27 10:06:40").build())
            .build();
    AddDocumentRequest docWithTimestamp2 =
        AddDocumentRequest.newBuilder()
            .setIndexName(name)
            .putFields("doc_id", MultiValuedField.newBuilder().addValue("2").build())
            .putFields(
                "timestamp_epoch_millis",
                MultiValuedField.newBuilder().addValue("1610742000").build())
            .putFields(
                "timestamp_string_format",
                MultiValuedField.newBuilder().addValue("2021-01-15 20:20:00").build())
            .build();
    docs.add(docWithTimestamp1);
    docs.add(docWithTimestamp2);
    addDocuments(docs.stream());
  }

  @Test
  public void testDateTimeRangeQueryEpochMillis() {
    SearchResponse response =
        doQuery(
            Query.newBuilder()
                .setRangeQuery(
                    RangeQuery.newBuilder()
                        .setField("timestamp_epoch_millis")
                        .setLower("1610741000")
                        .setUpper("1610743000")
                        .build())
                .build(),
            List.of("doc_id"));
    assertFields(response, "2");
  }

  @Test
  public void testDateTimeRangeQueryStringDateFormat() {
    SearchResponse response =
        doQuery(
            Query.newBuilder()
                .setRangeQuery(
                    RangeQuery.newBuilder()
                        .setField("timestamp_string_format")
                        .setLower("2021-01-27 10:05:40")
                        .setUpper("2021-01-27 10:07:40")
                        .build())
                .build(),
            List.of("doc_id"));
    assertFields(response, "1");
  }

  @Test
  public void testIndexInvalidEpochMillisDateTime() throws Exception {

    String dateTimeField = "timestamp_epoch_millis";
    String dateTimeValue = "definitely not a long";
    String dateTimeFormat = "epoch_millis";

    List<AddDocumentRequest> docs = new ArrayList<>();
    AddDocumentRequest docWithTimestamp =
        AddDocumentRequest.newBuilder()
            .setIndexName(DEFAULT_TEST_INDEX)
            .putFields("doc_id", MultiValuedField.newBuilder().addValue("1").build())
            .putFields(dateTimeField, MultiValuedField.newBuilder().addValue(dateTimeValue).build())
            .build();

    docs.add(docWithTimestamp);
    try {
      addDocuments(docs.stream());
    } catch (Exception e) {
      assertEquals(
          formatAddDocumentsExceptionMessage(dateTimeField, dateTimeValue, dateTimeFormat),
          e.getMessage());
    }
  }

  @Test
  public void testIndexInvalidStringDateTime() throws Exception {

    String dateTimeField = "timestamp_string_format";
    String dateTimeValue = "1610742000";
    String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";

    List<AddDocumentRequest> docs = new ArrayList<>();
    AddDocumentRequest docWithTimestamp =
        AddDocumentRequest.newBuilder()
            .setIndexName(DEFAULT_TEST_INDEX)
            .putFields("doc_id", MultiValuedField.newBuilder().addValue("1").build())
            .putFields(dateTimeField, MultiValuedField.newBuilder().addValue(dateTimeValue).build())
            .build();

    docs.add(docWithTimestamp);
    try {
      addDocuments(docs.stream());
    } catch (RuntimeException e) {
      assertEquals(
          formatAddDocumentsExceptionMessage(dateTimeField, dateTimeValue, dateTimeFormat),
          e.getMessage());
    }
  }

  @Test
  public void testRangeQueryEpochMillisInvalidFormat() {

    String dateTimeValueLower = "I'm not a long";
    String dateTimeValueUpper = "34234234.4234234";

    try {
      doQuery(
          Query.newBuilder()
              .setRangeQuery(
                  RangeQuery.newBuilder()
                      .setField("timestamp_epoch_millis")
                      .setLower(dateTimeValueLower)
                      .setUpper(dateTimeValueUpper)
                      .build())
              .build(),
          List.of("doc_id"));
    } catch (RuntimeException e) {
      assertEquals(
          String.format(
              "UNKNOWN: error while trying to execute search for index test_index. check logs for full searchRequest.\n"
                  + "For input string: \"%s\"",
              dateTimeValueLower),
          e.getMessage());
    }
  }

  @Test
  public void testRangeQueryStringDateTimeInvalidFormat() {

    String dateTimeValueLower = "34234234.4234234";
    String dateTimeValueUpepr = "I'm not a correct date string";

    try {
      doQuery(
          Query.newBuilder()
              .setRangeQuery(
                  RangeQuery.newBuilder()
                      .setField("timestamp_string_format")
                      .setLower(dateTimeValueLower)
                      .setUpper(dateTimeValueUpepr)
                      .build())
              .build(),
          List.of("doc_id"));
    } catch (RuntimeException e) {
      assertEquals(
          String.format(
              "UNKNOWN: error while trying to execute search for index test_index. check logs for full searchRequest.\n"
                  + "Text '%s' could not be parsed at index 0",
              dateTimeValueLower),
          e.getMessage());
    }
  }

  private SearchResponse doQuery(Query query, List<String> fields) {
    return getGrpcServer()
        .getBlockingStub()
        .search(
            SearchRequest.newBuilder()
                .setIndexName(DEFAULT_TEST_INDEX)
                .setStartHit(0)
                .setTopHits(10)
                .addAllRetrieveFields(fields)
                .setQuery(query)
                .build());
  }

  private void assertFields(SearchResponse response, String... expectedIds) {
    assertDataFields(response, "doc_id", expectedIds);
  }

  private void assertDataFields(
      SearchResponse response, String fieldName, String... expectedValues) {
    Set<String> seenSet = new HashSet<>();
    for (SearchResponse.Hit hit : response.getHitsList()) {
      String id = hit.getFieldsOrThrow(fieldName).getFieldValue(0).getTextValue();
      seenSet.add(id);
    }
    Set<String> expectedSet = new HashSet<>(Arrays.asList(expectedValues));
    assertEquals(seenSet, expectedSet);
  }

  private String formatAddDocumentsExceptionMessage(
      String dateTimeField, String dateTimeValue, String dateTimeFormat) {
    return String.format(
        "io.grpc.StatusRuntimeException: INTERNAL: error while trying to addDocuments \n"
            + "java.lang.Exception: java.lang.IllegalArgumentException: %s "
            + "could not parse %s as date_time with format %s",
        dateTimeField, dateTimeValue, dateTimeFormat);
  }
}
