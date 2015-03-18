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

/**
 * Specifies the number of retry attempts to be made before giving up.
 *
 */
public enum RetryPolicy
{
    /**
     * No retry. An error will be sent, notifying the link failed.
     */
    NO_RETRY,

    /**
     * Stop retrying once the number of unsuccessful retries have reached the
     * limit set by {@link RecoveryOptions#setMaxRetryLimit(int)}
     */
    RETRY_UPTO_MAX_LIMIT,

    /**
     * Continue to retry until the link is successfully re-established.
     */
    RETRY_UNTIL_SUCCESS;
}