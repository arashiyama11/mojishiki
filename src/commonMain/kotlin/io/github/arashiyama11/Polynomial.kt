package io.github.arashiyama11

import kotlin.math.abs
import kotlin.math.absoluteValue

class Polynomial(val unaries: List<Unary>) : TermBase() {

  constructor(polynomialString: String) : this(parse(polynomialString))

  constructor(unary: Unary) : this(listOf(unary))

  companion object {
    val ZERO get() = Rational.ZERO.toPolynomial()
    val ONE get() = Rational.ONE.toPolynomial()
    val MINUS_ONE get() = Rational.MINUS_ONE.toPolynomial()

    private fun parse(input: String): List<Unary> {
      var j = 0
      var depth = 0
      val unaryStrings = mutableListOf<String>()
      for (i in input.indices) {
        when (input[i]) {
          '(' -> depth++
          ')' -> depth--
          '+' -> if (depth == 0) {
            unaryStrings += input.substring(j, i).trim()
            j = i + 1
          }
          '-' -> if (depth == 0 && i > 0 && input[i - 1] != '^') {
            unaryStrings += input.substring(j, i).trim()
            //-ÇÃÇ∆Ç´ÇÕêÊì™Ç…-Çä‹Ç‹ÇπÇÈÇÃÇ≈1ë´Ç≥Ç»Ç¢
            j = i
          }
        }
      }
      unaryStrings += input.substring(j).trim()
      return unaryStrings.map { Unary(it) }.filter { it.termBases.isNotEmpty() }
    }
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(unaries.map { it.toUnary() })
  }

  override fun substitute(entries: Map<Letter, TermBase>) =
    Polynomial(unaries.map { it.substitute(entries) })

  fun evaluate(): Polynomial {
    if (unaries.isEmpty()) return Polynomial(listOf())
    val result = Polynomial(unaries.filter { !it.isZero() }.flatMap { it.evaluate().toPolynomial().unaries }).arranged()
    return if (result.unaries.size == 1 && result.unaries[0].termBases.size == 1 && result.unaries[0].denoTermBases.toString() == "1") {
      result.unaries[0].termBases[0].toPolynomial()
    } else result
  }

  override fun approximation() = Polynomial(unaries.map { it.approximation() })


  //éÆÇÃåWêîÇä»íPÇ…Ç∑ÇÈ
  /*fun simplify(): Polynomial {
    val terms = arranged().unaries.map { it.toTerm() }
    val mt = Rational(
      terms.map { abs(it.coefficient.denominator) }.reduce { acc, Unary -> acc * Unary / gcd(acc, Unary) },
      terms.map { abs(it.coefficient.numerator) }.reduce { acc, l -> gcd(acc, l) }
    )
    return Polynomial(terms.map { (it * mt).toUnary() })
  }*/

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
        i = 1L
      }
      i++
    }
    return result
  }

  private tailrec fun gcd(a: Long, b: Long): Long {
    if (b == 0L) return a
    return gcd(b, a % b)
  }

  fun solve(letter: Letter = Letter('x')): List<TermBase?> {
    val fact = factorization()
    val ltsRes = fact.termBases.filterIsInstance<Letter>().any { it == letter }
    val polsRes = fact.termBases.filterIsInstance<Polynomial>().flatMap { pol ->
      val deg = pol.unaries.maxOf { it.letters[letter] ?: 0 }

      val ps = List(deg + 1) { d ->
        val us = pol.unaries.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
        if (us.isEmpty()) return@List ZERO
        else us.map {
          Unary(
            it.rational,
            it.letters.filterKeys { k -> k != letter },
            it.funcs,
            it.polynomials
          ).toPolynomial()
        }
          .reduce { acc, p -> acc + p }
      }.reversed().toMutableList()

      when (deg) {
        0 -> listOf(null)
        1 -> listOf(-Unary(ps[1], ps[0]))
        2 -> {
          val a = ps[0]
          val b = ps[1]
          val c = ps[2]
          //sqrt(b^2-4ac)
          val d = Func("sqrt", b * b - (Rational(4) * a * c).toPolynomial()).toPolynomial()
          val da = a * 2.0
          listOf(Unary(-b + d, da), Unary(-b - d, da))
        }
        else -> listOf(null)
      }
    }
    return if (ltsRes) listOf(Rational.ZERO) + polsRes else polsRes
  }

  /*
  fun differential(letter: Char = 'x'): Polynomial {
    val terms = evaluate().unaries.map { it.toTerm() }
    return Polynomial(terms.map { Unary ->
      if (Unary.functions.size == 1)
        Unary.functions.forEach { (t, u) ->
          return@map Unary(
            listOf(
              u.args[0].toPolynomial().differential(), specialFunctions[t]!!.differential!!.invoke(u.args)
                .toPolynomial().evaluate(), Unary(Unary.coefficient)
            )
          ).evaluate().toUnary()
        }

      val d = Unary.letters[letter] ?: return@map Unary.ZERO
      Unary(Unary.coefficient * d, Unary.letters.mapValues { (k, v) ->
        if (k == letter) {
          v - 1
        } else {
          v
        }
      }).toUnary()
    }.toList())
  }

  fun integral(letter: Char = 'x'): Polynomial {
    val terms = evaluate().unaries.map { it.toTerm() }
    return Polynomial(terms.map {
      if (it.functions.size == 1) {
        it.functions.forEach { (t, u) ->
          return@map specialFunctions[t]!!.integral!!.invoke(u.args).toPolynomial().toUnary()
        }
      }
      val d = it.letters[letter] ?: 0
      Unary(
        it.coefficient / (d + 1),
        if (it.letters.containsKey(letter)) {
          it.letters.mapValues { (k, v) ->
            if (k == letter) {
              v + 1
            } else {
              v
            }
          }
        } else {
          mapOf(
            letter to 1
          ) + it.letters
        }
      ).toUnary()
    }.toList()) + Unary(Rational.ONE, mapOf('C' to 1)).toPolynomial()
  }*/

  fun factorization(): Unary {
    if (canBeUnary()) return toUnary()
    val us = evaluate().unaries.map { it.copy() }
    if (us.isEmpty()) return Unary.ONE
    val letter = us.map { it.letters.maxByOrNull { v -> v.value } }.maxByOrNull { it?.value ?: 0 }?.key ?: return Unary(
      toPolynomial()
    )
    if (us.maxOfOrNull { it.letters.values.maxOrNull() ?: 0 } == 1) return Unary(this)
    val mt = Rational(
      us.map { it.rational.numerator }.reduce { acc, l -> gcd(acc, l) },
      us.map { it.rational.denominator }.reduce { acc, l -> gcd(acc, l) }
    ).reduction()

    if (mt.numerator.absoluteValue != 1L || mt.denominator.absoluteValue != 1L) {
      return mt.factorization() * div(mt).factorization()
    }

    val minDeg = us.minOf { it.letters[letter] ?: 0 }

    if (minDeg > 0) {
      val u = Unary(lts = mapOf(letter to minDeg))
      return u * div(u).factorization()
    }

    //íËêîçÄ
    val const = us.find { !it.letters.containsKey(letter) } ?: Unary.ZERO
    val max = us.reduce { acc, u -> if ((u.letters[letter] ?: 0) > (acc.letters[letter] ?: 0)) u else acc }

    val cdivs = (divisors(const.rational.numerator) + 1).flatMap { listOf(it, -it) }.filter { it != 0L }.distinct()
      .sortedBy { abs(it) }
    var clts = listOf(Unary.ONE, Unary.MINUS_ONE)
    const.letters.entries.forEach { (l, i) ->
      clts = if (clts.isEmpty()) List(i + 1) { Unary(lts = mapOf(l to it)) }
      else clts.flatMap { t -> List(i + 1) { t * Unary(lts = mapOf(l to it)) } }
    }

    val mdivs = (divisors(max.rational.numerator) + 1).flatMap { listOf(it, -it) }.filter { it != 0L }.distinct()
      .sortedBy { abs(it) }
    var mlts = listOf(Unary.ONE, Unary.MINUS_ONE)
    max.letters.entries.forEach { (l, i) ->
      mlts = if (mlts.isEmpty()) List(i + 1) { Unary(lts = mapOf(l to it)) }
      else mlts.flatMap { t -> List(i + 1) { t * Unary(lts = mapOf(l to it)) } }
    }

    val cs = cdivs.flatMap { l -> clts.map { it * Rational(l) } }
    val ms = mdivs.flatMap { l -> clts.map { it * Rational(l) * Unary(lts = mapOf(letter to 1)) } }

    for (m in ms) {
      for (c in cs) {
        val d = (m.toPolynomial() + c.toPolynomial()).evaluate()
        val p = Polynomial(us.map { it.copy() }).divSafe(d)
        if (p.second.isZero()) {
          return Unary(d) * Polynomial(p.first.unaries.filter { !it.isZero() }).factorization()
        }
      }
    }
    return Unary(this)
  }

  override fun toString(): String {
    val us = unaries.filter { !it.isZero() }
    if (us.isEmpty()) return "0"
    return us
      .sortedBy {
        if (it.canBeUnary()) it.toUnary().letters[Letter('x')]?.times(-1) ?: 0 else 0
      }
      .mapIndexed { index, it ->
        val s = it.toString()
        if (index == 0 || s.isEmpty() || s[0] == '-') s else "+$s"
      }
      .joinToString("")
  }

  fun toStringWith(options: Set<String>): String {
    val us = unaries.filter { !it.isZero() }
    if (us.isEmpty()) return "0"
    return us
      /*.sortedBy {
        if (it.canBeTerm()) it.toTerm().letters['x']?.times(-1) ?: 0 else 0
      }*/
      .mapIndexed { index, it ->
        val s = it.toStringWith(options)
        if (index == 0 || s.isEmpty() || s[0] == '-') s else "+$s"
      }
      .joinToString("")
  }

  fun arranged(letter: Char = 'x'): Polynomial {
    val res = mutableListOf<Unary>()
    unaries.forEach { unary ->
      val i = res.indexOfFirst { unary.hasSameFuncAndLetter(it) }
      if (i == -1) {
        res += unary
      } else {
        res[i] += unary
      }
    }

    val funcMap = res.groupBy { u ->
      if (u.funcs.filterKeys { it.name == "log" }.let { it.size == 1 && it.toList()[0].second == 1 }) "log" else null
    }

    if (!funcMap.containsKey("log")) return Polynomial(res)

    val log = Func("log", funcMap["log"]!!.map { u ->
      u.funcs.filterKeys { it.name == "log" }.toList()[0].first.args[0].toPolynomial().pow(u.rational.toInt())
    }.reduce { acc, p -> acc * p }).toUnary()

    return Polynomial(funcMap[null]?.plus(log) ?: listOf(log))
  }

  operator fun plus(double: Double) = plus(Rational(double))

  operator fun plus(int: Int) = plus(Rational(int.toLong()))

  operator fun plus(pol: Polynomial): Polynomial {
    val res = unaries.toMutableList()
    pol.unaries.forEach { unary ->
      val i = res.indexOfFirst { it.hasSameFuncAndLetter(unary) }
      if (i == -1) {
        res += unary
      } else {
        res[i] += unary
      }
    }
    return Polynomial(res)
  }

  operator fun plus(termBase: TermBase) = plus(termBase.toPolynomial())

  operator fun minus(double: Double) = minus(Rational(double))

  operator fun minus(int: Int) = minus(Rational(int.toLong()))

  operator fun minus(pol: Polynomial) = plus(-pol)

  operator fun minus(termBase: TermBase) = minus(termBase.toPolynomial())

  operator fun times(double: Double) = times(Rational(double))

  operator fun times(int: Int) = times(Rational(int.toLong()))

  operator fun times(unary: Unary) = Polynomial(unaries.map { it * unary })

  operator fun times(pol: Polynomial) = Polynomial(unaries.flatMap { pol.unaries.map { u -> u * it } })

  override fun times(other: TermBase): Polynomial {
    return when (other) {
      is ExpressionUnit -> times(other.toUnary())
      is Unary -> times(other)
      is Polynomial -> times(other)
      else -> throw UnknownTermBaseInstanceException()
    }
  }

  operator fun div(double: Double) = div(Rational(double))

  operator fun div(int: Int) = div(Rational(int.toLong()))

  operator fun div(rational: Rational) = times(rational.reciprocal())

  operator fun div(unary: Unary) = times(unary.reciprocal())

  operator fun div(pol: Polynomial): Polynomial {
    val (res, t) = divSafe(pol)
    return if (t.isZero()) res else Unary(toPolynomial(), pol.toPolynomial()).toPolynomial()
  }

  operator fun div(termBase: TermBase) =
    when (termBase) {
      is Polynomial -> div(termBase)
      is Unary -> div(termBase)
      is ExpressionUnit -> div(termBase.toUnary())
      else -> throw UnknownTermBaseInstanceException()
    }

  operator fun unaryPlus() = toPolynomial()

  operator fun unaryMinus() = toPolynomial() * Rational.MINUS_ONE

  fun pow(i: Int): Polynomial {
    var j = i
    val t: Polynomial
    var r: Polynomial
    when (i) {
      0 -> return ONE
      else -> if (i > 0) {
        t = toPolynomial()
        r = toPolynomial()
      } else {
        j = -i
        r = reciprocal().toPolynomial()
        t = r.toPolynomial()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r.evaluate()
  }

  fun divSafe(pol: Polynomial): Pair<Polynomial, Polynomial> {
    if (canBeUnary() && pol.canBeUnary()) return (toUnary() / pol.toUnary()).toPolynomial() to ZERO
    val unaries = evaluate().unaries
    val dUnaries = pol.unaries
    val letter = dUnaries.map { it.letters.maxByOrNull { it.value } }.maxByOrNull { it?.value ?: 0 }!!.key
    //letterÇÃÇ›ÇèúÇ¢Çƒç~Ç◊Ç´Ç…Ç∑ÇÈ

    val a = List(unaries.maxOf { it.letters[letter] ?: 0 } + 1) { d ->
      val us = unaries.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (us.isEmpty()) return@List ZERO
      else us.map {
        Unary(
          it.rational,
          it.letters.filterKeys { k -> k != letter },
          it.funcs,
          it.polynomials
        ).toPolynomial()
      }
        .reduce { acc, p -> acc + p }
    }.reversed().toMutableList()

    val b = List(dUnaries.maxOf { it.letters[letter] ?: 0 } + 1) { d ->
      val us = dUnaries.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (us.isEmpty()) return@List ZERO
      else us.map {
        Unary(
          it.rational,
          it.letters.filterKeys { k -> k != letter },
          it.funcs,
          it.polynomials
        ).toPolynomial()
      }
        .reduce { acc, p -> acc + p }
    }.reversed().toMutableList()

    val result = mutableListOf<TermBase>()
    for (i in 0..a.size - b.size) {
      val r = Unary(a[i], b[0])//.evaluate()
      result += r
      val mi = b.map { it * r }
      mi.forEachIndexed { index, p ->
        a[index + i] = (a[index + i] + -p.toPolynomial()).evaluate()
      }
    }

    a.reverse()
    result.reverse()

    return List(result.size) {
      result[it].toPolynomial() * Unary(lts = mapOf(letter to it))
    }.reduce { acc, p -> acc + p }.evaluate() to
        List(a.size) {
          a[it].toPolynomial() * Unary(lts = mapOf(letter to it))
        }.reduce { acc, p -> acc + p }//.evaluate()
  }

  override fun equals(other: Any?): Boolean {
    if (other is Polynomial) {
      val a = unaries.filter { !it.isOne() }
      val b = other.unaries.filter { !it.isOne() }
      return a.size == b.size && a.containsAll(b)
    }
    return false
  }

  fun isZero(): Boolean {
    return unaries.all { it.isZero() }
  }

  fun isOne(): Boolean {
    val t = evaluate()
    if (t.unaries.size != 1) return false
    return unaries[0].isOne()
  }

  fun canBeRational() = canBeUnary() && toUnary().canBeRational()

  override fun canBeUnary() = unaries.size == 1

  override fun toUnary(): Unary {
    if (unaries.size != 1) throw ClassCastException("Cannot be unary")
    return unaries[0]
  }

  override fun hashCode() = unaries.hashCode()

  override fun copy() = toPolynomial()

  fun reciprocal() = Unary(dt = toPolynomial())
}