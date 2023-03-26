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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import java.util.LinkedList

internal fun KDoc.getAllTags(): List<KDocTag> {
  return collectDescendantsOfType<KDocTag>()
    .sortedBy { it.startOffset }
}

internal val PsiElement.startOffset: Int get() = textRange.startOffset

internal fun KDoc.findIndent(): String {
  val fileLines = containingFile.text.lines()

  var acc = startOffset + 1

  val numSpaces = fileLines.asSequence()
    .mapNotNull {
      if (it.length + 1 < acc) {
        acc -= (it.length + 1)
        null
      } else {
        acc
      }
    }
    .first()
  return " ".repeat(numSpaces)
}

internal fun PsiElement.depthFirst(): Sequence<PsiElement> {

  val toVisit = LinkedList(children.toList())

  return generateSequence(toVisit::removeFirstOrNull) { node ->

    repeat(node.children.lastIndex + 1) {
      toVisit.addFirst(node.children[node.children.lastIndex - it])
    }

    toVisit.removeFirstOrNull()
  }
}

internal inline fun PsiElement.depthFirst(
  crossinline predicate: (PsiElement) -> Boolean
): Sequence<PsiElement> {

  val toVisit = LinkedList(children.filter(predicate))

  return generateSequence(toVisit::removeFirstOrNull) { node ->

    if (predicate(node)) {
      val filtered = node.children.filter(predicate)

      repeat(filtered.lastIndex + 1) {
        toVisit.addFirst(filtered[filtered.lastIndex - it])
      }

      toVisit.removeFirstOrNull()
    } else {
      null
    }
  }
}

internal inline fun <reified T : PsiElement> PsiElement.isPartOf(): Boolean =
  getNonStrictParentOfType<T>() != null

internal inline fun <reified T : PsiElement> PsiElement.getChildrenOfTypeRecursive(): List<T> {
  return generateSequence(children.asSequence()) { children ->
    children.toList()
      .flatMap { it.children.toList() }
      .takeIf { it.isNotEmpty() }
      ?.asSequence()
  }
    .flatten()
    .filterIsInstance<T>()
    .toList()
}
