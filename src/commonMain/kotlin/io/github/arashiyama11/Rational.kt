package io.github.arashiyama11

import kotlin.math.*

data class Rational(var numerator: Long, var denominator: Long = 1) {

  constructor(double: Double) : this(0) {
    val length = double.toString().length - double.toInt().toString().length - 1
    val d = 10.0.pow(length.toDouble()).toLong()
    numerator = (double * d).toLong()
    denominator = d
  }

  companion object {
    val ZERO = Rational(0)
    val ONE = Rational(1)
    val MINUS_ONE = Rational(-1)
  }


  override fun toString(): String {
    if (denominator < 0) {
      numerator *= -1
      denominator *= -1
    }
    if (denominator == 1L) return "$numerator"
    return "$numerator/$denominator"
  }

  fun toDouble(): Double {
    return numerator.toDouble() / denominator.toDouble()
  }

  fun toInt(): Int {
    return toDouble().toInt()
  }

  fun toRational(): Rational {
    return Rational(numerator, denominator)
  }

  operator fun plus(long: Long): Rational {
    return plus(Rational(long).reduction())
  }

  operator fun plus(int: Int): Rational {
    return plus(Rational(int.toLong())).reduction()
  }

  operator fun plus(rational: Rational): Rational {
    var a = toRational()
    var b = rational

    //オーバーフロー対策

    //通分時にオーバーフローするかどうか
    val m = max(
      log2(a.numerator.toDouble()) + log2(b.denominator.toDouble()),
      max(
        log2(a.numerator.toDouble()) + log2(b.denominator.toDouble()),
        log2(a.denominator.toDouble()) + log2(b.denominator.toDouble())
      )
    )

    if (m > 63) {
      a = a.divBoth(2.0.pow((m - 60) / 2).toLong())
      b = b.divBoth(2.0.pow((m - 60) / 2).toLong())
    }

    //足すときにオーバーフローするか
    if ((a.numerator * b.denominator).absoluteValue / 2 + (b.numerator * a.denominator).absoluteValue / 2 > Int.MAX_VALUE) {
      a = a.divBoth(2L)
      b = b.divBoth(2L)
    }

    return Rational(
      a.numerator * b.denominator + b.numerator * a.denominator, a.denominator * b.denominator
    ).reduction()
  }

  operator fun times(long: Long): Rational {
    return times(Rational(long)).reduction()
  }

  operator fun times(int: Int): Rational {
    return times(Rational(int.toLong())).reduction()
  }

  operator fun times(rational: Rational): Rational {
    var a = toRational()
    var b = rational

    //オーバーフロー対策
    val ma = max(log2(a.numerator.toDouble()), log2(a.denominator.toDouble()))
    val mb = max(log2(b.numerator.toDouble()), log2(b.denominator.toDouble()))

    if (ma > 31) {
      a = a.divBoth(2.0.pow(ma - 31).toLong())
    }

    if (mb > 31) {
      b = b.divBoth(2.0.pow(mb - 31).toLong())
    }

    return Rational(a.numerator * b.numerator, a.denominator * b.denominator).reduction()
  }

  private fun divBoth(n: Long): Rational {
    return Rational(numerator / n, denominator / n).reduction()
  }

  operator fun div(long: Long): Rational {
    return div(Rational(long)).reduction()
  }

  operator fun div(int: Int): Rational {
    return div(Rational(int.toLong())).reduction()
  }

  operator fun div(rational: Rational): Rational {
    return Rational(numerator * rational.denominator, denominator * rational.numerator).reduction()
  }

  operator fun unaryPlus(): Rational {
    return toRational()
  }

  operator fun unaryMinus(): Rational {
    return toRational() * -1
  }

  override fun equals(other: Any?): Boolean {
    if (other is Rational) {
      val a = reduction()
      val b = other.reduction()
      return a.numerator == b.numerator && a.denominator == b.denominator
    }
    return false
  }

  //最大公約数
  private tailrec fun gcd(a: Long, b: Long): Long {
    if (b == 0L) return a
    return gcd(b, a % b)
  }

  fun reduction(): Rational {
    val gc = gcd(numerator, denominator)
    return Rational(numerator / gc, denominator / gc)
  }

  //逆数
  fun reciprocal(): Rational {
    return Rational(denominator, numerator)
  }

  fun pow(i: Int): Rational {
    var r = toRational()
    for (a in 1 until i) {
      r *= this
    }
    return r
  }

  override fun hashCode(): Int {
    var result = numerator.hashCode()
    result = 31 * result + denominator.hashCode()
    return result
  }
}