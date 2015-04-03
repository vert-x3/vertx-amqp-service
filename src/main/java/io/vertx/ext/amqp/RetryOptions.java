/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.ext.amqp;

import java.util.Collections;
import java.util.List;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

@DataObject
public class RetryOptions
{
    public final static String MIN_RETRY_INTERVAL = "min-retry-interval";

    public final static String MAX_RETRY_INTERVAL = "max-retry-interval";

    public final static String MAX_RETRY_LIMIT = "max-retry-limit";

    public final static String ALT_ADDRESS_LIST = "alternative-address-list";

    public final static String RETRY_POLICY = "retry-policy";

    private final static long DEFAULT_MIN_RETRY = 1000;

    private final static long DEFAULT_MAX_RETRY = 1000 * 60 * 5;

    private long minRetryInterval = DEFAULT_MIN_RETRY;

    private long maxRetryInterval = DEFAULT_MAX_RETRY;

    private int maxRetryLimit = 1;

    private List<String> alternativeAddressList = Collections.emptyList();

    private RetryPolicy retryPolicy = RetryPolicy.NO_RETRY;

    public RetryOptions()
    {
    }

    public RetryOptions(RetryOptions options)
    {
        this.minRetryInterval = options.minRetryInterval;
        this.maxRetryInterval = options.maxRetryInterval;
        this.maxRetryLimit = options.maxRetryLimit;
    }

    @SuppressWarnings("unchecked")
    public RetryOptions(JsonObject options)
    {
        if (options != null)
        {
            this.minRetryInterval = options.getLong(MIN_RETRY_INTERVAL, DEFAULT_MIN_RETRY);
            this.maxRetryInterval = options.getLong(MAX_RETRY_INTERVAL, DEFAULT_MAX_RETRY);
            this.maxRetryLimit = options.getInteger(MAX_RETRY_LIMIT, 1);
            this.alternativeAddressList = options.getJsonArray(ALT_ADDRESS_LIST).getList();
            this.retryPolicy = RetryPolicy.valueOf(options.getString(RETRY_POLICY,
                    RetryPolicy.RETRY_UNTIL_SUCCESS.name()));
        }
    }

    public JsonObject toJson()
    {
        JsonObject json = new JsonObject();
        json.put(MIN_RETRY_INTERVAL, minRetryInterval);
        json.put(MAX_RETRY_INTERVAL, maxRetryInterval);
        json.put(MAX_RETRY_LIMIT, maxRetryLimit);
        json.put(ALT_ADDRESS_LIST, alternativeAddressList);
        json.put(RETRY_POLICY, retryPolicy.name());
        return json;
    }

    public long getMinRetryInterval()
    {
        return minRetryInterval;
    }

    public void setMinRetryInterval(long minRetryInterval)
    {
        this.minRetryInterval = minRetryInterval;
    }

    public long getMaxRetryInterval()
    {
        return maxRetryInterval;
    }

    public void setMaxRetryInterval(long maxRetryInterval)
    {
        this.maxRetryInterval = maxRetryInterval;
    }

    public int getMaxRetryLimit()
    {
        return maxRetryLimit;
    }

    public void setMaxRetryLimit(int maxRetryLimit)
    {
        this.maxRetryLimit = maxRetryLimit;
    }

    public List<String> getAlternativeAddressList()
    {
        return alternativeAddressList;
    }

    public void setAlternativeAddressList(List<String> alternativeAddressList)
    {
        this.alternativeAddressList = alternativeAddressList;
    }

    public RetryPolicy getRetryPolicy()
    {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy)
    {
        this.retryPolicy = retryPolicy;
    }
}