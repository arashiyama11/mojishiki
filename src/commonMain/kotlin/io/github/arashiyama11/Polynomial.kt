package io.github.arashiyama11
import kotlin.math.*

class Polynomial(polynomialString: String) : TermBase() {
  var unaries: List<Unary>

  init {
    unaries = parse(polynomialString)
  }

  companion object {
    val ZERO = Term.ZERO.toPolynomial()
    val ONE = Term.ONE.toPolynomial()
    val MINUS_ONE = Term.MINUS_ONE.toPolynomial()
  }

  constructor(us: List<Unary>) : this("") {
    unaries = us
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(unaries.map { it.toUnary() })
  }

  fun substitute(arg: Map<Char, Rational>): Polynomial {
    return Polynomial(unaries.map { it.substitute(arg) })
  }

  fun evaluate(): Polynomial {
    if (unaries.isEmpty()) return Polynomial(listOf())
    val result = Polynomial(unaries.filter { !it.isZero() }.flatMap { it.evaluate().toPolynomial().unaries }).arranged()
    return if (result.unaries.size == 1) {
      result.unaries[0].toPolynomial()
    } else result
  }

  fun approximation(): Polynomial {
    return Polynomial(unaries.map { it.approximation() })
  }

  //®‚ÌŒW”‚ğŠÈ’P‚É‚·‚é
  fun simplify(): Polynomial {
    val terms = arranged().unaries.map { it.toTerm() }
    val mt = Rational(
      terms.map { abs(it.coefficient.denominator) }.reduce { acc, term -> acc * term / gcd(acc, term) },
      terms.map { abs(it.coefficient.numerator) }.reduce { acc, l -> gcd(acc, l) }
    )
    return Polynomial(terms.map { (it * mt).toUnary() })
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

  //this=0‚Æ‚µ‚Ä‰ğ‚ğ‹‚ß‚é
  //À””ÍˆÍ‚Ì‚İ
  fun solve(letter: Char = 'x'): List<Polynomial>? {
    val terms = evaluate().simplify().unaries.map { it.toTerm() }
    val deg = terms.maxOf { term -> term.letters.maxOfOrNull { it.value } ?: 0 }
    //~‚×‚«‚É‚µ‚Ä‹ó‚¢‚Ä‚¢‚éŸ”‚ğ0–„‚ß
    val cor = List(deg + 1) { d ->
      terms.find { it.letters[letter] == d || d == 0 && it.letters.isEmpty() || it.letters[letter] == null }
        ?.div(Term(Rational.ONE, mapOf(letter to d)))
        ?: Term.ZERO
    }.reversed()
    //®‚ÌŸ”
    return when (deg) {
      0 -> null // ‘S‚Ä‚Ì”
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
            Term(
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

  //ˆö”•ª‰ğ
  //—L—””ÍˆÍ
  fun factorization(): Unary {
    val terms = unaries.map { it.toTerm() }
    if (terms.isEmpty()) return Unary.ZERO
    //letter‚ÍÅ‚Ÿ‚Ì•¶š                ‚±‚ÌtoMutableMap‚ª‚È‚¢‚ÆQÆ‚Ì–â‘è‚ÅƒoƒO‚é
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
      ).map { Term(it.toString()) },
        divisors(terms[0].coefficient.denominator).map { Term(it.toString()) }
      )

    //ˆêŸ®ˆÈ‰º‚È‚ç‚»‚Ì‚Ü‚Ü•Ô‚·
    if (terms.maxOf { it.letters[letter] ?: 0 } <= 1) {
      return Unary("(${terms.joinToString("+")})")
    }


    //’è”‚Å‚­‚­‚ê‚é‚©
    val mt = Rational(
      terms.map { abs(it.coefficient.numerator) }.reduce { acc, l -> gcd(acc, l) },
      terms.map { abs(it.coefficient.denominator) }.reduce { acc, term -> acc * term / gcd(acc, term) }
    )

    if (mt.numerator != 1L || mt.denominator != 1L) {
      val d = Term(mt)
      return Unary(listOf(d)) * Polynomial(terms.map { (it / d).toUnary() }).factorization()
    }
    //ŒW”‚ª1‚Ì€‚Å‚­‚­‚ê‚é‚©
    val minDeg = terms.minOf { it.letters[letter] ?: 0 }
    if (minDeg > 0) {
      val d = Term(Rational.ONE, mapOf(letter to minDeg))
      return d.toUnary() * Polynomial(terms.map { (it / d).toUnary() }).factorization()
    }

    val const = terms.find { it.letters.isEmpty() || it.letters[letter] == null } ?: Term.ZERO
    val max = terms.reduce { acc, term -> if ((term.letters[letter] ?: 0) > (acc.letters[letter] ?: 0)) term else acc }

    //•ª•ê‚Í•K‚¸1‚É‚È‚Á‚Ä‚¢‚é
    if (const.coefficient.denominator != 1L || max.coefficient.denominator != 1L) throw PolynomialDivBy1DException("Highest order denominator must be 1")

    val cdivs = (divisors(const.coefficient.numerator) + 1)
      .flatMap { listOf(it, -it) }.filter { it != 0L }.distinct().sortedBy { abs(it) }
    var clts = listOf(Term.ONE, Term.MINUS_ONE)
    const.letters.entries.forEach { (c, i) ->
      clts = if (clts.isEmpty()) List(i + 1) { Term(Rational.ONE, mapOf(c to it)) }
      else clts.flatMap { t -> List(i + 1) { t * Term(Rational.ONE, mapOf(c to it)) } }
    }
    val mdivs = (divisors(max.coefficient.numerator) + 1).distinct().sortedBy { abs(it) }
    var mlts = listOf(Term.ONE)
    max.letters.entries.filter { it.key != letter }.forEach { (c, i) ->
      mlts = if (mlts.isEmpty()) List(i + 1) { Term(Rational.ONE, mapOf(c to it)) }
      else mlts.flatMap { t -> List(i + 1) { t * Term(Rational.ONE, mapOf(c to it)) } }
    }

    val cs = cdivs.flatMap { a -> clts.map { it * Rational(a) } }
    val ms = mdivs.flatMap { a -> mlts.map { it * Rational(a) * Term(Rational.ONE, mapOf(letter to 1)) } }
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
    return Polynomial(terms.map { term ->
      if (term.functions.size == 1)
        term.functions.forEach { (t, u) ->
          return@map Unary(
            listOf(
              u.args[0].toPolynomial().differential(), specialFunctions[t]!!.differential!!.invoke(u.args)
                .toPolynomial().evaluate(), Term(term.coefficient)
            )
          ).evaluate().toUnary()
        }

      val d = term.letters[letter] ?: return@map Unary.ZERO
      Term(term.coefficient * d, term.letters.mapValues { (k, v) ->
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
      Term(
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
    }.toList()) + Term(Rational.ONE, mapOf('C' to 1)).toPolynomial()
  }

  override fun toString(): String {
    val us = unaries.filter { !it.isZero() }
    if (us.isEmpty()) return "0"
    return us
      .sortedBy {
        if (it.canBeTerm()) it.toTerm().letters['x']?.times(-1) ?: 0 else 0
      }
      .mapIndexed { index, it ->
        val s = it.toString()
        if (index == 0 || s.isEmpty() || s[0] == '-') s else "+$s"
      }
      .joinToString("")
  }

  override fun toStringWith(options: Set<String>): String {
    val us = unaries.filter { !it.isZero() }
    if (us.isEmpty()) return "0"
    return us
      .sortedBy {
        if (it.canBeTerm()) it.toTerm().letters['x']?.times(-1) ?: 0 else 0
      }
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
          //-‚Ì‚Æ‚«‚Íæ“ª‚É-‚ğŠÜ‚Ü‚¹‚é‚Ì‚Å1‘«‚³‚È‚¢
          j = i
        }
      }
    }
    unaryStrings += input.substring(j).trim()
    return unaryStrings.map { Unary(it) }.filter { it.polynomials.isNotEmpty() }
  }

  fun arranged(letter: Char = 'x'): Polynomial {
    var us = mutableListOf<Term>()
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

    //ŠÖ”“¯m‚Ì‰‰Z‚Ìˆ—
    //log‚Ìˆ—
    val logs = us.filter { it.functions.containsKey("log") }
    if (logs.size > 1) {
      val arg = logs.map {
        it.functions["log"]!!.args[0].toPolynomial().pow(it.coefficient.toInt())
      }.reduce { acc, pol -> acc * pol }.evaluate()
      us = (us.filterNot { it.functions.containsKey("log") } + Term(
        Rational.ONE,
        null,
        mapOf("log" to FunctionValue(1, listOf(arg)))
      )).toMutableList()
    }

    return Polynomial(us.filter { it.coefficient.numerator != 0L }.sortedBy { it.letters[letter]?.times(-1) }
      .map { it.toUnary() } + (a[false] ?: emptyList()))
  }

  operator fun plus(term: Term): Polynomial {
    return Polynomial(unaries + term.toUnary())
  }

  operator fun plus(pol: Polynomial): Polynomial {
    val result = unaries.flatMap { it.evaluate().toPolynomial().unaries.map { t -> t.toTerm() } }.toMutableList()
    pol.unaries.forEach { unary ->
      val t = unary.evaluate().toPolynomial()
      t.unaries.map { it.toTerm() }.forEach {
        val i = result.indexOfFirst { t -> t.letters == it.letters }
        if (i == -1) {
          result += it
        } else {
          result[i] += it
        }
      }
    }
    return Polynomial(result.map { it.toUnary() })
  }

  operator fun minus(term: Term): Polynomial {
    return plus(-term)
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

  operator fun times(term: Term): Polynomial {
    return Polynomial(unaries.map { it * term })
  }

  operator fun div(Double: Double): Polynomial {
    return times(1 / Double)
  }

  operator fun div(term: Term): Polynomial {
    return times(term.reciprocal())
  }

  //—]‚è‚ª0‚Ì‚Æ‚«‚¾‚¯•Ô‚·
  operator fun div(pol: Polynomial): Polynomial {
    val (res, t) = divSafe(pol) ?: return Unary("(${toString()})/(${pol})").toPolynomial()
    return if (t.isZero()) {
      res
    } else {
      Unary("(${toString()})/(${pol})").toPolynomial()
    }
  }

  operator fun unaryPlus(): Polynomial {
    return toPolynomial()
  }

  operator fun unaryMinus(): Polynomial {
    return toPolynomial() * Term.MINUS_ONE
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
        r = Unary(listOf(Term.ONE), listOf(toPolynomial())).toPolynomial()
        t = Unary(listOf(Term.ONE), listOf(toPolynomial())).toPolynomial()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r.evaluate()
  }

  //¤‚Æ—]‚è‚ğ•Ô‚·
  fun divSafe(pol: Polynomial): Pair<Polynomial, Polynomial>? {
    if (pol.canBeTerm()) return this / pol.toTerm() to Polynomial("0")
    if (canBeTerm() && pol
        .canBeTerm()
    ) return (toTerm() / pol.toTerm()).toPolynomial() to Polynomial("0")
    val terms = evaluate().unaries.map { it.toTerm() }
    val dTerms = pol.evaluate().unaries.map { it.toTerm() }
    //’–Ú‚·‚é•¶š‚ÍŠ„‚é•û‚ÌÅ‚Ÿ‚Ì•¶š
    val dds = dTerms.map { it.letters.values.maxOrNull() }
    //Š„‚é•û‚Ì®‚ÌŸ”‚ª‚·‚×‚Ä“¯‚¶‚Æ‚«‚ÍŠ„‚ç‚ê‚é•û‚ÌÅ‚Ÿ‚É‚·‚é
    val letter = if (dds.groupBy { it }.size == 1) {
      val maxTerm = terms.maxBy { it.letters.values.maxOrNull() ?: 0 }
      maxTerm.letters.entries.maxBy { it.value }.key
    } else {
      val maxTerm = dTerms.maxBy { it.letters.values.maxOrNull() ?: 0 }
      maxTerm.letters.entries.maxBy { it.value }.key
    }

    //x‚Ì‚İ‚ğœ‚¢‚Ä~‚×‚«‚É‚·‚é
    val a = List(terms.size) { d ->
      val ts = terms.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (ts.isEmpty()) return@List ONE
      else ts.map { it.toPolynomial() / Term(Rational.ONE, mapOf(letter to d)) }
        .reduce { acc, pol -> acc + pol }.toPolynomial()
    }.reversed().toMutableList()

    val b = List(dTerms.size) { d ->
      val ts = dTerms.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (ts.isEmpty()) return@List ONE
      else ts.map { it.toPolynomial() / Term(Rational.ONE, mapOf(letter to d)) }
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
      result[it].toPolynomial() * Term(
        Rational.ONE,
        mapOf(letter to it)
      )
    }.reduce { acc, p -> acc + p },
      List(a.size) {
        a[it].toPolynomial() * Term(
          Rational.ONE,
          mapOf(letter to it)
        )
      }.reduce { acc, p -> acc + p }
    )
  }

  private class PolynomialDivBy1DException(e: String) : Exception(e)

  //ˆêŸ®‚Å‚í‚é
  //Œ‹‰Ê‚Æ—]‚è‚ğ•Ô‚·
  //nx}m‚ÌŒ`‚Ì‚İ
  fun divBy1D(pol: Polynomial, letter: Char = 'x'): Pair<Polynomial, Term> {
    var terms = pol.evaluate().unaries.map { it.toTerm() }
    if (terms.maxOf { it.letters[letter] ?: 0 } != 1) {
      throw PolynomialDivBy1DException("The arg is not 1D")
    }

    //‘g‚İ—§‚Äœ–@‚Å‚Æ‚­
    //x‚ÌŒW”‚ª1‚Å‚È‚¢‚Í’è””{‚µ‚Ä1‚É‚µ‚Ä‚©‚çŒvZ‚µAÅŒã‚ÉŒ³‚É–ß‚·
    val oneDegCoef = terms.find { it.letters[letter] == 1 }!!.coefficient.reduction()
    terms = terms.map { it * oneDegCoef.reciprocal() }
    //‘g‚İ—§‚Äœ–@‚Ìã‚Ì’i‚©‚ç‡‚Éa,b result

    val dived = evaluate().unaries.map { it.toTerm() }
    //Š„‚ç‚ê‚é‚Ù‚¤‚Ì®‚ÌŸ”
    val divedMaxDeno = dived.maxOf { it.letters[letter] ?: 0 }
    val t =
      terms.find { it.letters.isEmpty() || it.letters[letter] == 0 || it.letters[letter] == null }!!.times(-1.0)
    val a = List(divedMaxDeno + 1) { d ->
      //x‚Ì‚İ‚ğœ‚¢‚½‚©‚ñ‚¶‚É‚·‚é
      val ts = dived.filter { it.letters[letter] == d || d == 0 && it.letters[letter] == null }
      if (ts.isEmpty()) Term.ZERO
      else ts.map { it.toPolynomial() / Term(Rational.ONE, mapOf(letter to d)) }
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
        tb.toPolynomial() / Term(oneDegCoef) * Term(
          Rational.ONE,
          mapOf(letter to divedMaxDeno - index - 1)
        )
      }
    return Pair(
      s.reduceOrNull { acc, cur -> acc + cur } ?: ZERO,
      result.last().toPolynomial().evaluate().unaries.filter { !it.isZero() }
        .reduceOrNull { acc, cur -> acc * cur }?.evaluate()?.toPolynomial()?.unaries?.get(0)?.toTerm() ?: Term.ZERO

    )
  }

  override fun toTerm(): Term {
    if (unaries.isEmpty()) return Term.ZERO
    if (unaries.size != 1) throw ClassCastException("Cannot be term")
    return unaries[0].toUnary().toTerm()
  }

  override fun canBeTerm(): Boolean {
    if (unaries.isEmpty()) return true
    if (unaries.size != 1) return false
    return unaries[0].canBeTerm()
  }

  override fun equals(other: Any?): Boolean {
    if (other is Polynomial) {
      val a = unaries.filter { !it.isOne() }
      val b = other.unaries.filter { !it.isOne() }
      return a.size == b.size && a.containsAll(b)
    }
    return false
  }

  override fun isZero(): Boolean {
    return unaries.all { it.isZero() }
  }

  override fun isOne(): Boolean {
    val t = evaluate()
    if (t.unaries.size != 1) return false
    return t.unaries[0].isOne()
  }

  override fun toUnary(): Unary {
    if (unaries.size != 1) throw ClassCastException("Cannot be unary")
    return unaries[0]
  }

  override fun hashCode()=unaries.hashCode()
}