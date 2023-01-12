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
      if (i > 0) for (j in 0 until i) tb += p.copy()
      else if (i < 0) for (j in 0 until -i) dtb += p.copy()
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

    val a = tb.groupBy { it is Letter && it.letter == 'i' }
    var ac = Rational.ONE
    var ai = a[true]?.size ?: 0
    while (ai !in -1..1) {
      if (ai < 0) ai += 2 else ai -= 2
      ac = -ac
    }

    val b = dtb.groupBy { it is Letter && it.letter == 'i' }
    var bc = Rational.ONE
    var bi = b[true]?.size ?: 0
    while (bi !in -1..1) {
      if (bi < 0) bi += 2 else bi -= 2
      bc = -bc
    }

    termBases =
      (a[false] ?: emptyList()) + (if (!ac.isOne()) listOf(ac) else emptyList()) + if (ai > bi) List(ai - bi) {
        Letter(
          'i'
        )
      } else emptyList()
    denoTermBases =
      (b[false] ?: emptyList()) + (if (!bc.isOne()) listOf(bc) else emptyList()) + if (ai < bi) List(bi - ai) {
        Letter('i')
      } else emptyList()
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
    var str = input.replace(Regex("\\s"), "")

    var b = 1
    while (b + 1 < str.length) {
      if (str[b] == '^') {
        var isBaseBrk = false
        var isFn = false

        val base = when (str[b - 1]) {
          ')' -> {
            isBaseBrk = true
            var depth = 0
            val r = str.substring(0, b - 1).takeLastWhile {
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
            val s = str.substring(0, b - r.length - 2)
            val f = validFunctions.find { s.endsWith(it) }
            if (f != null) {
              isFn = true
              "$f($r)"
            } else r
          }
          in "1234567890" -> {
            str.substring(0, b).takeLastWhile { it.isDigit() }
          }
          else -> str[b - 1].toString()
        }

        var isDBrk = false
        val d = when (str[b + 1]) {
          '(' -> {
            isDBrk = true
            var depth = 0
            str.substring(b + 2).takeWhile {
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
          '-' -> "-" + str.substring(b + 2).takeWhile { it.isDigit() }
          in "1234567890" -> str.substring(b + 1).takeWhile { it.isDigit() }
          else -> str[b + 1].toString()
        }

        val p = "pow($base,$d)"
        str = str.substring(
          0,
          b - base.length - if (isBaseBrk && !isFn) 2 else 0
        ) + p + str.substring(b + d.length + if (isDBrk) 3 else 1)
      }
      b++
    }

    var a = 0
    if (str.length > 1 && str[0] == '-') {
      if (str[1] in Letter.valid || str[1] == '(') {
        str = "-1*" + str.substring(1)
        a = 3
      } else if (str[1].isDigit()) {
        a++
        while (a < str.length && str[a].isDigit()) a++
        if (a < str.length && str[a] == '.') {
          a++
          if (a >= str.length || !str[a].isDigit()) throw ParseException(str, a, "Excepting digit")
          while (a < str.length && str[a].isDigit()) a++
        }
        if (a < str.length && str[a] != '*')
          str = str.substring(0, a) + "*" + str.substring(a)
        a++
      }
    }

    while (a + 1 < str.length) {
      when (str[a]) {
        '/' -> {}
        '(' -> {
          if (a > 0 && (str[a - 1].isDigit() || str[a - 1] in Letter.valid)) {
            str = str.substring(0, a) + "*" + str.substring(a)
          }
          var depth = 0
          a++
          while (true) {
            if (str[a] == '(') depth++ else if (str[a] == ')') {
              if (depth == 0) break else depth--
            }
            a++
          }
          if (a + 1 >= str.length) break
          if (str[a + 1].isDigit() || str[a + 1] in Letter.valid || str[a + 1] == '(') {
            str = str.substring(0, a + 1) + "*" + str.substring(a + 1)
            a++
          } else if (str[a + 1] == '*') a++ else throw ParseException(str, a + 1)
        }
        in "1234567890" -> {
          if (str[a] == '-') {
            a++
            if (!str[a].isDigit()) throw ParseException(str, a, "Excepting digit")
          }
          while (str[a].isDigit()) {
            a++
            if (a == str.length) break
          }
          if (a < str.length && str[a] == '.') {
            a++
            if (a >= str.length || !str[a].isDigit()) throw ParseException(str, a, "Excepting digit")
            while (a < str.length && str[a].isDigit()) a++
          }
          if (a != str.length && str[a] !in ")/,*^.") str = str.substring(0, a) + "*" + str.substring(a)
        }
        in Letter.valid ->
          if (str[a + 1] in Letter.valid || str[a + 1] == '(') {
            val i = validFunctions.indexOfFirst { str.substring(a).startsWith(it) }
            if (i >= 0) {
              a += validFunctions[i].length + 1
              var depth = 0
              while (a < str.length) {
                if (str[a] == '(') depth++ else if (str[a] == ')') {
                  if (depth == 0) break else depth--
                }
                a++
              }
              if (a + 1 >= str.length) break
              if (str[a + 1].isDigit() || str[a + 1] in Letter.valid || str[a + 1] == '(') {
                str = str.substring(0, a + 1) + "*" + str.substring(a + 1)
                a++
              } else if (str[a + 1] == '*') a++
            } else {
              if (str[a + 1] != '*') str = str.substring(0, a + 1) + "*" + str.substring(a + 1)
              a++
            }
          } else if (str[a + 1] == '*') a++ else throw ParseException(str, a + 1)
        else -> throw ParseException(str, a)
      }
      a++
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

  private fun evalTs(pols: List<TermBase>): TermBase {
    if (pols.isEmpty()) return Rational.ZERO
    if (pols.any { it.toPolynomial().arranged().isZero() }) return Rational.ZERO
    return pols.fold(mutableListOf(), ::foldT).map(::evalT).reduce { acc, cur -> (acc * cur) }
  }

  private fun foldT(acc: MutableList<TermBase>, tb: TermBase): MutableList<TermBase> {
    when (tb) {
      is Func -> when (tb.name) {
        "pow" -> {
          val i = acc.indexOfFirst { it is Func && it.name == "pow" && it.args[0] == tb.args[0] }
          if (i == -1) acc += tb else acc[i] =
            Func("pow", tb.args[0], (acc[i] as Func).args[1].toPolynomial() + tb.args[1])
        }
        "sqrt" -> {
          val i = acc.indexOfFirst { it is Func && it.name == "sqrt" }
          if (i == -1) acc += tb else acc[i] = Func("sqrt", (acc[i] as Func).args[0] * tb.args[0])
        }
        else -> acc += tb
      }
      else -> acc += tb
    }
    return acc
  }

  private fun evalT(termBase: TermBase): TermBase {
    return when (termBase) {
      is Func -> when (termBase.name) {
        "pow" -> {
          val b = termBase.args[0]
          val d = termBase.args[1]
          if (d.canBeUnary() && d.toUnary().canBeRational()) {
            b.toPolynomial().pow(d.toUnary().toRational().toInt())
          } else termBase
        }
        "sqrt" -> {
          val arg = termBase.args[0]
          if (arg is Unary || arg.canBeUnary()) {
            val unary = arg.toUnary()
            val isImaginary = unary.rational.toDouble() < 0
            val fact = if (isImaginary) (-unary.rational).factorization() else unary.rational.factorization()
            val rs = fact.termBases.map { it as Rational }
            val drs = fact.denoTermBases.map { it as Rational }
            val list = mutableListOf<Rational>()
            val dlist = mutableListOf<Rational>()
            var coefRes = Rational.ONE
            rs.forEach {
              if (list.contains(it)) {
                coefRes *= it
                list.remove(it)
              } else list.add(it)
            }

            drs.forEach {
              if (dlist.contains(it)) {
                coefRes /= it
                dlist.remove(it)
              } else dlist.add(it)
            }

            val n = list.reduceOrNull { acc, r -> acc * r } ?: Rational.ONE
            val d = dlist.reduceOrNull { acc, r -> acc * r } ?: Rational.ONE

            (if (n.isOne() && d.isOne()) coefRes else
              Unary(
                listOf(
                  coefRes,
                  Func("sqrt", n / d)
                )
              )) * if (isImaginary) Letter('i') else ONE
          } else arg
        }
        else -> termBase
      }
      is Polynomial -> termBase.evaluate()
      else -> termBase
    }
  }

  fun evaluate(): TermBase {
    val uts = evalTs(termBases)
    val dts = evalTs(denoTermBases)

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
    else -> throw UnknownTermBaseInstanceException()
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
    else -> throw UnknownTermBaseInstanceException()
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