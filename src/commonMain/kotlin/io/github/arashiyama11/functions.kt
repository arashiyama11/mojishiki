package io.github.arashiyama11

import kotlin.math.*


val validFunctions = listOf("sin", "cos", "tan", "log", "sqrt", "abs", "max", "min", "pow")
val specialFunctions = mapOf(
  "sin" to SpecialFunction(
    1,
    { Rational(sin(it[0])).reduction() },
    { Unary("cos(${it[0]})") },
    { Unary("-cos(${it[0]})") }),
  "cos" to SpecialFunction(
    1,
    { Rational(cos(it[0])).reduction() },
    { Unary("-sin(${it[0]})") },
    { Unary("sin(${it[0]})") }),
  "tan" to SpecialFunction(1,
    { Rational(tan(it[0])).reduction() },
    { Polynomial("1/cos(${it[0]})^2") },
    { Unary("-log(abs(cos(${it[0]})))") }),
  "log" to SpecialFunction(1,
    { Rational(log(it[0], E)).reduction() },
    { Polynomial("1/${it[0]}") },
    { Polynomial("${it[0]}log(${it[0]})-${it[0]}") }),
  "sqrt" to SpecialFunction(1, {
    if (it[0] >= 0) Rational(sqrt(it[0])).reduction() else Unary(listOf(Rational(sqrt(-it[0])), Letter('i')))
  }, { Polynomial("1/2sqrt(${it[0]})") }, { Polynomial("2${it[0]}^2sqrt(${it[0]})/3") }),
  "abs" to SpecialFunction(1, { Rational(abs(it[0])).reduction() }, { Polynomial("${it[0]}/abs(${it[0]})") }, null),
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
  val differential: ((List<TermBase>) -> TermBase)?,
  val integral: ((List<TermBase>) -> TermBase)?,
  val toStringFn: ((List<TermBase>) -> String)? = null
)