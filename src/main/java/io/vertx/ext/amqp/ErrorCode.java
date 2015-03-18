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

public enum ErrorCode
{
    INTERNAL_ERROR,

    AUTHENTICATION_ERROR_AT_AMQP_PEER,

    ACCESS_DENIED_BY_AMQP_PEER,

    INVALID_ADDRESS_FORMAT,

    ADDRESS_NOT_FOUND_AT_AMQP_PEER,

    ALREADY_EXISTS,

    LINK_CLOSED,

    LINK_NOT_READY,

    LINK_RETRY_IN_PROGRESS,

    LINK_FAILED,

    LINK_RETRY_FAILED,

    INVALID_LINK_REF,

    INVALID_MSG_REF,

    INVALID_MSG_FORMAT,

    SESSION_CLOSED,

    SESSION_ERROR,

    OPERATION_TIMED_OUT
}