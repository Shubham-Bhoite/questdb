/*******************************************************************************
 *     ___                  _   ____  ____
 *    / _ \ _   _  ___  ___| |_|  _ \| __ )
 *   | | | | | | |/ _ \/ __| __| | | |  _ \
 *   | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *    \__\_\\__,_|\___||___/\__|____/|____/
 *
 *  Copyright (c) 2014-2019 Appsicle
 *  Copyright (c) 2019-2024 QuestDB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package io.questdb.griffin;

/**
 * Interface used to add steps before and/or after query compilation, e.g. cache checks and query result sending to JDBC client.
 */
public interface BatchCallback {

  /**
   * This method is called after a query is successfully compiled.
   * 
   * @param compiler The compiler that compiled the query.
   * @param cq The compiled query object.
   * @param queryText The original query text.
   * @throws Exception If there is an error during post-compilation processing.
   */
  void postCompile(SqlCompiler compiler, CompiledQuery cq, CharSequence queryText) throws Exception;

  /**
   * This method is called before a query is compiled.
   * 
   * @param compiler The compiler that will compile the query.
   * @throws Exception If there is an error during pre-compilation processing.
   */
  void preCompile(SqlCompiler compiler) throws Exception;

  default void myCustomLogic(SqlCompiler compiler, CharSequence queryText) {
    // Example logic to print the compiled query text
    System.out.println("Compiled Query: " + queryText);
  }
}

