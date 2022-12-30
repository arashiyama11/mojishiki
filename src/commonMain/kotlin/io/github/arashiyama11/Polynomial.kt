package io.github.arashiyama11
import kotlin.math.*

class Polynomial(polynomialString: String) : TermBase() {
  var unaries: List<Unary>

  init {
    unaries = parse(polynomialString)
  }

  companion object {
    val ZERO get() = Rational.ZERO.toPolynomial()
    val ONE get() = Rational.ONE.toPolynomial()
    val MINUS_ONE get() = Rational.MINUS_ONE.toPolynomial()
  }

  constructor(us: List<Unary>) : this("") {
    unaries = us
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(unaries.map { it.toUnary() })
  }

  /*fun substitute(arg: Map<Char, Rational>): Polynomial {
    return Polynomial(unaries.map { it.substitute(arg) })
  }*/

  fun evaluate(): Polynomial {
    if (unaries.isEmpty()) return Polynomial(listOf())
    val result = Polynomial(unaries.filter { !it.isZero() }.flatMap { it.evaluate().toPolynomial().unaries }).arranged()
    return if (result.unaries.size == 1) {
      result.unaries[0].toPolynomial()
    } else result
  }

  /*fun approximation(): Polynomial {
    return Polynomial(unaries.map { it.approximation() })
  }*/

  //式の係数を簡単にする
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

  //this=0として解を求める
  //実数範囲のみ
  /*fun solve(letter: Char = 'x'): List<Polynomial>? {
    val terms = evaluate().simplify().unaries.map { it.toTerm() }
    val deg = terms.maxOf { Unary -> Unary.letters.maxOfOrNull { it.value } ?: 0 }
    //降べきにして空いている次数を0埋め
    val cor = List(deg + 1) { d ->
      terms.find { it.letters[letter] == d || d == 0 && it.letters.isEmpty() || it.letters[letter] == null }
        ?.div(Unary(Rational.ONE, mapOf(letter to d)))
        ?: Unary.ZERO
    }.reversed()
    //式の次数
    return when (deg) {
      0 -> null // 全ての数
      1 ->
        //ax+b=0
        //x=-b/a
        listOf((cor[1] / cor[0] * Rational.MINUS_ONE).toPolynomial())
      2 -> {
        val pols = factorization().polynomials
        if (pols.size == 1) {
          val a = cor[0]
          val b = cor[1]
          val c = cor[2]
          val d =
            Unary(
              Rational.ONE,
              null,
              mapOf("sqrt" to FunctionValue(1, listOf((b * b).toPolynomial() + (a * c * -4.0).toPolynomial())))
            ).toPolynomial()
          val da = a * 2.0
          val mb = -b.toPolynomial()
          listOf(
            Unary(listOf(Polynomial(mb.unaries + d.unaries).evaluate()), listOf(da)).toPolynomial(),
            Unary(listOf(Polynomial(mb.unaries + (-d).unaries).evaluate()), listOf(da)).toPolynomial()
          )
        } else {
          pols.flatMap { it.toPolynomial().solve(letter) ?: emptyList() }
        }
      }
      else -> {
        val facted = factorization()
        if (facted.polynomials.size == 1) {
          null
        } else {
          facted.polynomials.flatMap { it.toPolynomial().solve(letter) ?: emptyList() }
        }
      }
    }
  }

  //因数分解
  //有理数範囲
  fun factorization(): Unary {
    val terms = unaries.map { it.toTerm() }
    if (terms.isEmpty()) return Unary.ZERO
    //letterは最高次の文字                このtoMutableMapがないと参照の問題でバグる
    val letter = terms.map { it.letters.toMutableMap() }.reduceOrNull { acc, mutableMap ->
      mutableMap.forEach {
        if (acc.containsKey(it.key)) {
          acc[it.key] = acc[it.key]!! + it.value
        } else {
          acc[it.key] = it.value
        }
      }
      acc
    }?.maxByOrNull { it.value }?.key
      ?: return if (terms.any { it.functions.isNotEmpty() })
        Unary("(${terms.joinToString("+")})")
      else Unary(divisors(
        terms[0].coefficient.numerator
      ).map { Unary(it.toString()) },
        divisors(terms[0].coefficient.denominator).map { Unary(it.toString()) }
      )

    //一次式以下ならそのまま返す
    if (terms.maxOf { it.letters[letter] ?: 0 } <= 1) {
      return Unary("(${terms.joinToString("+")})")
    }


    //定数でくくれるか
    val mt = Rational(
      terms.map { abs(it.coefficient.numerator) }.reduce { acc, l -> gcd(acc, l) },
      terms.map { abs(it.coefficient.denominator) }.reduce { acc, Unary -> acc * Unary / gcd(acc, Unary) }
    )

    if (mt.numerator != 1L || mt.denominator != 1L) {
      val d = Unary(mt)
      return Unary(listOf(d)) * Polynomial(terms.map { (it / d).toUnary() }).factorization()
    }
    //係数が1の項でくくれるか
    val minDeg = terms.minOf { it.letters[letter] ?: 0 }
    if (minDeg > 0) {
      val d = Unary(Rational.ONE, mapOf(letter to minDeg))
      return d.toUnary() * Polynomial(terms.map { (it / d).toUnary() }).factorization()
    }

    val const = terms.find { it.letters.isEmpty() || it.letters[letter] == null } ?: Unary.ZERO
    val max = terms.reduce { acc, Unary -> if ((Unary.letters[letter] ?: 0) > (acc.letters[letter] ?: 0)) Unary else acc }

    //分母は必ず1になっている
    if (const.coefficient.denominator != 1L || max.coefficient.denominator != 1L) throw PolynomialDivBy1DException("Highest order denominator must be 1")

    val cdivs = (divisors(const.coefficient.numerator) + 1)
      .flatMap { listOf(it, -it) }.filter { it != 0L }.distinct().sortedBy { abs(it) }
    var clts = listOf(Unary.ONE, Unary.MINUS_ONE)
    const.letters.entries.forEach { (c, i) ->
      clts = if (clts.isEmpty()) List(i + 1) { Unary(Rational.ONE, mapOf(c to it)) }
      else clts.flatMap { t -> List(i + 1) { t * Unary(Rational.ONE, mapOf(c to it)) } }
    }
    val mdivs = (divisors(max.coefficient.numerator) + 1).distinct().sortedBy { abs(it) }
    var mlts = listOf(Unary.ONE)
    max.letters.entries.filter { it.key != letter }.forEach { (c, i) ->
      mlts = if (mlts.isEmpty()) List(i + 1) { Unary(Rational.ONE, mapOf(c to it)) }
      else mlts.flatMap { t -> List(i + 1) { t * Unary(Rational.ONE, mapOf(c to it)) } }
    }

    val cs = cdivs.flatMap { a -> clts.map { it * Rational(a) } }
    val ms = mdivs.flatMap { a -> mlts.map { it * Rational(a) * Unary(Rational.ONE, mapOf(letter to 1)) } }
    for (m in ms) {
      for (c in cs) {
        val d = (m.toPolynomial() + c.toPolynomial()).evaluate()
        val p = Polynomial(terms.map { it.toUnary() }).divBy1D(d, letter)
        if (p.second.coefficient.numerator == 0L) {
          return Unary(listOf(d)) * Polynomial(p.first.unaries.filter { !it.isZero() }).factorization()
        }
      }
    }
    return Unary("(${terms.joinToString("+")})")
  }

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

  override fun toString(): String {
    val us = unaries.filter { !it.isZero() }
    if (us.isEmpty()) return "0"
    return us
      /*.sortedBy {
        if (it.canBeTerm()) it.toTerm().letters['x']?.times(-1) ?: 0 else 0
      }*/
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
          //-のときは先頭に-を含ませるので1足さない
          j = i
        }
      }
    }
    unaryStrings += input.substring(j).trim()
    return unaryStrings.map { Unary(it) }.filter { it.polynomials.isNotEmpty() }
  }

  fun arranged(letter: Char = 'x'): Polynomial {
    /*var us = mutableListOf<>()
    val a = unaries.groupBy { it.canBeTerm() }
    a[true]
      ?.forEach { u ->
        val e = u.toTerm()
        val i = us.indexOfFirst {
          it.letters == e.letters && it.functions == e.functions
        }
        if (i == -1) {
          us += e
        } else {
          us[i] += e
        }
      }

    //関数同士の演算の処理
    //logの処理
    /*val logs = us.filter { it.functions.containsKey("log") }
    if (logs.size > 1) {
      val arg = logs.map {
        it.functions["log"]!!.args[0].toPolynomial().pow(it.coefficient.toInt())
      }.reduce { acc, pol -> acc * pol }.evaluate()
      us = (us.filterNot { it.functions.containsKey("log") } + Unary(
        Rational.ONE,
        null,
        mapOf("log" to FunctionValue(1, listOf(arg)))
      )).toMutableList()
    }*/

    return Polynomial(us
      .filter { it.coefficient.numerator != 0L }
      .sortedBy { it.letters[letter]?.times(-1) }
      .map { it.toUnary() } + (a[false] ?: emptyList()))*/
    return this
  }



  operator fun plus(pol: Polynomial): Polynomial {
    val res=unaries.toMutableList()
    pol.unaries.forEach { unary->
      val i=res.indexOfFirst { it.hasSameFuncAndLetter(unary) }
      if(i==-1){
        res+=unary
      }else{
        res[i]+=unary
      }
    }
    return Polynomial(res)
  }


  operator fun times(pol: Polynomial): Polynomial {
    return Polynomial(unaries.flatMap { pol.unaries.map { u -> u * it } })
  }

  operator fun times(Double: Double): Polynomial {
    return Polynomial(unaries.map { it * Double })
  }

  operator fun times(rational: Rational): Polynomial {
    return Polynomial(unaries.map { it * rational })
  }

  override fun times(other: TermBase): TermBase {
    TODO("Not yet implemented")
  }


  /*operator fun div(Double: Double): Polynomial {
    return times(1 / Double)
  }

  operator fun div(Unary: Unary): Polynomial {
    return times(Unary.reciprocal())
  }

  //余りが0のときだけ返す
  operator fun div(pol: Polynomial): Polynomial {
    val (res, t) = divSafe(pol) ?: return Unary("(${toString()})/(${pol})").toPolynomial()
    return if (t.isZero()) {
      res
    } else {
      Unary("(${toString()})/(${pol})").toPolynomial()
    }
  }*/

  operator fun unaryPlus(): Polynomial {
    return toPolynomial()
  }

  operator fun unaryMinus(): Polynomial {
    return toPolynomial() * Rational.MINUS_ONE
  }

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
        r = Unary(listOf(Rational.ONE), listOf(toPolynomial())).toPolynomial()
        t = Unary(listOf(Rational.ONE), listOf(toPolynomial())).toPolynomial()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r.evaluate()
  }

  //商と余りを返す
  /*fun divSafe(pol: Polynomial): Pair<Polynomial, Polynomial>? {
    if (pol.canBeTerm()) return this / pol.toTerm() to Polynomial("0")
    if (canBeTerm() && pol
        .canBeTerm()
    ) return (toTerm() / pol.toTerm()).toPolynomial() to Polynomial("0")
    val terms = evaluate().unaries.map { it.toTerm() }
    val dTerms = pol.evaluate().unaries.map { it.toTerm() }
    //注目する文字は割る方の最高次の文字
    val dds = dTerms.map { it.letters.values.maxOrNull() }
    //割る方の式の次数がすべて同じときは割られる方の最高次にする
    val letter = if (dds.groupBy { it }.size == 1) {
      val maxTerm = terms.maxBy { it.letters.values.maxOrNull() ?: 0 }
      maxTerm.letters.entries.maxBy { it.value }.key
    } else {
      val maxTerm = dTerms.maxBy { it.letters.values.maxOrNull() ?: 0 }
      maxTerm.letters.entries.maxBy { it.value }.key
    }

    //xのみを除いて降べきにする
    val a = List(terms.size) { d ->
      val ts = terms.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (ts.isEmpty()) return@List ONE
      else ts.map { it.toPolynomial() / Unary(Rational.ONE, mapOf(letter to d)) }
        .reduce { acc, pol -> acc + pol }.toPolynomial()
    }.reversed().toMutableList()

    val b = List(dTerms.size) { d ->
      val ts = dTerms.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (ts.isEmpty()) return@List ONE
      else ts.map { it.toPolynomial() / Unary(Rational.ONE, mapOf(letter to d)) }
        .reduce { acc, pol -> acc + pol }.toPolynomial()
    }.reversed()

    val result = mutableListOf<TermBase>()
    for (i in 0 until a.size - b.size + 1) {
      val r = a[i] / b[0]
      result += r
      val mi = b.map { it * r }
      mi.forEachIndexed { index, p ->
        a[index + i] = a[index + i] + -p
      }
    }
    if (result.isEmpty()) return null

    result.reverse()
    a.reverse()
    return Pair(List(result.size) {
      result[it].toPolynomial() * Unary(
        Rational.ONE,
        mapOf(letter to it)
      )
    }.reduce { acc, p -> acc + p },
      List(a.size) {
        a[it].toPolynomial() * Unary(
          Rational.ONE,
          mapOf(letter to it)
        )
      }.reduce { acc, p -> acc + p }
    )
  }

  private class PolynomialDivBy1DException(e: String) : Exception(e)

  //一次式でわる
  //結果と余りを返す
  //nx±mの形のみ
  fun divBy1D(pol: Polynomial, letter: Char = 'x'): Pair<Polynomial, Unary> {
    var terms = pol.evaluate().unaries.map { it.toTerm() }
    if (terms.maxOf { it.letters[letter] ?: 0 } != 1) {
      throw PolynomialDivBy1DException("The arg is not 1D")
    }

    //組み立て除法でとく
    //xの係数が1でない時は定数倍して1にしてから計算し、最後に元に戻す
    val oneDegCoef = terms.find { it.letters[letter] == 1 }!!.coefficient.reduction()
    terms = terms.map { it * oneDegCoef.reciprocal() }
    //組み立て除法の上の段から順にa,b result

    val dived = evaluate().unaries.map { it.toTerm() }
    //割られるほうの式の次数
    val divedMaxDeno = dived.maxOf { it.letters[letter] ?: 0 }
    val t =
      terms.find { it.letters.isEmpty() || it.letters[letter] == 0 || it.letters[letter] == null }!!.times(-1.0)
    val a = List(divedMaxDeno + 1) { d ->
      //xのみを除いたかんじにする
      val ts = dived.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (ts.isEmpty()) Unary.ZERO
      else ts.map { it.toPolynomial() / Unary(Rational.ONE, mapOf(letter to d)) }
        .reduce { acc, pol -> acc + pol }
    }.reversed()
    val b = mutableListOf<TermBase>()
    val result = mutableListOf(a[0])

    for (i in 0 until divedMaxDeno) {
      b += t * result.last().toPolynomial()
      result += b.last().toPolynomial() + a[i + 1].toPolynomial()
    }

    val s = result.slice(0 until divedMaxDeno)
      .mapIndexed { index, tb ->
        tb.toPolynomial() / Unary(oneDegCoef) * Unary(
          Rational.ONE,
          mapOf(letter to divedMaxDeno - index - 1)
        )
      }
    return Pair(
      s.reduceOrNull { acc, cur -> acc + cur } ?: ZERO,
      result.last().toPolynomial().evaluate().unaries.filter { !it.isZero() }
        .reduceOrNull { acc, cur -> acc * cur }?.evaluate()?.toPolynomial()?.unaries?.get(0)?.toTerm() ?: Unary.ZERO

    )
  }*/



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

  override fun toUnary(): Unary {
    if (unaries.size != 1) throw ClassCastException("Cannot be unary")
    return unaries[0]
  }

  override fun hashCode()=unaries.hashCode()


  override fun copy()=toPolynomial()
}