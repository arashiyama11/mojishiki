package io.github.arashiyama11

import kotlin.math.absoluteValue

class Unary(var termBases: List<TermBase>, var denoTermBases: List<TermBase> = listOf(Rational.ONE)) : TermBase() {
  var rational = Rational.ONE
  var letters = mutableMapOf<Letter, Int>()
  var funcs = mutableMapOf<Func, Int>()
  var pols = mutableMapOf<Polynomial, Int>()

  init {
    autoEvaluate()
    classification()
  }

  constructor(unaryString: String) : this() {
    val (t, d) = parse(unaryString)
    termBases = t
    denoTermBases = d
    autoEvaluate()
    classification()
  }

  constructor(t: TermBase = Rational.ONE, dt: TermBase = Rational.ONE) : this(listOf(t), listOf(dt))

  constructor(
    rat: Rational = Rational.ONE,
    lts: Map<Letter, Int>? = null,
    fns: Map<Func, Int>? = null,
    ps: Map<Polynomial, Int>? = null
  ) : this() {
    val tb = mutableListOf<TermBase>(Rational(rat.numerator))
    val dtb = mutableListOf<TermBase>(Rational(rat.denominator))

    lts?.forEach { (l, i) ->
      if (i > 0) for (j in 0 until i) tb += l.copy()
      else if (i < 0) for (j in 0 until -i) dtb += l.copy()
    }

    fns?.forEach { (f, i) ->
      if (i > 0) for (j in 0 until i) tb += f.copy()
      else if (i < 0) for (j in 0 until -i) dtb += f.copy()
    }

    ps?.forEach { (p, i) ->
      if (i > 0) for (j in 1 until i) tb += p.copy()
      else if (i < 0) for (j in 1 until -i) dtb += p.copy()
    }

    termBases = tb
    denoTermBases = dtb

    autoEvaluate()
    classification()
  }

  companion object {
    val ZERO get() = Rational.ZERO.toUnary()
    val ONE get() = Rational.ONE.toUnary()
    val MINUS_ONE get() = Rational.MINUS_ONE.toUnary()
  }

  private fun autoEvaluate() {
    val tb = mutableListOf<TermBase>()
    val dtb = mutableListOf<TermBase>()

    for (t in termBases.map { if (it is Polynomial && it.canBeUnary()) it.toUnary() else it }
      .filter { it.toString() != "1" }) {
      when (t) {
        is Unary -> {
          tb += t.termBases
          dtb += t.denoTermBases
        }
        is Func -> when (t.name) {
          "pow" -> if (t.args[0].canBeUnary() && t.args[1].canBeUnary()) {
            val b = t.args[0].toUnary()
            val d = t.args[1].toUnary()
            if (d.canBeRational() && b.pols.isEmpty()) {
              val a = d.toRational().toInt()
              if (a > 0) {
                for (i in 1..a) {
                  val copy = b.toUnary()
                  tb += copy.termBases
                  dtb += copy.denoTermBases
                }
              } else if (a < 0) {
                for (i in 1 until -a) {
                  val copy = b.toUnary()
                  dtb += copy.denoTermBases
                  tb += copy.termBases
                }
              }
            } else tb += t
          } else tb += t
          else -> tb += t
        }
        is Rational -> tb += t.reduction()
        else -> tb += t
      }
    }

    for (t in denoTermBases.map { if (it is Polynomial && it.canBeUnary()) it.toUnary() else it }
      .filter { it.toString() != "1" }) {
      when (t) {
        is Unary -> {
          tb += t.denoTermBases
          dtb += t.termBases
        }
        is Func -> when (t.name) {
          "pow" -> if (t.args[0] is Unary && t.args[1] is Unary) {
            val b = t.args[0] as Unary
            val d = t.args[1] as Unary
            if (d.canBeRational() && b.pols.isEmpty()) {
              val a = d.toRational().toInt()
              if (a > 0) {
                for (i in 1..a) {
                  val copy = b.copy()
                  tb += copy.denoTermBases
                  dtb += copy.termBases
                }
              } else if (a < 0) {
                for (i in 1 until -a) {
                  val copy = b.copy()
                  dtb += copy.termBases
                  tb += copy.denoTermBases
                }
              }
            }
          }
          else -> dtb += t
        }
        is Rational -> dtb += t.reduction()
        else -> dtb += t
      }
    }
    if (tb.isEmpty()) tb += Rational.ONE
    if (dtb.isEmpty()) dtb += Rational.ONE

    termBases = tb
    denoTermBases = dtb
  }

  private fun classification() {
    termBases.forEach {
      if (it is ExpressionUnit) when (it) {
        is Rational -> rational *= it
        is Letter -> if (letters.containsKey(it)) letters[it] = letters[it]!! + 1 else letters += it to 1
        is Func -> if (funcs.containsKey(it)) funcs[it] = funcs[it]!! + 1 else funcs += it to 1
      } else {
        val p = it as Polynomial
        if (pols.containsKey(p)) pols[p] = pols[p]!! + 1 else pols += p to 1
      }
    }

    denoTermBases.forEach {
      if (it is ExpressionUnit) when (it) {
        is Rational -> rational /= it
        is Letter -> if (letters.containsKey(it)) letters[it] = letters[it]!! - 1 else letters += it to -1
        is Func -> if (funcs.containsKey(it)) funcs[it] = funcs[it]!! - 1 else funcs += it to -1
      } else {
        val p = it as Polynomial
        if (pols.containsKey(p)) pols[p] = pols[p]!! - 1 else pols += p to -1
      }
    }
  }

  private fun desuger(input: String): String {
    var a = 0
    var str = input.trim()

    while (a < str.length) {
      if (str[a].isDigit()) {
        while (str[a].isDigit()) {
          a++
          if (a == str.length) break
        }
        if (a != str.length && str[a] !in ")/,*^") str = str.substring(0, a) + "*" + str.substring(a)
      }

      if (a + 1 < str.length && str[a].isLetter() && str[a + 1].isLetter()) {
        val i = validFunctions.indexOfFirst { str.substring(a).startsWith(it) }
        if (i >= 0) {
          a += validFunctions[i].length
          while (str[a] != ')') {
            a++
          }
        } else str = str.substring(0, a + 1) + "*" + str.substring(a + 1)
      }

      if (a + 1 < str.length && str[a] == ')' && a != str.length - 1 && (str[a + 1].isLetterOrDigit() || str[a + 1] == '(')) {
        str = str.substring(0, a + 1) + "*" + str.substring(a + 1)
      }

      if (a + 1 < str.length && a > 0 && str[a] == '(' && !validFunctions.any { str.substring(0, a).endsWith(it) }) {
        val char = str[a - 1]
        if (char.isLetterOrDigit()) {
          str = str.substring(0, a) + "*" + str.substring(a)
        }
      }

      //ó›èÊÇÃèàóù
      if (a + 1 < str.length && str[a] == '^') {
        var isBaseBrk = false
        var isFn = false

        val base = when (str[a - 1]) {
          ')' -> {
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
          }
          in "1234567890" -> {
            str.substring(0, a).takeLastWhile { it.isDigit() }
          }
          else -> str[a - 1].toString()
        }

        var isDBrk = false
        val d = when (str[a + 1]) {
          '(' -> {
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
          }
          '-' -> "-" + str.substring(a + 2).takeWhile { it.isDigit() }
          in "1234567890" -> str.substring(a + 1).takeWhile { it.isDigit() }
          else -> str[a + 1].toString()
        }

        val p = "pow($base,$d)"
        str = str.substring(
          0,
          a - base.length - if (isBaseBrk && !isFn) 2 else 0
        ) + p + str.substring(a + d.length + if (isDBrk) 2 else 1)
      }

      a++
    }

    if (str.length > 1 && str[0] == '-' && str[1].isLetter()) {
      str = "-1*" + str.substring(1)
    }

    return str
  }

  private fun parse(input: String): Pair<List<TermBase>, List<TermBase>> {
    if (input.isEmpty()) return emptyList<TermBase>() to emptyList()
    val str = desuger(input)
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
    val nums = mutableListOf<TermBase>()
    val denos = mutableListOf<TermBase>()
    strUnarys.filter { it.isNotEmpty() }.forEach {
      if (it[0] == '/') {
        denos += if (it.length > 1 && it[1] == '(' && it.last() == ')') {
          Polynomial(it.substring(2, it.length - 1).trim())
        } else {
          ExpressionUnit.parse(it.substring(1).trim())
        }
      } else {
        nums += if (it[0] == '(' && it.last() == ')') {
          Polynomial(it.substring(1, it.length - 1))
        } else {
          ExpressionUnit.parse(it)
        }
      }
    }
    if (denos.isEmpty()) denos += Rational.ONE
    return nums to denos
  }

  override fun substitute(entries: Map<Letter, TermBase>) =
    Unary(termBases.map { it.substitute(entries) }, denoTermBases.map { it.substitute(entries) })

  private fun evalPs(pols: List<TermBase>): TermBase {
    if (pols.isEmpty()) return Rational.ZERO
    if (pols.any { it.toPolynomial().arranged().isZero() }) return Rational.ZERO
    val ps = mutableListOf<TermBase>()
    pols.forEach {
      when (it) {
        is Func -> when (it.name) {
          "pow" -> {
            val b = it.args[0]
            val d = it.args[1]
            if (d.canBeUnary() && d.toUnary().canBeRational()) {
              for (i in 0 until d.toUnary().toRational().toInt()) ps += b
            } else ps += it
          }
          else -> ps += it
        }
        else -> ps += it
      }
    }
    return ps.reduce { acc, cur -> (acc * cur) }
  }

  fun evaluate(): TermBase {
    val uts = evalPs(termBases)
    val dts = evalPs(denoTermBases)

    if (dts.toString() == "1") return uts.let { if (it is Polynomial) it.arranged() else it }
    if (dts.toString() == "-1") return uts.let { if (it is Polynomial) it.arranged() else it } * Rational.MINUS_ONE

    val u = uts.let { t ->
      when (t) {
        is Polynomial -> t.factorization()
        is Rational -> t.factorization()
        else -> t.toUnary().let { if (it.canBeRational()) it.toRational().factorization() else it }
      }
    }

    val d = dts.let { t ->
      when (t) {
        is Polynomial -> t.factorization()
        is Rational -> t.factorization()
        else -> t.toUnary().let { if (it.canBeRational()) it.toRational().factorization() else it }
      }
    }

    val us = (u.termBases + d.denoTermBases).filter { it.toString() != "1" }.toMutableList()
    val ds = (d.termBases + u.denoTermBases).filter { it.toString() != "1" }.toMutableList()

    var i = 0
    while (i < ds.size) {
      val j = us.indexOf(ds[i])
      if (j != -1) {
        ds.removeAt(i)
        us.removeAt(j)
      } else i++
    }
    if (ds.isEmpty()) ds += Rational.ONE
    if (us.isEmpty()) us += Rational.ONE

    return if (ds.all { it.toString() == "1" }) {
      if (us.filter { it.toString() != "1" }.size == 1) {
        us.filter { it.toString() != "1" }[0].let { if (it is Polynomial) it.arranged() else it }
      } else us.reduce { acc, t -> acc * t }.toPolynomial().arranged()
    } else Unary(us, ds)
  }

  override fun approximation() = Unary(termBases.map { it.approximation() }, denoTermBases.map { it.approximation() })

  private fun exprToString(exp: List<MutableMap.MutableEntry<out TermBase, Int>>?): String {
    if (exp == null) return ""
    return exp.let { if (it.isNotEmpty() && it[0].key is Letter) it.sortedBy { (it.key as Letter).letter } else it }
      .mapIndexed { i, (tb, d) ->
        if (d == 0) return@mapIndexed ""
        (if (tb is Polynomial) "($tb)" else "$tb") + when (d) {
          1 -> ""
          else -> "^${d.absoluteValue}"
        }
      }.joinToString("")
  }

  override fun toString(): String {
    val lts = letters.entries.groupBy { it.value > 0 }
    val fns = funcs.entries.groupBy { it.value > 0 }
    val ps = pols.entries.groupBy { it.value > 0 }

    var n = "${rational.numerator}${exprToString(lts[true])}${exprToString(fns[true])}${exprToString(ps[true])}"
    var d = "${rational.denominator}${exprToString(lts[false])}${exprToString(fns[false])}${exprToString(ps[false])}"

    if (n.length > 1 && n[0] == '1' && !n[1].isDigit()) n = n.substring(1)
    else if (n.length > 2 && n[0] == '-' && n[1] == '1' && !n[2].isDigit()) n = '-' + n.substring(2)
    if (d.length > 1 && d[0] == '1' && !d[1].isDigit()) d = d.substring(1)
    else if (d.length > 2 && d[0] == '-' && d[1] == '1' && !d[2].isDigit()) d = '-' + d.substring(2)

    return if (d == "1") n
    else "$n/$d"
  }

  fun toStringWith(options: Set<String>): String {
    TODO()
  }

  operator fun times(double: Double) = times(Rational(double))

  operator fun times(int: Int) = times(Rational(int.toLong()))

  operator fun times(rational: Rational) = Unary(
    termBases + Rational(rational.numerator),
    denoTermBases + Rational(rational.denominator)
  )

  operator fun times(unary: Unary) = Unary(termBases + unary.termBases, denoTermBases + unary.denoTermBases)

  operator fun times(pol: Polynomial) = Unary(termBases + pol, denoTermBases)

  operator fun times(exp: ExpressionUnit) = when (exp) {
    is Rational -> times(exp)
    else -> Unary(termBases + exp, denoTermBases)
  }

  override fun times(other: TermBase): TermBase = when (other) {
    is ExpressionUnit -> times(other)
    is Unary -> times(other)
    is Polynomial -> times(other)
    else -> throw Exception("")
  }

  operator fun div(double: Double) = div(Rational(double))

  operator fun div(int: Int) = div(Rational(int.toLong()))

  operator fun div(rational: Rational) =
    Unary(termBases + Rational(rational.denominator), denoTermBases + Rational(rational.numerator))

  operator fun div(unary: Unary) = Unary(termBases + unary.denoTermBases, denoTermBases + unary.termBases)

  operator fun div(pol: Polynomial) = Unary(termBases, denoTermBases + pol)

  operator fun div(exp: ExpressionUnit) = if (exp is Rational) div(exp) else Unary(termBases, denoTermBases + exp)

  operator fun div(termBase: TermBase) = when (termBase) {
    is Polynomial -> div(termBase)
    is Unary -> div(termBase)
    is ExpressionUnit -> div(termBase)
    else -> throw Exception("")
  }

  operator fun plus(unary: Unary): Unary {
    if (!hasSameFuncAndLetter(unary)) throw Exception("Cannot plus")
    return Unary(rational + unary.rational, letters, funcs, pols)
  }

  operator fun unaryPlus() = toUnary()

  operator fun unaryMinus() = times(Rational.MINUS_ONE)

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
        r = reciprocal()
        t = r.toUnary()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r
  }

  override fun canBeUnary() = true

  override fun toUnary() = Unary(termBases.map { it.copy() }, denoTermBases.map { it.copy() })

  override fun toPolynomial() = Polynomial(toUnary())

  fun isZero(): Boolean = termBases.any {
    when (it) {
      is Polynomial -> it.isZero()
      is Unary -> it.rational.numerator == 0L
      is Rational -> it.numerator == 0L
      else -> false
    }
  }

  fun isOne(): Boolean = evaluate().let {
    when (it) {
      is Polynomial -> it.toString() == "1"
      is Unary -> it.rational.toDouble() == 1.0
      is Rational -> it.toDouble() == 1.0
      else -> false
    }
  }

  fun toRational(): Rational {
    if (!canBeRational()) throw ClassCastException("")
    return rational.toRational()
  }

  fun canBeRational() = letters.isEmpty() && funcs.isEmpty() && pols.isEmpty()

  fun hasSameFuncAndLetter(other: Unary) = letters == other.letters && funcs == other.funcs && pols == other.pols

  override fun equals(other: Any?): Boolean {
    if (other is Unary) {
      return rational == other.rational && letters == other.letters && funcs == other.funcs
    }
    return false
  }

  override fun hashCode(): Int {
    var result = termBases.hashCode()
    result = 31 * result + denoTermBases.hashCode()
    return result
  }

  override fun copy() = toUnary()

  fun reciprocal() = Unary(denoTermBases.map { it.copy() }, termBases.map { it.copy() })
}