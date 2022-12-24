package io.github.arashiyama11

import kotlin.test.Test
import kotlin.test.assertEquals

class UnaryTest {
  @Test
  fun parseTest(){
    assert(Unary("334"), "334")
    assert(Unary("-334"), "-334")
    assert(Unary("2^10").evaluate(), "1024")
    assert(Unary("-3^2").evaluate(), "-9")
    assert(Unary("(-3)^2").evaluate(), "9")
    assert(Unary("3x"), "3x")
    assert(Unary("x(x+1)"), "x(x+1)")
    assert(Unary("-2(x+1)"), "-2(x+1)")
    assert(Unary("(x+2)(x+1)"), "(x+2)(x+1)")
    assert(Unary("sin(x+1)cos(x)"), "sin(x+1)cos(x)")
    assert(Unary("sqrt(sin(x))"), "sqrt(sin(x))")
    assert(Unary("x^(x+1)"), "x^(x+1)")
    assert(Unary("(x+2)^3"), "(x+2)^3")
    assert(Unary("ab^xy"), "ab^xy")
    assert(Unary("5x^2y"), "5x^2y")
    assert(Unary("x(x+y)"), "x(x+y)")
    assert(Unary("(x+y)(x+z)"), "(x+y)(x+z)")
  }

  @Test
  fun calculateTest(){
    assert(Unary("x") * Unary("(x+1)"), "x(x+1)")
    assert(Unary("ab") * Unary("(x+1)"), "ab(x+1)")
    assert((Unary("12") * Unary("3")).evaluate(), "36")
    assert((Unary("3x") * Unary("2x")).evaluate(), "6x^2")
    assert((Unary("x^3") * Unary("2x")).evaluate(), "2x^4")
    assert(Polynomial("(sinx+1)(2sinx+1)").evaluate(), "2sin(x)^2+3sin(x)+1")
  }

  @Test
  fun substituteTest(){
    assert(Unary("x(x+1)").substitute(mapOf('x' to Rational(1.0))).evaluate(), "2")
    assert(Unary("(x+1)(x+3)").substitute(mapOf('x' to Rational(1.0))).evaluate(), "8")
    assert(Unary("(x+2)(x+1)").substitute(mapOf('x' to Rational(-2.0))).evaluate(), "0")
    assert(Unary("(x+5)^x").substitute(mapOf('x' to Rational(-2.0))).evaluate(), "1/9")
  }

  private fun assert(a:Any?, b:Any?)= assertEquals(a.toString(),b.toString())
}