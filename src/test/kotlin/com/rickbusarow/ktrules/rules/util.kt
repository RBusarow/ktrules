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

package com.rickbusarow.ktrules.rules

import com.pinterest.ktlint.core.RuleProvider
import com.pinterest.ktlint.core.api.EditorConfigOverride
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import java.util.stream.Stream
import com.pinterest.ktlint.test.format as ktlintTestFormat

fun Set<RuleProvider>.format(
  @Language("kotlin")
  text: String,
  editorConfigOverride: EditorConfigOverride = EditorConfigOverride.EMPTY_EDITOR_CONFIG_OVERRIDE
): String = ktlintTestFormat(
  text = text,
  filePath = null,
  editorConfigOverride = editorConfigOverride,
)
  .first

inline fun <T> Iterable<T>.container(
  crossinline name: (T) -> String,
  crossinline action: (T) -> Iterable<DynamicNode>
): Stream<DynamicContainer> = map { t ->
  DynamicContainer.dynamicContainer(name(t), action(t))
}.stream()

fun test(name: String, action: () -> Unit): DynamicTest = DynamicTest.dynamicTest(name, action)

inline fun <T> Iterable<T>.test(
  crossinline name: (T) -> String,
  crossinline action: (T) -> Unit
): List<DynamicTest> = map { t ->
  DynamicTest.dynamicTest(name(t)) { action(t) }
}
