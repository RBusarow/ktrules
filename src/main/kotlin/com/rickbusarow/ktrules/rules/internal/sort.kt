/*
 * Copyright (C) 2023 Rick Busarow
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

package com.rickbusarow.ktrules.rules.internal

/**
 * Returns a list of all elements sorted according to the specified [selectors].
 *
 * The sort is _stable_. It means that equal elements preserve their order relative to each other
 * after sorting.
 */
fun <T> Iterable<T>.sortedWith(vararg selectors: (T) -> Comparable<*>): List<T> {
  if (this is Collection) {
    if (size <= 1) return this.toList()
    @Suppress("UNCHECKED_CAST")
    return (toTypedArray<Any?>() as Array<T>).apply { sortWith(compareBy(*selectors)) }.asList()
  }
  return toMutableList().apply { sortWith(compareBy(*selectors)) }
}
