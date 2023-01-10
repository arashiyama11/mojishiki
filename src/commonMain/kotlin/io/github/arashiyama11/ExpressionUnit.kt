package io.github.arashiyama11

import kotlin.math.*

sealed class ExpressionUnit : TermBase() {
  companion object {
    fun parse(input: String): ExpressionUnit {
      val str = input.trim()
      var a = 0
      val isMinus = if (str[a] == '-') {
        a++
        true
      } else if (str[a] == '+') {
        a++
        false
      } else false

      while (str[a].isWhitespace()) a++

      val func = validFunctions.indexOfFirst { str.substring(a).startsWith(it) }
      return if (str[a].isDigit()) {
        //整数、小数、分数
        val numStart = a
        val num = str.substring(a).takeWhile {
          a++
          it.isDigit()
        }
        when (str[a - 1]) {
          '.' -> Rational(input.substring(numStart).toDouble())
          '/' -> Rational(num.toLong(), str.substring(a).toLong())
          else -> Rational(num.toLong())
        } * if (isMinus) Rational.MINUS_ONE else Rational.ONE
      } else if (func != -1) {
        a += validFunctions[func].length + 1
        val args = mutableListOf<String>()
        var b = a
        var depth = 0
        while (a < str.length) {
          when (str[a]) {
            '(' -> depth++
            ')' -> if (depth == 0) break else depth--
            ',' -> if (depth == 0) {
              args += str.substring(b, a)
              b = a + 1
            }
          }
          a++
        }
        args += str.substring(b, str.length - 1)
        Func(validFunctions[func], args.map { Polynomial(it) })
      } else {
        Letter(str[a])
      }
    }
  }

  override fun times(other: TermBase): TermBase {
    return when (other) {
      is Polynomial, is Unary -> other.times(this)
      is ExpressionUnit -> Unary(listOf(this, other))
      else -> throw Exception("")
    }
  }
}

data class Letter(val letter: Char) : ExpressionUnit() {

  init {
    if (letter !in valid) throw InvalidLetterException(letter)
  }

  companion object {
    const val valid = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
  }

  override fun toPolynomial() = Polynomial(listOf(toUnary()))

  override fun toUnary() = Unary(listOf(toLetter()))

  fun toLetter() = Letter(letter)

  override fun toString() = letter.toString()

  override fun copy() = toLetter()

  override fun canBeUnary() = true

  override fun substitute(entries: Map<Letter, TermBase>) = entries[this] ?: this

  override fun approximation() = this
}

data class Func(val name: String, val args: List<TermBase>) : ExpressionUnit() {

  constructor(name: String, vararg args: TermBase) : this(name, args.toList())

  override fun toPolynomial() = Polynomial(listOf(toUnary()))

  override fun toUnary() = Unary(listOf(toFunc()))

  fun toFunc() = Func(name, args.toList())

  override fun toString(): String {
    return if (specialFunctions.containsKey(name) && specialFunctions[name]!!.toStringFn != null) {
      specialFunctions[name]!!.toStringFn!!.invoke(args)
    } else "$name(${args.joinToString(",")})"
  }

  override fun copy() = toFunc()

  override fun canBeUnary() = true

  override fun substitute(entries: Map<Letter, TermBase>) = Func(name, args.map { it.substitute(entries) })

  override fun approximation(): TermBase {
    return if (args.all { it.canBeUnary() && it.toUnary().canBeRational() }) {
      specialFunctions[name]!!.approximation(args.map { it.toUnary().toRational().toDouble() })
    } else this
  }
}

data class Rational(var numerator: Long, var denominator: Long = 1) : ExpressionUnit() {

  init {
    if (denominator < 0) {
      numerator *= -1
      denominator *= -1
    }
  }

  constructor(double: Double) : this(0) {
    //This is decimal length
    //in js , 3.0.toString()==3 but others are 3.0.toString()==3.0
    //so in js,if input is integer, length will be -1
    var length = double.toString().length - double.toInt().toString().length - 1
    if (length == -1) length = 1
    val d = 10.0.pow(length.toDouble()).toLong()
    numerator = (double * d).toLong()
    denominator = d

    if (denominator < 0) {
      numerator *= -1
      denominator *= -1
    }
  }

  companion object {
    val ZERO get() = Rational(0)
    val ONE get() = Rational(1)
    val MINUS_ONE get() = Rational(-1)
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

  override fun canBeUnary() = true

  override fun toUnary(): Unary {
    return Unary(listOf(toRational()))
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(listOf(toUnary()))
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
    var j = i
    var r = ONE
    val t = if (j == 0) return ONE
    else if (j < 0) {
      j = -j
      reciprocal()
    } else this
    for (a in 0 until j) {
      r *= t
    }
    return r
  }

  override fun hashCode(): Int {
    var result = numerator.hashCode()
    result = 31 * result + denominator.hashCode()
    return result
  }

  override fun copy() = toRational()

  override fun substitute(entries: Map<Letter, TermBase>) = this

  override fun approximation() = this

  fun factorization(): Unary {
    return Unary(divisors(numerator).map(::Rational), divisors(denominator).map(::Rational))
  }

  private fun divisors(long: Long): MutableList<Long> {
    var n = long
    var i = 2L
    val result = mutableListOf<Long>()
    if (n < 0L) {
      n *= -1L
      result += -1L
    }
    while (true) {
      if (i * i > n) {
        result += n
        break
      }
      if (n % i == 0L) {
        result += i
        n /= i
      } else i++
    }
    return result
  }
}