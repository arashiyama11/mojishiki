package io.github.arashiyama11

import kotlin.math.*


val validFunctions = listOf("sin", "cos", "tan", "log", "sqrt", "abs", "max", "min", "pow")
val specialFunctions = mapOf(
  "sin" to SpecialFunction(
    1,
    { Rational(sin(it[0])).reduction() },
    { t, l -> Unary("${t[0].toPolynomial().differential(l).evaluate()}cos(${t[0]})") },
    { Unary("-cos(${it[0]})") }),
  "cos" to SpecialFunction(
    1,
    { Rational(cos(it[0])).reduction() },
    { t, l -> Unary("-${t[0].toPolynomial().differential(l).evaluate()}sin(${t[0]})") },
    { Unary("sin(${it[0]})") }),
  "tan" to SpecialFunction(1,
    { Rational(tan(it[0])).reduction() },
    { t, l -> Polynomial("${t[0].toPolynomial().differential(l).evaluate()}/(cos(${t[0]})*cos(${t[0]}))") },
    { Unary("-log(abs(cos(${it[0]})))") }),
  "log" to SpecialFunction(1,
    { Rational(log(it[0], E)).reduction() },
    { t, l -> Unary("${t[0].toPolynomial().differential(l).evaluate()}/${t[0]}") },
    { Polynomial("${it[0]}log(${it[0]})-${it[0]}") }),
  "sqrt" to SpecialFunction(
    1,
    {
      if (it[0] >= 0) Rational(sqrt(it[0])).reduction() else Unary(listOf(Rational(sqrt(-it[0])), Letter('i')))
    },
    { t, l -> Polynomial("${t[0].toPolynomial().differential(l).evaluate()}/2sqrt(${t[0]})") },
    { Polynomial("2${it[0]}^2sqrt(${it[0]})/3") }),
  "abs" to SpecialFunction(
    1,
    { Rational(abs(it[0])).reduction() },
    { t, l -> Polynomial("${t[0].toPolynomial().differential(l).evaluate()}${t[0]}/abs(${t[0]})") },
    null
  ),
  "min" to SpecialFunction(2, { Rational(min(it[0], it[1])).reduction() }, null, null),
  "max" to SpecialFunction(2, { Rational(max(it[0], it[1])).reduction() }, null, null),
  "pow" to SpecialFunction(2, {
    Rational(it[0]).pow(it[1].toInt()).reduction()
  }, null, null, { args ->
    val b = args[0].toString().let { if (it.length == 1) it else "($it)" }
    val d = args[1].toString().let { if (it.length == 1) it else "($it)" }
    "$b^$d"
  })
)

data class SpecialFunction(
  //null is vararg
  val argLength: Int?,
  val approximation: (List<Double>) -> TermBase,
  val differential: ((List<TermBase>, Letter) -> TermBase)?,
  val integral: ((List<TermBase>) -> TermBase)?,
  val toStringFn: ((List<TermBase>) -> String)? = null
)