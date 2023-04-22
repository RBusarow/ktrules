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

import com.pinterest.ktlint.rule.engine.core.api.ElementType.KDOC_TEXT
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.children
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.MAX_LINE_LENGTH_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import com.rickbusarow.ktrules.KtRulesRuleSetProvider.Companion.ABOUT
import com.rickbusarow.ktrules.rules.internal.psi.fileIndent
import com.rickbusarow.ktrules.rules.internal.psi.getKDocSections
import com.rickbusarow.ktrules.rules.internal.psi.getKDocTextWithoutLeadingAsterisks
import com.rickbusarow.ktrules.rules.internal.psi.isKDoc
import com.rickbusarow.ktrules.rules.internal.psi.isKDocSection
import com.rickbusarow.ktrules.rules.internal.psi.removeAllChildren
import com.rickbusarow.ktrules.rules.internal.removeRegex
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl

/**
 * Collapse short KDoc comments into a single line, like `/** my comment */`
 *
 * @since 1.0.7
 */
class KDocCollapseRule : Rule(
  ID,
  ABOUT,
  visitorModifiers = setOf(
    VisitorModifier.RunAfterRule(
      KDocContentWrappingRule.ID,
      REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
    ),
    VisitorModifier.RunAfterRule(
      KDocLeadingAsteriskRule.ID,
      REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
    ),
    VisitorModifier.RunAfterRule(
      KDocBlankLinesRule.ID,
      REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
    ),
  ),
  usesEditorConfigProperties = setOf(MAX_LINE_LENGTH_PROPERTY, WRAPPING_STYLE_PROPERTY)
) {

  private var maxLineLength: Int = MAX_LINE_LENGTH_PROPERTY.defaultValue

  private val skipAll by lazy { maxLineLength < 0 }

  override fun beforeFirstNode(editorConfig: EditorConfig) {

    maxLineLength = editorConfig[MAX_LINE_LENGTH_PROPERTY]

    super.beforeFirstNode(editorConfig)
  }

  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {

    if (skipAll) return
    if (!node.isKDoc()) return

    if (node.text.lines().size == 1) return

    val singleSection = node.getKDocSections()
      .filter { it.text.removeRegex("[* ]+").isNotBlank() }
      .singleOrNull()
      ?.takeIf { it.getKDocTextWithoutLeadingAsterisks().lines().size == 1 }
      ?: return

    val kdocStart = node.children().first()
    val kdocEnd = node.children().last()

    val indent = node.fileIndent(0)

    val sectionText = singleSection.getKDocTextWithoutLeadingAsterisks()

    val totalLength = sequenceOf(
      indent,
      kdocStart.text,
      sectionText.trimPreservingCodeBlockIndent(),
      kdocEnd.text
    )
      .sumOf { it.length }
      // Add two spaces to account for one space after the KDOC_START and one before KDOC_END
      .plus(2)

    if (totalLength > maxLineLength) return

    emit(node.startOffset, ERROR_MESSAGE, true)

    if (autoCorrect) {

      // If the second child of the KDoc is a whitespace, that means there's a newline and that
      //    newline should be removed.
      //
      // If there is no newline, the whitespace is included in the default KDOC_SECTION.
      //
      // If the first actual content of the KDoc is a tag, there will still be a default section and
      //    that default section will only contain a single WHITE_SPACE child.
      node.children()
        .drop(1)
        .firstOrNull()
        .takeIf { it.isWhiteSpace() }
        ?.let {
          node.removeChild(it)
        }

      val defaultSection = node.children().first { it.isKDocSection() }

      val defaultSectionText = defaultSection.getKDocTextWithoutLeadingAsterisks()
        .trimPreservingCodeBlockIndent()

      defaultSection.removeAllChildren()

      val newDefaultSectionText = when {
        defaultSectionText.startsWith(' ') -> "$defaultSectionText "
        defaultSectionText.isNotBlank() -> " $defaultSectionText "
        else -> " "
      }

      defaultSection.addChild(LeafPsiElement(KDOC_TEXT, newDefaultSectionText), null)

      val whiteSpaceBeforeEnd = kdocEnd.prevLeaf(includeEmpty = true)
        .takeIf { it.isWhiteSpace() }

      when {
        // If there is whitespace before the end and only one KDoc section, remove the whitespace
        whiteSpaceBeforeEnd != null && node.children().count { it.isKDocSection() } == 1 -> {
          node.removeChild(whiteSpaceBeforeEnd)
        }

        // If there is no whitespace before the end and more than one KDoc section, add a space
        whiteSpaceBeforeEnd == null && node.children().count { it.isKDocSection() } > 1 -> {
          kdocEnd.upsertWhitespaceBeforeMe(" ")
        }

        // If there is whitespace before the end and it's a newline, replace it with a space
        whiteSpaceBeforeEnd != null && whiteSpaceBeforeEnd.isWhiteSpaceWithNewline() -> {
          node.replaceChild(whiteSpaceBeforeEnd, PsiWhiteSpaceImpl(" "))
        }
      }
    }
  }

  private fun String.trimPreservingCodeBlockIndent() = removeRegex("^ {1,3}(?=[^ ])", "\\s$")

  internal companion object {

    val ID = RuleId("kt-rules:kdoc-collapse")
    const val ERROR_MESSAGE = "kdoc should be collapsed into a single line"
  }
}
