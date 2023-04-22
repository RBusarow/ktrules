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

import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule
import com.pinterest.ktlint.rule.engine.core.api.Rule.VisitorModifier.RunAfterRule.Mode.REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpaceWithNewline
import com.pinterest.ktlint.rule.engine.core.api.nextLeaf
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.rickbusarow.ktrules.KtRulesRuleSetProvider.Companion.ABOUT
import com.rickbusarow.ktrules.rules.internal.psi.isInKDocTag
import com.rickbusarow.ktrules.rules.internal.psi.isKDoc
import com.rickbusarow.ktrules.rules.internal.psi.isKDocEnd
import com.rickbusarow.ktrules.rules.internal.psi.isKDocLeadingAsterisk
import com.rickbusarow.ktrules.rules.internal.psi.isKDocStart
import com.rickbusarow.ktrules.rules.internal.psi.isKDocTagName
import com.rickbusarow.ktrules.rules.internal.psi.isKDocWhitespaceAfterLeadingAsterisk
import com.rickbusarow.ktrules.rules.internal.psi.isWhiteSpaceOrBlank
import com.rickbusarow.ktrules.rules.internal.psi.parent
import com.rickbusarow.ktrules.rules.internal.psi.parentsWithSelf
import org.jetbrains.kotlin.com.intellij.lang.ASTNode

/**
 * Ensures that there are no consecutive or extraneous blank lines within a KDoc comment's content
 *
 * @since 1.0.7
 */
class KDocBlankLinesRule : Rule(
  ID,
  ABOUT,
  visitorModifiers = setOf(
    RunAfterRule(
      KDocLeadingAsteriskRule.ID,
      REGARDLESS_WHETHER_RUN_AFTER_RULE_IS_LOADED_OR_DISABLED
    ),
  )
) {
  override fun beforeVisitChildNodes(
    node: ASTNode,
    autoCorrect: Boolean,
    emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit
  ) {

    if (node.parentsWithSelf().none { it.isKDoc() }) return

    if (!node.isKDocWhitespaceAfterLeadingAsterisk()) return
    if (!node.isWhiteSpaceWithNewline()) return

    val previousNewline = node.prevLeaf { it.isWhiteSpaceWithNewline() } ?: return

    val parent = node.parent ?: return
    val previousLeaf = node.prevLeaf(true) ?: return

    val prevContent = node.prevLeaf { !it.isWhiteSpaceOrBlank() && !it.isKDocLeadingAsterisk() }
    val nextContent = node.nextLeaf { !it.isWhiteSpaceOrBlank() && !it.isKDocLeadingAsterisk() }

    fun emitAndMaybeFix(errorMessage: String) {

      emit(previousLeaf.startOffset, errorMessage, true)

      if (autoCorrect) {
        parent.removeChild(previousLeaf)
        parent.removeChild(node)
      }
    }

    when {
      prevContent.isKDocStart() -> {
        emitAndMaybeFix("leading blank line in kdoc")
      }

      nextContent.isKDocEnd() -> {
        emitAndMaybeFix("trailing blank line in kdoc")
      }

      nextContent.isKDocTagName() && prevContent.isInKDocTag() -> {
        emitAndMaybeFix("extra blank line before subsequent kdoc tag")
      }

      previousNewline.isKDocWhitespaceAfterLeadingAsterisk() -> {
        emitAndMaybeFix("consecutive blank lines in kdoc")
      }

      else -> return
    }
  }

  internal companion object {

    val ID = RuleId("kt-rules:kdoc-blank-lines")
  }
}
