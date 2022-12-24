package io.github.arashiyama11

abstract class TermBase {
  abstract fun toPolynomial(): Polynomial
  abstract fun toUnary(): Unary
  abstract fun toTerm(): Term
  abstract fun canBeTerm(): Boolean
  abstract override fun equals(other: Any?): Boolean
  abstract override fun hashCode(): Int
  abstract fun isZero(): Boolean
  abstract fun isOne(): Boolean
  abstract fun toStringWith(options: Set<String>): String
}