package io.github.arashiyama11

abstract class TermBase {
  abstract fun toPolynomial(): Polynomial
  abstract fun toUnary(): Unary
  abstract fun canBeUnary(): Boolean
  abstract override fun equals(other: Any?): Boolean
  abstract override fun hashCode(): Int
  abstract fun toStringWith(decimal: Boolean = false, lang: Language? = null): String
  abstract operator fun times(other: TermBase): TermBase
  abstract fun copy(): TermBase
  abstract fun substitute(entries: Map<Letter, TermBase>): TermBase
  abstract fun approximation(): TermBase
}