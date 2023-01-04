package io.github.arashiyama11

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

  override fun substitute(entries: Map<Letter, TermBase>)=
    Polynomial(unaries.map { it.substitute(entries) })

  fun evaluate(): Polynomial {
    if (unaries.isEmpty()) return Polynomial(listOf())
    val result = Polynomial(unaries.filter { !it.isZero() }.flatMap { it.evaluate().toPolynomial().unaries }).arranged()
    return if (result.unaries.size == 1) {
      result.unaries[0].toPolynomial()
    } else result
  }

  override fun approximation()
    =Polynomial(unaries.map { it.approximation() })


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

  //this=0Ç∆ÇµÇƒâÇãÅÇﬂÇÈ
  //é¿êîîÕàÕÇÃÇ›
  /*fun solve(letter: Char = 'x'): List<Polynomial>? {
    val terms = evaluate().simplify().unaries.map { it.toTerm() }
    val deg = terms.maxOf { Unary -> Unary.letters.maxOfOrNull { it.value } ?: 0 }
    //ç~Ç◊Ç´Ç…ÇµÇƒãÛÇ¢ÇƒÇ¢ÇÈéüêîÇ0ñÑÇﬂ
    val cor = List(deg + 1) { d ->
      terms.find { it.letters[letter] == d || d == 0 && it.letters.isEmpty() || it.letters[letter] == null }
        ?.div(Unary(Rational.ONE, mapOf(letter to d)))
        ?: Unary.ZERO
    }.reversed()
    //éÆÇÃéüêî
    return when (deg) {
      0 -> null // ëSÇƒÇÃêî
      1 ->
        //ax+b=0
        //x=-b/a
        listOf((cor[1] / cor[0] * Rational.MINUS_ONE).toPolynomial())
      2 -> {
        val pols = factorization().termBases
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
        if (facted.termBases.size == 1) {
          null
        } else {
          facted.termBases.flatMap { it.toPolynomial().solve(letter) ?: emptyList() }
        }
      }
    }
  }

  //àˆêîï™â
  //óLóùêîîÕàÕ
  fun factorization(): Unary {
    val terms = unaries.map { it.toTerm() }
    if (terms.isEmpty()) return Unary.ZERO
    //letterÇÕç≈çÇéüÇÃï∂éö                Ç±ÇÃtoMutableMapÇ™Ç»Ç¢Ç∆éQè∆ÇÃñ‚ëËÇ≈ÉoÉOÇÈ
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

    //àÍéüéÆà»â∫Ç»ÇÁÇªÇÃÇ‹Ç‹ï‘Ç∑
    if (terms.maxOf { it.letters[letter] ?: 0 } <= 1) {
      return Unary("(${terms.joinToString("+")})")
    }


    //íËêîÇ≈Ç≠Ç≠ÇÍÇÈÇ©
    val mt = Rational(
      terms.map { abs(it.coefficient.numerator) }.reduce { acc, l -> gcd(acc, l) },
      terms.map { abs(it.coefficient.denominator) }.reduce { acc, Unary -> acc * Unary / gcd(acc, Unary) }
    )

    if (mt.numerator != 1L || mt.denominator != 1L) {
      val d = Unary(mt)
      return Unary(listOf(d)) * Polynomial(terms.map { (it / d).toUnary() }).factorization()
    }
    //åWêîÇ™1ÇÃçÄÇ≈Ç≠Ç≠ÇÍÇÈÇ©
    val minDeg = terms.minOf { it.letters[letter] ?: 0 }
    if (minDeg > 0) {
      val d = Unary(Rational.ONE, mapOf(letter to minDeg))
      return d.toUnary() * Polynomial(terms.map { (it / d).toUnary() }).factorization()
    }

    val const = terms.find { it.letters.isEmpty() || it.letters[letter] == null } ?: Unary.ZERO
    val max = terms.reduce { acc, Unary -> if ((Unary.letters[letter] ?: 0) > (acc.letters[letter] ?: 0)) Unary else acc }

    //ï™ïÍÇÕïKÇ∏1Ç…Ç»Ç¡ÇƒÇ¢ÇÈ
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
      .sortedBy {
        if (it.canBeUnary()) it.toUnary().letters[Letter('x')]?.times(-1)?:0 else 0
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

  fun arranged(letter: Char = 'x'): Polynomial {
    var res= mutableListOf<Unary>()
    unaries.forEach {unary->
      val i=res.indexOfFirst { unary.hasSameFuncAndLetter(it) }
      if(i==-1){
        res+=unary
      }else{
        res[i]+=unary
      }
    }
    return Polynomial(res)

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

    //ä÷êîìØémÇÃââéZÇÃèàóù
    //logÇÃèàóù
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

  operator fun times(unary: Unary):Polynomial{
    return Polynomial(unaries.map{ it * unary })
  }

  operator fun times(Double: Double): Polynomial {
    return Polynomial(unaries.map { it * Double })
  }

  operator fun times(rational: Rational): Polynomial {
    return Polynomial(unaries.map { it * rational })
  }

  operator fun times(letter:Letter):Polynomial{
    return Polynomial(unaries.map{it*letter})
  }

  operator fun times(func:Func):Polynomial{
    return Polynomial(unaries.map{it*func})
  }

  operator fun times(other:ExpressionUnit):Polynomial{
    return Polynomial(unaries.map{it*other})
  }

  override fun times(other: TermBase): TermBase {
    return when(other){
      is ExpressionUnit->times(other)
      is Unary->times(other)
      is Polynomial->times(other)
      else->throw Exception("")
    }
  }

  operator fun div(Double: Double): Polynomial {
    return times(1 / Double)
  }

  operator fun div(Unary: Unary): Polynomial {
    return times(Unary.reciprocal())
  }

  //ó]ÇËÇ™0ÇÃÇ∆Ç´ÇæÇØï‘Ç∑
  operator fun div(pol: Polynomial): Polynomial {
    val (res, t) = divSafe(pol)
    return if (t.isZero()) res else Unary("(${toString()})/(${pol})").toPolynomial()
  }

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

  fun divSafe(pol:Polynomial):Pair<Polynomial,Polynomial>{
    if(canBeUnary()&&pol.canBeUnary())return (toUnary()/pol.toUnary()).toPolynomial() to ZERO
    val unaries=evaluate().unaries
    val dUnaries=pol.evaluate().unaries
    val letter=dUnaries.map{it.letters.maxByOrNull { it.value }}.maxByOrNull { it?.value?:0 }!!.key
    //letterÇÃÇ›ÇèúÇ¢Çƒç~Ç◊Ç´Ç…Ç∑ÇÈ

    val a=List(unaries.maxOf { it.letters[letter]?:0 }+1){d->
      val us=unaries.filter{it.letters[letter]==d  || d == 0 && it.letters[letter] == null}
      if (us.isEmpty()) return@List ONE
      else us.map{Unary(it.rational,it.letters.filterKeys { k -> k!=letter },it.funcs,it.pols).toPolynomial()}.reduce { acc, p -> acc+p }
    }.reversed().toMutableList()

    val b=List(dUnaries.maxOf { it.letters[letter]?:0 }+1){d->
      val us=dUnaries.filter{it.letters[letter]==d  || d == 0 && it.letters[letter] == null}
      if (us.isEmpty()) return@List ONE
      else us.map{Unary(it.rational,it.letters.filterKeys { k -> k!=letter },it.funcs,it.pols).toPolynomial()}.reduce { acc, p -> acc+p }
    }.reversed().toMutableList()

    val result= mutableListOf<TermBase>()
    for (i in 0 .. a.size - b.size) {
      val r = Unary(a[i].evaluate(),b[0]).evaluate()
      result += r
      val mi = b.map { it * r }
      mi.forEachIndexed { index, p ->
        a[index + i] = (a[index + i] + -p.toPolynomial()).evaluate()
      }
    }

    a.reverse()
    result.reverse()

    return List(result.size) {
      result[it].toPolynomial() * Unary(lts=mapOf(letter to it))
    }.reduce { acc, p -> acc + p }.evaluate() to
      List(a.size) {
        a[it].toPolynomial() * Unary(lts=mapOf(letter to it))
      }.reduce { acc, p -> acc + p }.evaluate()
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

  fun canBeRational()=canBeUnary()&&toUnary().canBeRational()

  override fun canBeUnary()=unaries.size==1

  override fun toUnary(): Unary {
    if (unaries.size != 1) throw ClassCastException("Cannot be unary")
    return unaries[0]
  }

  override fun hashCode()=unaries.hashCode()

  override fun copy()=toPolynomial()

  fun reciprocal()=Unary(dp = toPolynomial())
}