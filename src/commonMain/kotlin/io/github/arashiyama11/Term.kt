package io.github.arashiyama11

class Term(termString: String) : TermBase() {
  var coefficient: Rational
  var letters: MutableMap<Char, Int> = mutableMapOf()

  //2以上の引数を持つ関数についてはそのうち考える
  var functions: MutableMap<String, FunctionValue> = mutableMapOf()


  companion object {
    val ZERO get() = Term(Rational.ZERO)
    val ONE get() = Term(Rational.ONE)
    val MINUS_ONE get() = Term(Rational.MINUS_ONE)
  }

  init {
    val result = parse(termString)
    coefficient = result.first.reduction()
    letters = result.second
    functions = result.third
    evaluate()
  }

  constructor(
    coef: Rational,
    lts: Map<Char, Int>? = null,
    funcs: Map<String, FunctionValue>? = null
  ) : this("") {
    coefficient = coef.reduction()
    letters = lts?.toMutableMap() ?: mutableMapOf()
    functions = funcs?.toMutableMap() ?: mutableMapOf()
    evaluate()
  }

  private fun evaluate(): Term {
    var c = coefficient.toRational()
    var l = mutableMapOf<Char, Int>()
    val f= mutableMapOf<String,FunctionValue>()
    letters.forEach { (k, v) ->
      if (v == 0) return@forEach
      when (k) {
        'i' -> {
          var i = v
          while (i > 1) {
            i -= 2
            c *= -1
          }
          if (i != 0) {
            l += k to i
          }
        }
        'C' -> l += k to 1
        else -> {
          l += k to v
        }
      }
    }

    functions.forEach {(name,fv)->
      val fn=when(name){
        "pow"->{
          val base=fv.args[0]
          val deg=fv.args[1].toPolynomial()*fv.degree.toDouble()
          if(base.canBeTerm()&&deg.canBeTerm()){
            val t=deg.toTerm()
            if(t.letters.isEmpty()&&t.functions.isEmpty()){
              val r=base.toTerm().pow(t.coefficient.toInt())
              c*=r.coefficient

              val ls=(l.keys+r.letters.keys).distinct()
              val res= mutableMapOf<Char,Int>()
              for(k in ls){
                res+=k to (l[k]?:0)+(r.letters[k]?:0)
              }

              l=res

              r.functions.forEach { (t,fv)->
                val i=fv.degree
                val p=fv.args
                if(f[t]?.args.toString()==p.toString()) {
                  f[t] = FunctionValue(f[t]!!.degree + i, p)
                }else {
                  f += t to FunctionValue(i, p)
                }
              }
              null
            }else name to fv
          }else name to fv
        }
        else -> name to fv
      }

      if(fn!=null){
        f+=fn
      }
    }

    functions=f
    coefficient = c
    letters = l
    return this
  }

  //関数を実行して近似値を得る
  fun approximation(): Term {
    var r = Term(coefficient, letters)
    functions.forEach { (func, fv) ->
      val i=fv.degree
      val tb=fv.args
      //argをappしたもの
      val argPs = tb.map { Polynomial(it.toPolynomial().unaries).evaluate().approximation() }
      //app可能(全て数だったらapp)でなければid
      if (argPs.any { !it.canBeTerm() }) {
        r *= Term(Rational.ONE,null,mapOf(func to FunctionValue(i, argPs)))
        return@forEach
      }
      val ts = argPs.map { it.toTerm() }
      if (ts.any { it.functions.isNotEmpty() || it.letters.isNotEmpty() }) {
        r *= Term(Rational.ONE,null,mapOf(func to FunctionValue(i, argPs)))
        return@forEach
      }
      val v = specialFunctions[func]!!.approximation(ts.map { it.coefficient.toDouble() })
      for (j in 0 until i) {
        r *= v
      }
    }
    return r
  }

  fun substitute(arg: Map<Char, Rational>): Term {
    var result = coefficient
    arg.forEach { (c, r) ->
      if (letters.containsKey(c)) {
        for (i in 0 until letters[c]!!) {
          result *= r
        }
      }
    }

    return Term(
      result,
      letters.filterKeys { !arg.containsKey(it) },
      functions.mapValues {
        FunctionValue(it.value.degree, it.value.args.map { t -> t.toPolynomial().substitute(arg) })
      }
    )
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(listOf(toUnary()))
  }

  override fun toUnary(): Unary {
    return Unary(listOf(toTerm()))
  }

  override fun toTerm(): Term {
    return Term(coefficient.toRational(), letters.toMap(), functions.mapValues { it.value.copy() })
  }

  override fun canBeTerm() = true

  override fun equals(other: Any?): Boolean {
    if (other is Term) {
      return letters == other.letters
          && coefficient == other.coefficient
          && functions == other.functions
    }
    return false
  }

  override fun isZero(): Boolean {
    return coefficient.numerator == 0L
  }

  override fun isOne(): Boolean {
    return coefficient.toDouble() == 1.0 && letters.isEmpty() && functions.isEmpty()
  }

  private class TermPlusException(e: String) : Exception(e)

  operator fun plus(term: Term): Term {
    if (term.letters.all { (k, v) ->
        letters[k] == v
      } && term.functions.all { (k, v) ->
        functions[k]?.let { it.degree == v.degree && it.args.toString() == v.args.toString() } == true
      }) {
      return Term(coefficient + term.coefficient, letters, functions)
    } else {
      throw TermPlusException("Cannot plus")
    }
  }

  operator fun plus(double: Double): Term {
    if (letters.isEmpty()) {
      return Term(coefficient + Rational(double), letters, functions)
    } else {
      throw TermPlusException("Cannot plus")
    }
  }

  operator fun minus(term: Term): Term {
    return plus(-term)
  }

  operator fun times(term: Term): Term {
    val ls = (letters.keys + term.letters.keys).distinct()
    val res = mutableMapOf<Char, Int>()
    for (c in ls) {
      res += c to (letters[c] ?: 0) + (term.letters[c] ?: 0)
    }

    val fs = functions.toMutableMap()
    term.functions.forEach { (t, fv) ->
      val i=fv.degree
      val p=fv.args
      if (fs[t]?.args.toString() == p.toString()) {
        fs[t] = FunctionValue(fs[t]!!.degree + i, p)
      } else {
        fs += t to FunctionValue(i, p)
      }
    }
    return Term(coefficient * term.coefficient, res, fs)
  }

  operator fun times(double: Double): Term {
    return Term(coefficient * Rational(double), letters, functions)
  }

  operator fun times(rational: Rational): Term {
    return Term(coefficient * rational, letters, functions)
  }

  operator fun times(Unary: Unary): Unary {
    return Unary.times(this)
  }

  operator fun times(polynomial: Polynomial): Polynomial {
    return Polynomial(polynomial.unaries.map { it * this })
  }

  operator fun div(term: Term): Term {
    return times(term.reciprocal())
  }

  operator fun div(double: Double): Term {
    return Term(coefficient / Rational(double), letters, functions)
  }

  operator fun div(rational: Rational): Term {
    return times(rational.reciprocal())
  }

  operator fun unaryPlus(): Term {
    return toTerm()
  }

  operator fun unaryMinus(): Term {
    return toTerm() * -1.0
  }

  fun pow(i: Int): Term {
    var r: Term
    val t: Term
    var j = i
    when (i) {
      0 -> return ONE
      else -> if (i > 0) {
        r = toTerm()
        t = this
      } else {
        j = -i
        r = reciprocal()
        t = reciprocal()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r
  }

  private class TermParseException(e: String) : Exception(e)

  private fun parse(input: String): Triple<Rational, MutableMap<Char, Int>, MutableMap<String, FunctionValue>> {
    if (input.isEmpty()) return Triple(Rational.ZERO, mutableMapOf(), mutableMapOf())
    var i = 0
    var coefficientString = ""
    if (input[0] == '-') {
      coefficientString = "-"
      i++
    } else if (input[0] == '+') {
      i++
    }

    //+,- の後のスペースは許容
    if (i == 1) {
      while (input[i].isWhitespace()) i++
    }

    while (i < input.length && input[i].isDigit()) {
      coefficientString += input[i]
      i++
    }

    val decimal = if (i < input.length && input[i] == '.') {
      "." + (input.substring(i + 1) + " ").takeWhile {
        i++
        it.isDigit()
      }
    } else ""

    var deno = 1L
    if (i < input.length && input[i] == '/') {
      i++
      var denoStr = ""
      while (i < input.length && input[i].isDigit()) {
        denoStr += input[i]
        i++
      }
      deno = if (denoStr.isEmpty()) 1L else denoStr.toLong()
    }

    if (coefficientString == "") coefficientString = "1"
    val ls = mutableMapOf<Char, Int>()
    val funcs = mutableMapOf<String, FunctionValue>()
    //xの次数
    while (i < input.length) {
      val func = validFunctions.find { input.substring(i).startsWith(it) }
      if (func == null) {
        if (input[i].isLetter()) {
          val l = input[i]
          val d = if (i + 1 < input.length && input[i + 1] == '^') {
            val t = if (input[i + 2] == '-') {
              i++
              -1
            } else {
              1
            }
            i++
            var str = ""
            while (i + 1 < input.length && input[i + 1].isDigit()) {
              i++
              str += input[i]
            }
            str.toInt() * t
          } else 1
          ls += l to d
        } else {
          throw TermParseException("Unexpected token:${input[i]}")
        }
      } else {
        i += func.length
        var deg = if (input[i] == '^') {
          i++
          input.substring(i).takeWhile {
            i++
            it.isDigit()
          }.toInt()
        } else 1

        val args = if (input[i] == '(') {
          var depth = 0

          val argsStr = input.substring(i + 1).takeWhile {
            i++
            when (it) {
              '(' -> {
                depth++
                true
              }
              ')' -> if (depth == 0) {
                false
              } else {
                depth--
                true
              }
              else -> true
            }
          }
          val result = mutableListOf<Polynomial>()
          var d = 0
          var k = 0
          for (j in argsStr.indices) {
            when (argsStr[j]) {
              '(' -> d++
              ')' -> d--
              ',' -> if (d == 0) {
                result += Polynomial(argsStr.substring(k, j))
                k = j + 1
              }
            }
          }
          result += Polynomial(argsStr.substring(k))
          result
        } else {
          listOf(Term(input[i].toString()))
        }
        i++
        if (i < input.length && input[i] == '^') {
          i++

          deg = (if (input[i] == '-') {
            i++
            -1
          } else 1) * input.substring(i).takeWhile {
            i++
            it.isDigit()
          }.toInt()
          i-=2
        }else i--

        funcs += func to FunctionValue(
          deg, args
        )
        if(i+2==input.length)break
      }
      i++
    }

    val c = if (coefficientString == "-") Rational.MINUS_ONE else when (deno) {
      1L -> Rational((coefficientString + decimal).toDouble() / deno)
      else -> Rational(coefficientString.toLong(), deno)
    }
    return Triple(c, ls, funcs)
  }


  override fun toString(): String {
    val coef = coefficient.reduction().let {
      if (it.denominator < 0)
        Rational(-it.numerator, -it.denominator)
      else it
    }
    if (coef.numerator == 0L) return "0"

    val lts = letters.filter { it.value != 0 }.entries.sortedBy { it.key }
    val a = lts.groupBy { it.value > 0 }

    val pos = a[true]?.joinToString("") { (c, i) ->
      when (i) {
        1 -> "$c"
        else -> "$c^$i"
      }
    } ?: ""

    val neg = a[false]?.joinToString("") { (c, i) ->
      when (val j = -i) {
        1 -> "$c"
        else -> "$c^$j"
      }
    } ?: ""

    val funcs = functions.toList().joinToString("") { (f, v) ->
      if (specialFunctions[f]?.toStringFn != null) {
        specialFunctions[f]!!.toStringFn?.let { it(v.args, v.degree) }!!
      } else if (v.degree == 1) {
        "$f(${v.args.joinToString(",")})"
      } else {
        "$f(${v.args.joinToString(",")})^${v.degree}"
      }
    }

    val add1 = funcs.isEmpty() && pos.isEmpty()

    return when (coef.numerator) {
      1L -> if (add1) "1" else ""
      -1L -> if (add1) "-1" else "-"
      else -> coef.numerator.toString()
    } + pos + funcs +
        if (coef.denominator != 1L || neg.isNotEmpty()) "/${if (coef.denominator == 1L && neg.isNotEmpty()) "" else coef.denominator}${neg}" else ""
  }

  override fun toStringWith(options: Set<String>): String {
    val coef = coefficient.reduction().let {
      if (it.denominator < 0)
        Rational(-it.numerator, -it.denominator)
      else it
    }
    if (coef.numerator == 0L) return "0"

    val lts = letters.filter { it.value != 0 }.entries.sortedBy { it.key }
    val a = lts.groupBy { it.value > 0 }

    val pos = a[true]?.joinToString("") { (c, i) ->
      when (i) {
        1 -> "$c"
        else -> "$c^$i"
      }
    } ?: ""

    val neg = a[false]?.joinToString("") { (c, i) ->
      when (val j = -i) {
        1 -> "$c"
        else -> "$c^$j"
      }
    } ?: ""

    val funcs = functions.toList().joinToString("") { (f, v) ->
      if (v.degree == 1) {
        "$f(${v.args.joinToString(",")})"
      } else {
        "$f^${v.degree}(${v.args.joinToString(",")})"
      }
    }

    val add1 = funcs.isEmpty() && pos.isEmpty()
    if (options.contains("double")) {
      return "${coef.toDouble()}${lts.joinToString("") { (k, v) -> "$k^$v" }}$funcs"
    }

    return when (coef.numerator) {
      1L -> if (add1) "1" else ""
      -1L -> if (add1) "-1" else "-"
      else -> coef.numerator.toString()
    } + pos + funcs +
        if (coef.denominator != 1L || neg.isNotEmpty()) "/${if (coef.denominator == 1L && neg.isNotEmpty()) "" else coef.denominator}${neg}" else ""
  }

  fun reciprocal(): Term {
    return Term(
      coefficient.reciprocal(),
      letters.mapValues { (_, v) -> -v },
      functions.mapValues { FunctionValue(-it.value.degree, it.value.args) }
    )
  }

  override fun hashCode(): Int {
    var result =  coefficient.hashCode()
    result = 31 * result + letters.hashCode()
    result = 31 * result + functions.hashCode()
    return result
  }
}