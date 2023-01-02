package io.github.arashiyama11

abstract class TermBase {
  abstract fun toPolynomial(): Polynomial
  abstract fun toUnary(): Unary
  abstract fun canBeUnary():Boolean
  abstract override fun equals(other: Any?): Boolean
  abstract override fun hashCode(): Int
  fun toStringWith()=toString()
  abstract operator fun times(other: TermBase): TermBase
  abstract fun copy():TermBase
}