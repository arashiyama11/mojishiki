package io.github.arashiyama11
import kotlin.math.floor
import kotlin.math.pow

class Unary(unaryString: String) {
  var polynomials: List<TermBase>
  var denoPolynomial: List<TermBase>

  init {
    val (p, d) = parse(unaryString)
    polynomials = p
    denoPolynomial = d
  }

  constructor(ps: List<TermBase>, dps: List<TermBase>? = null) : this("") {
    polynomials = ps
    denoPolynomial = dps ?: listOf(Term.ONE)
  }

  companion object {
    val ZERO = Term.ZERO.toUnary()
    val ONE = Term.ONE.toUnary()
    val MINUS_ONE = Term.MINUS_ONE.toUnary()
  }

  //*で結合される式のリストを返す
  private fun parse(input: String): Pair<List<TermBase>, List<TermBase>> {
    //処理汚いから余裕あるときに直す
    if (input.isEmpty()) return emptyList<TermBase>() to emptyList()
    val fs = validFunctions.joinToString("|")
    var str = input.trim()
    var a = 0
    val regex = Regex("^\\)($fs).*")
    //*の補完
    while (a < str.length) {
      if (a > 0 && str[a] == '(' && !validFunctions.any { str.substring(0, a).endsWith(it) }) {
        val char = str[a - 1]
        if (char.isLetterOrDigit() || char == ')') {
          str = str.substring(0, a) + "*" + str.substring(a)
        }
      }
      //fn()fn()をfn()*fn()に
      if (str.substring(a).matches(regex)) {
        str = str.substring(0, a + 1) + "*" + str.substring(a + 1)
      }

      if (str[a] == '^') {
        var isBaseBrk = false
        var isFn = false
        val base = if (str[a - 1] == ')') {
          //カッコの開きが関数
          isBaseBrk = true
          var depth = 0
          val r = str.substring(0, a - 1).takeLastWhile {
            when (it) {
              ')' -> {
                depth++
                true
              }
              '(' -> if (depth == 0) false else {
                depth--
                true
              }
              else -> true
            }
          }
          val s = str.substring(0, a - r.length - 2)
          val f = validFunctions.find { s.endsWith(it) }
          if (f != null) {
            isFn = true
            "$f($r)"
          } else r
        } else str.substring(0, a).takeLastWhile {
          it.isLetterOrDigit()
        }
        var isDBrk = false
        val d = if (str[a + 1] == '(') {
          isDBrk = true
          var depth = 0
          str.substring(a + 2).takeWhile {
            when (it) {
              '(' -> {
                depth++
                true
              }
              ')' -> if (depth == 0) false else {
                depth--
                true
              }
              else -> true
            }
          }
        } else {
          str.substring(a + 1).takeWhile {
            it.isLetterOrDigit()
          }
        }
        val bp = Polynomial(base)
        if (base.all { it.isDigit() || it == '-' } && d.all { it.isDigit() || it == '-' }
          || d.any { it.isLetter() } || !bp.canBeTerm()
        ) {
          val p = "pow($base,$d)"
          str = str.substring(
            0,
            a - base.length - if (isBaseBrk && !isFn) 2 else 0
          ) + p + str.substring(a + d.length + if (isDBrk) 2 else 1)
        }
      }
      a++
    }
    val strUnarys = mutableListOf<String>()
    var j = 0
    var depth = 0
    for (i in str.indices) {
      when (str[i]) {
        '(' -> depth++
        ')' -> depth--
        '*' -> if (depth == 0) {
          strUnarys += str.substring(j, i).trim()
          j = i + 1
        }
        '/' -> if (depth == 0) {
          strUnarys += str.substring(j, i).trim()
          j = i
        }
      }
    }
    strUnarys += str.substring(j).trim()
    //関数で終わってるものを連結する
    var i = 0
    //関数^数字終わりか関数終わりにマッチする
    val reg = Regex(".*(($fs)\\^\\d+\$|($fs))$")
    while (i < strUnarys.size) {
      if (strUnarys[i].matches(reg)) {
        strUnarys[i] += strUnarys[i + 1]
        strUnarys.removeAt(i + 1)
        i--
      }
      i++
    }
    val nums = mutableListOf<TermBase>()
    val denos = mutableListOf<TermBase>()
    strUnarys.filter { it.isNotEmpty() }.forEach {
      val trimed = it.trim()
      if (trimed[0] == '/') {
        denos += if (trimed.length > 1 && trimed[1] == '(' && trimed.last() == ')') {
          Polynomial(trimed.substring(2, trimed.length - 1).trim())
        } else {
          Term(trimed.substring(1).trim())
        }
      } else {
        nums += if (it[0] == '(' && it.last() == ')') {
          Polynomial(it.substring(1, it.length - 1))
        } else {
          Term(it)
        }
      }
    }
    if (denos.isEmpty()) denos += Term.ONE
    return nums to denos
  }

  fun substitute(arg: Map<Char, Rational>): Unary {
    return Unary(polynomials.map {
      if (it is Term) it.substitute(arg) else (it as Polynomial).substitute(arg)
    }, denoPolynomial.map {
      if (it is Term) it.substitute(arg) else (it as Polynomial).substitute(arg)
    })
  }

  private fun evalPs(pols: List<TermBase>): TermBase {
    if (pols.isEmpty()) return Term("0")
    if (pols.any { it.toPolynomial().arranged().isZero() }) return Term("0")
    var ps = pols.map {
      if (it !is Term && !it.canBeTerm()) return@map it
      val term = it.toTerm()
      if (!term.functions.containsKey("pow")) return@map it
      val fn = term.functions["pow"]!!
      if (!fn.args[1].canBeTerm()) return@map it
      val t = fn.args[1].toTerm()
      if (t.functions.isNotEmpty() || t.letters.isNotEmpty()) return@map it
      fn.args[0].toPolynomial().pow(t.coefficient.toInt()) * Term(
        term.coefficient,
        term.letters,
        term.functions.filterKeys { k -> k != "pow" })
    }

    val sqrts = ps.filter { it is Term && it.functions.containsKey("sqrt") }
      .map { (it as Term).toTerm() }
    ps = if (sqrts.isNotEmpty()) {
      var coef = sqrts.map { term ->
        Term(
          term.coefficient,
          term.letters,
          term.functions.filterKeys { it != "sqrt" }
        )
      }.reduce { acc, term -> acc * term }.toPolynomial()
      val funs = sqrts.map { it.functions["sqrt"]!! }
      var arg = funs.map {
        var d = it.degree
        val arg = it.args[0]
        while (d !in -1..1) {
          if (d > 1) {
            d -= 2
            coef *= arg.toPolynomial()
          } else {
            d += 2
            coef /= arg.toTerm()
          }
        }
        when (d) {
          1 -> arg
          -1 -> Unary(listOf(Term.ONE), listOf(arg)).toPolynomial()
          else -> Term.ONE
        }
      }.reduce { acc, tb -> if (acc is Term && tb is Term) acc * tb else acc.toPolynomial() * tb.toPolynomial() }
        .toPolynomial().evaluate()
      if (arg.canBeTerm()) {
        var t = arg.toTerm()
        var n = Rational.ONE
        if (t.coefficient.toDouble() < 0) {
          t *= -1.0
          coef *= Term("i")
        }
        divisors(t.coefficient.numerator).groupBy { it.toInt() }.forEach { (t, u) ->
          if (u.size > 1) {
            n *= t.toDouble().pow(floor(u.size.toDouble() / 2)).toInt()
          }
        }
        divisors(t.coefficient.denominator).groupBy { it.toInt() }.forEach { (t, u) ->
          if (u.size > 1) {
            n /= t.toDouble().pow(floor(u.size.toDouble() / 2)).toInt()
          }
        }
        arg = (t.toPolynomial() / Term(n * n)).evaluate()
        coef *= n
      }
      ps.filter { it !is Term || !it.functions.containsKey("sqrt") } + coef.toTerm() + if (arg.isOne()) Term(
        Rational.ONE
      ) else Term(
        Rational.ONE,
        null,
        mapOf("sqrt" to FunctionValue(1, listOf(arg)))
      )
    } else ps
    return ps.reduce { acc, cur ->
      when (cur) {
        is Term -> if (acc is Term) acc * cur else acc.toPolynomial().evaluate() * cur
        else -> if (acc is Term) acc * cur.toPolynomial().evaluate() else acc.toPolynomial() * cur.toPolynomial()
          .evaluate()
      }
    }
  }

  fun evaluate(): TermBase {
    val u =
      evalPs(polynomials).let { if (it is Polynomial) it.evaluate().factorization() else it.toUnary() }
    val d =
      evalPs(denoPolynomial).let { if (it is Polynomial) it.evaluate() else it }.toPolynomial().factorization()
    if (u.isZero()) return Term.ZERO
    val us = (u.polynomials + d.denoPolynomial).toMutableList()
    val ds = (d.polynomials + u.denoPolynomial).toMutableList()
    var i = 0
    while (i < us.size && i < ds.size) {
      val j = us.indexOf(ds[i])
      if (j != -1) {
        ds.removeAt(i)
        us.removeAt(j)
      }
      i++
    }
    if (us.size == 0) us += Term.ONE
    if (ds.size == 0) ds += Term.ONE

    val result = Unary(
      listOf(us.reduce { acc, p ->
        when (acc) {
          is Term -> if (p is Term) acc * p else acc * p as Polynomial
          is Polynomial -> if (p is Term) acc * p else (acc * p as Polynomial).evaluate()
          else -> throw Exception("Don't get here")
        }
      }),
      listOf(ds.reduce { acc, p ->
        when (acc) {
          is Term -> if (p is Term) acc * p else acc * p as Polynomial
          is Polynomial -> if (p is Term) acc * p else (acc * p as Polynomial).evaluate()
          else -> throw Exception("Don't get here")
        }
      })
    )
    return if (result.polynomials.size == 1 && result.denoPolynomial.size == 1 && result.denoPolynomial[0].isOne()) {
      result.polynomials[0]
    } else result.toPolynomial()
  }

  fun approximation(): Unary {
    return Unary(polynomials.map { if (it is Term) it.approximation() else (it as Polynomial).approximation() },
      denoPolynomial.map { if (it is Term) it.approximation() else (it as Polynomial).approximation() }
    )
  }

  private fun psToString(ps: List<TermBase>, options: Set<String>?): String {
    val op = options ?: emptySet()
    return ps
      .filter { if (it is Term) !it.isOne() else true }
      .mapIndexed { index, it ->
        if (it is Term || it.toPolynomial().unaries.size == 1) {
          val a = if (it is Term) it else it.toTerm()
          //現在と前が両方数字かどうか
          val b = if (index > 0 && polynomials[index - 1] is Term) polynomials[index - 1] as Term else null
          val f =
            b != null && b.letters.isEmpty() && b.functions.isEmpty() && a.letters.isEmpty()
          if (f || index > 0 && (polynomials[index - 1] is Term && (polynomials[index - 1] as Term).letters.isNotEmpty())) {
            "*${a.toStringWith(op)}"
          } else {
            a.toStringWith(op)
          }
        } else {
          "(${it})"
        }
      }.joinToString("")
  }

  override fun toString(): String {
    return when {
      denoPolynomial.size == 1 && denoPolynomial[0].isOne() -> if (polynomials.size == 1) polynomials[0].toString()
      else psToString(polynomials, null)
      denoPolynomial.isEmpty() || polynomials.isEmpty() -> ""
      else -> {
        val n = psToString(polynomials, null).ifEmpty { "1" }
        val d = psToString(denoPolynomial, null)
        if (d.isEmpty()) n
        else "$n/$d"
      }
    }
  }

  fun toStringWith(options: Set<String>): String {
    if (polynomials.size == 1) return polynomials[0].toStringWith(options)
    return if (denoPolynomial.map { it.toPolynomial() }.reduce { acc, p -> acc + p }.isOne())
      psToString(polynomials, options)
    else "(${psToString(polynomials, options)})/${psToString(denoPolynomial, options)}"
  }

  operator fun times(double: Double): Unary {
    return times(Rational(double))
  }

  operator fun times(rational: Rational): Unary {
    return Unary(
      polynomials + Term(Rational(rational.numerator)),
      denoPolynomial + Term(Rational(rational.denominator))
    )
  }

  operator fun times(term: Term): Unary {
    return Unary(polynomials + term, denoPolynomial)
  }

  operator fun times(unary: Unary): Unary {
    return Unary(polynomials + unary.polynomials, denoPolynomial + unary.denoPolynomial)
  }

  operator fun times(pol: Polynomial): Unary {
    return Unary(polynomials + pol, denoPolynomial)
  }

  operator fun div(double: Double): Unary {
    return Unary(polynomials, denoPolynomial + Term(Rational(double)))
  }

  operator fun div(term: Term): Unary {
    return Unary(polynomials, denoPolynomial + term)
  }

  operator fun div(unary: Unary): Unary {
    return Unary(polynomials, denoPolynomial + unary.toPolynomial())
  }

  operator fun div(pol: Polynomial): Unary {
    return Unary(polynomials, denoPolynomial + pol)
  }

  operator fun plus(unary: Unary): Polynomial {
    return evaluate().toPolynomial() + unary.evaluate().toPolynomial()
  }

  operator fun unaryPlus(): Unary {
    return toUnary()
  }

  operator fun unaryMinus(): Unary {
    return times(Term.MINUS_ONE)
  }

  fun pow(i: Int): Unary {
    var r: Unary
    val t: Unary
    var j = i
    when (i) {
      0 -> return ONE
      else -> if (i > 0) {
        r = toUnary()
        t = this
      } else {
        j = -i
        r = Unary(denoPolynomial, polynomials)
        t = r.toUnary()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r
  }


  fun toTerm(): Term {
    if (!canBeTerm()) {
      throw ClassCastException("Cannot be term")
    }
    if (polynomials.isEmpty()) return Term.ONE
    return polynomials.map { it.toTerm() }.reduce { acc, t -> acc * t } / denoPolynomial.map { it.toTerm() }.reduce { acc, t -> acc * t }
  }

  fun canBeTerm(): Boolean {
    val u = polynomials.filter { it.toString() != "1" }.toMutableList()
    val d = denoPolynomial.filter { it.toString() != "1" }.toMutableList()
    if (u.isEmpty()) u += Term.ONE
    if (d.isEmpty()) d += Term.ONE
    if (d.size != 1 || !d[0].canBeTerm()) return false
    return u.all { it.canBeTerm() }
  }

  fun toUnary(): Unary {
    return Unary(polynomials.map {
      if (it is Term) {
        it.toTerm()
      } else {
        it.toPolynomial()
      }
    }, denoPolynomial.map {
      if (it is Term) {
        it.toTerm()
      } else {
        it.toPolynomial()
      }
    })
  }

  fun toPolynomial(): Polynomial {
    return Polynomial(listOf(toUnary()))
  }

  fun isZero(): Boolean {
    return polynomials.any { it.isZero() }
  }

  fun isOne(): Boolean {
    val t = evaluate()
    return if (t is Term) t.isOne()
    else if (t is Polynomial) {
      if (t.canBeTerm()) t.toTerm().isOne()
      else false
    } else throw Exception("Don't get here")
  }

  override fun equals(other: Any?): Boolean {
    if (other is Unary) {
      val a = polynomials.filter { !it.isZero() }
      val b = other.polynomials.filter { !it.isZero() }
      val c = denoPolynomial.filter { !it.isZero() }
      val d = other.denoPolynomial.filter { !it.isZero() }
      return a.size == b.size && a.containsAll(b) && c.size == b.size && c.containsAll(d)
    }
    return super.equals(other)
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

  override fun hashCode(): Int {
    var result = polynomials.hashCode()
    result = 31 * result + denoPolynomial.hashCode()
    return result
  }
}