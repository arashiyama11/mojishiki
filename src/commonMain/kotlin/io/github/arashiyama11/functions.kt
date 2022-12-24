package io.github.arashiyama11

import kotlin.math.*


val validFunctions = listOf("sin", "cos", "tan", "log", "sqrt", "abs", "max", "min", "pow")
val specialFunctions = mapOf(
  "sin" to SpecialFunction({ Term(Rational(sin(it[0]))) }, { Term("cos(${it[0]})") }, { Term("-cos(${it[0]})") }),
  "cos" to SpecialFunction({ Term(Rational(cos(it[0]))) }, { Term("-sin(${it[0]})") }, { Term("sin(${it[0]})") }),
  "tan" to SpecialFunction(
    { Term(Rational(tan(it[0]))) },
    { Polynomial("1/cos(${it[0]})^2") },
    { Term("-log(abs(cos(${it[0]})))") }),
  "log" to SpecialFunction(
    { Term(Rational(log(it[0], E))) },
    { Polynomial("1/${it[0]}") },
    { Polynomial("${it[0]}log(${it[0]})-${it[0]}") }),
  "sqrt" to SpecialFunction({
    if (it[0] >= 0) Term(Rational(sqrt(it[0]))) else Term(
      Rational(sqrt(-it[0])),
      mutableMapOf('i' to 1)
    )
  }, { Polynomial("1/2sqrt(${it[0]})") }, { Polynomial("2x^2sqrt(${it[0]})/3") }),
  "abs" to SpecialFunction({ Term(Rational(abs(it[0]))) }, { Polynomial("${it[0]}/abs(${it[0]})") }, null),
  "min" to SpecialFunction({ Term(Rational(min(it[0], it[1]))) }, null, null),
  "max" to SpecialFunction({ Term(Rational(max(it[0], it[1]))) }, null, null),
  "pow" to SpecialFunction({
    Term(Rational(it[0]).pow(it[1].toInt()))
  }, null, null, { args, _ ->
    val b = if (args[0].canBeTerm()) "${args[0].toTerm()}" else "(${args[0]})"
    val d = if (args[1].canBeTerm()) "${args[1].toTerm()}" else "(${args[1]})"
    "$b^$d"
  })
)

data class SpecialFunction(
  val approximation: (List<Double>) -> Term,
  val differential: ((List<TermBase>) -> TermBase)?,
  val integral: ((List<TermBase>) -> TermBase)?,
  val toStringFn: ((List<TermBase>, Int) -> String)? = null
)

data class FunctionValue(var degree: Int, var args: List<TermBase>)
