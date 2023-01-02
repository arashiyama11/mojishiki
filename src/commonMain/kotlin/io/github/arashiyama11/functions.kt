package io.github.arashiyama11

import kotlin.math.*


val validFunctions = listOf("sin", "cos", "tan", "log", "sqrt", "abs", "max", "min", "pow")
val specialFunctions = mapOf(
  "sin" to SpecialFunction({ Unary(Rational(sin(it[0]))) }, { Unary("cos(${it[0]})") }, { Unary("-cos(${it[0]})") }),
  "cos" to SpecialFunction({ Unary(Rational(cos(it[0]))) }, { Unary("-sin(${it[0]})") }, { Unary("sin(${it[0]})") }),
  "tan" to SpecialFunction(
    { Unary(Rational(tan(it[0]))) },
    { Polynomial("1/cos(${it[0]})^2") },
    { Unary("-log(abs(cos(${it[0]})))") }),
  "log" to SpecialFunction(
    { Unary(Rational(log(it[0], E))) },
    { Polynomial("1/${it[0]}") },
    { Polynomial("${it[0]}log(${it[0]})-${it[0]}") }),
  "sqrt" to SpecialFunction({
    if (it[0] >= 0) Unary(Rational(sqrt(it[0]))) else Unary(
      Rational(sqrt(-it[0])),
      Letter('i')
    )
  }, { Polynomial("1/2sqrt(${it[0]})") }, { Polynomial("2${it[0]}^2sqrt(${it[0]})/3") }),
  "abs" to SpecialFunction({ Unary(Rational(abs(it[0]))) }, { Polynomial("${it[0]}/abs(${it[0]})") }, null),
  "min" to SpecialFunction({ Unary(Rational(min(it[0], it[1]))) }, null, null),
  "max" to SpecialFunction({ Unary(Rational(max(it[0], it[1]))) }, null, null),
  "pow" to SpecialFunction({
    Unary(Rational(it[0]).pow(it[1].toInt()))
  }, null, null, { args->
    val b=args[0].toString().let { if(it.length==1) it else "($it)" }
    val d=args[1].toString().let { if(it.length==1) it else "($it)" }
    "$b^$d"
  })
)

data class SpecialFunction(
  val approximation: (List<Double>) -> Unary,
  val differential: ((List<TermBase>) -> TermBase)?,
  val integral: ((List<TermBase>) -> TermBase)?,
  val toStringFn: ((List<TermBase>) -> String)? = null
)