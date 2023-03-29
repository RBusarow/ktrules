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
import io.kotest.matchers.shouldBe
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import com.pinterest.ktlint.test.format as ktlintTestFormat

class NoSinceInKDocRuleTest {

  private val currentVersion = "0.2.3"

  val rules = setOf(
    RuleProvider { NoSinceInKDocRule() }
  )

  @Test
  fun `a missing since is not an issue if the version is a -SNAPSHOT`() {

    val rules = setOf(
      RuleProvider { NoSinceInKDocRule() }
    )

    rules.format(
      """
      /**
       * comment
       *
       * @property name a name
       */
      data class Subject(
        val name: String
      )
      """.trimIndent(),
      editorConfigOverride = EditorConfigOverride.from(PROJECT_VERSION_PROPERTY to "0.0.1-SNAPSHOT")
    ) shouldBe
      """
      /**
       * comment
       *
       * @property name a name
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
  }

  @Test
  fun `missing since in comment is auto-corrected`() {

    rules.format(
      """
      /**
       * comment
       *
       * @property name a name
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
      /**
       * comment
       *
       * @property name a name
       * @since $currentVersion
       */
      data class Subject(
        val name: String
      )
    """.trimIndent()
  }

  @Test
  fun `missing since in empty comment is auto-corrected`() {

    rules.format(
      """
      /** */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /** @since $currentVersion */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `missing since in suppressed comment is auto-corrected`() {

    rules.format(
      """
      /** @suppress */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /**
     * @suppress
     * @since $currentVersion
     */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `missing since in nested comment is auto-corrected`() {

    rules.format(
      """
      class Outer {
        /**
         * comment
         *
         * @property name a name
         */
        data class Subject(
          val name: String
        )
      }
      """.trimIndent()
    ) shouldBe """
    class Outer {
      /**
       * comment
       *
       * @property name a name
       * @since $currentVersion
       */
      data class Subject(
        val name: String
      )
    }
    """.trimIndent()
  }

  @Test
  fun `single-line kdoc is auto-corrected`() {

    rules.format(
      """
      /** comment */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /**
     * comment
     *
     * @since $currentVersion
     */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `single-line kdoc with tag is auto-corrected`() {

    rules.format(
      """
      /** @property name a name */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /**
     * @property name a name
     * @since $currentVersion
     */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `since tag without version content is auto-corrected`() {

    rules.format(
      """
      /**
       * comment
       *
       * @property name a name
       * @since
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /**
     * comment
     *
     * @property name a name
     * @since $currentVersion
     */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `multi line kdoc without tags has blank line before since tag`() {

    rules.format(
      """
      /**
       * comment
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /**
     * comment
     *
     * @since $currentVersion
     */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `multi line blank kdoc is auto-corrected`() {

    rules.format(
      """
      /**
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe """
    /** @since $currentVersion */
    data class Subject(
      val name: String
    )
    """.trimIndent()
  }

  @Test
  fun `a missing since is not an issue if the version is an -RC`() {

    val rules = setOf(
      RuleProvider { NoSinceInKDocRule() }
    )

    rules.format(
      """
      /**
       * comment
       *
       * @property name a name
       */
      data class Subject(
        val name: String
      )
      """.trimIndent(),
      editorConfigOverride = EditorConfigOverride.from(WRAPPING_STYLE_PROPERTY to "0.0.1-RC")
    ) shouldBe
      """
      /**
       * comment
       *
       * @property name a name
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
  }

  @Test
  fun `existing since has no issue`() {

    rules.format(
      """
      /**
       * comment
       *
       * @property name a name
       * @since 0.0.1
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
    ) shouldBe
      """
      /**
       * comment
       *
       * @property name a name
       * @since 0.0.1
       */
      data class Subject(
        val name: String
      )
      """.trimIndent()
  }

  private fun Set<RuleProvider>.format(
    @Language("kotlin")
    text: String,
    editorConfigOverride: EditorConfigOverride =
      EditorConfigOverride.from(PROJECT_VERSION_PROPERTY to currentVersion)
  ): String = ktlintTestFormat(
    text = text,
    filePath = null,
    editorConfigOverride = editorConfigOverride,
  )
    .first
}
