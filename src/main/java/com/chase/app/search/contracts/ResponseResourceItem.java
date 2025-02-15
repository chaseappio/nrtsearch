/*
 * Copyright 2021 Yelp Inc.
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
package com.chase.app.search.contracts;

public class ResponseResourceItem {
  public String id;
  public String appId;
  public String updateId;
  public String accountId;
  public String linkId;
  public String type;
  public String externalId;
  public String name;
  public long indexTime;
  public Object traits;
  public Object data;
}
