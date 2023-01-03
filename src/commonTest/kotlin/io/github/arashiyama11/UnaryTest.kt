package io.github.arashiyama11

import kotlin.test.Test
import kotlin.test.assertEquals

class UnaryTest {
  @Test
  fun parseTest(){
    assert(Unary("334"), "334")
    assert(Unary("-334"), "-334")
    assert(Unary("2^10"), "1024")
    assert(Unary("-3^2"), "-9")
    assert(Unary("(-3)^2"), "9")
    assert(Unary("3x"), "3x")
    assert(Unary("x(x+1)"), "x(x+1)")
    assert(Unary("-2(x+1)"), "-2(x+1)")
    assert(Unary("(x+2)(x+1)"), "(x+2)(x+1)")
    assert(Unary("sin(x+1)cos(x)"), "sin(x+1)cos(x)")
    assert(Unary("sqrt(sin(x))"), "sqrt(sin(x))")
    assert(Unary("x^(x+1)"), "x^(x+1)")
    assert(Unary("(x+2)^3"), "(x+2)^3")
    assert(Unary("ab^(xy)"), "ab^(xy)")
    assert(Unary("(5x)^(2y)"), "(5x)^(2y)")
    assert(Unary("(5x)^2y"), "25x^2y")
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
    assert(Polynomial("(sin(x)+1)(2sin(x)+1)").evaluate(), "2sin(x)^2+3sin(x)+1")
  }

  @Test
  fun substituteTest(){
    assert(Unary("x(x+1)").substitute(mapOf(Letter('x') to Rational(1.0))).evaluate(), "2")
    assert(Unary("(x+1)(x+3)sin(2x)").substitute(mapOf(Letter('x') to Letter('y'))), "sin(2y)(y+1)(y+3)")
    assert(Unary("(x+2)(x+1)").substitute(mapOf(Letter('x') to Unary("-3abc"))), "(-3abc+2)(-3abc+1)")
    assert(Unary("xysin(x)").substitute(mapOf(Letter('x') to Polynomial("y+1"))), "ysin((y+1))(y+1)")
  }

  @Test
  fun approximationTest(){
    assert(Unary("3xcos(0)").approximation(),"3x")
    assert(Unary("3xcos(x)").approximation(),"3xcos(x)")
    assert(Unary("-5xyz").approximation(),"-5xyz")
    assert(Unary("max(5,2)min(3,-2)abs(-12i)").approximation(),"-10abs(-12i)")
  }

  private fun assert(a:Any?, b:Any?)= assertEquals(b.toString(),a.toString())
}