package io.github.arashiyama11

abstract class TermBase {
  abstract fun toPolynomial(): Polynomial
  abstract fun toUnary(): Unary
  abstract override fun equals(other: Any?): Boolean
  abstract override fun hashCode(): Int
}