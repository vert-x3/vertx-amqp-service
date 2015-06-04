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
package io.vertx.ext.amqp.impl.util;

public class Functions {
  public static String format(String format, Object... args) {
    return String.format(format, args);
  }

  public static void print(String format, Object... args) {
    System.out.println("TEMP-SYSTEM.OUT :".concat(format(format, args)));
  }

  /**
   * A quick hack to retrieve destination (source or target) from an address.
   * If '/' is missing treat the whole thing as the destination.
   * Example behavior
   * 1. localhost:5673/my-dest --> my-dest
   * 2. my-dest --> my-dest
   * 3. localhost:5673/ --> ''
   * 4. localhost:5673  --> localhost:5673
   */
  public static String extractDestination(String address) {
    int index = address.lastIndexOf('/');
    if (index > 0) {
      return address.substring(index + 1);
    } else {
      return address;
    }
  }
}