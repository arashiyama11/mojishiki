package io.github.arashiyama11

import kotlin.test.Test
import kotlin.test.assertEquals

class UnaryTest {
  @Test
  fun parseTest() {
    assert(Unary("334"), "334")
    assert(Unary("33.4"), "167/5")
    assert(Unary("-334"), "-334")
    assert(Unary("-3.34"), "-167/50")
    assert(Unary("2^10"), "1024")
    assert(Unary("-3^2"), "-9")
    assert(Unary("(-3)^2"), "9")
    assert(Unary("3x"), "3x")
    assert(Unary("3.2x"), "16x/5")
    assert(Unary("x(x+1)"), "x(x+1)")
    assert(Unary("-2(x+1)"), "-2(x+1)")
    assert(Unary("-2.12(x+1)"), "-53(x+1)/25")
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
    assert(Unary("(x+y)*(x+z)"), "(x+y)(x+z)")
    assert(Unary("2.5*2.0*3.0*0.5"), "15/2")
  }

  @Test
  fun calculateTest() {
    assert(Unary("x") * Unary("(x+1)"), "x(x+1)")
    assert(Unary("ab") * Unary("(x+1)"), "ab(x+1)")
    assert((Unary("12") * Unary("3")).evaluate(), "36")
    assert((Unary("3x") * Unary("2x")).evaluate(), "6x^2")
    assert((Unary("x^3") * Unary("2x")).evaluate(), "2x^4")
    assert(Polynomial("(sin(x)+1)(2sin(x)+1)").evaluate(), "2sin(x)^2+3sin(x)+1")
  }

  @Test
  fun substituteTest() {
    assert(Unary("x(x+1)").substitute(mapOf(Letter('x') to Rational(1.0))).evaluate(), "2")
    assert(Unary("(x+1)(x+3)sin(2x)").substitute(mapOf(Letter('x') to Letter('y'))), "sin(2y)(y+1)(y+3)")
    assert(Unary("(x+2)(x+1)").substitute(mapOf(Letter('x') to Unary("-3abc"))), "(-3abc+2)(-3abc+1)")
    assert(Unary("xysin(x)").substitute(mapOf(Letter('x') to Polynomial("y+1"))), "ysin((y+1))(y+1)")
  }

  @Test
  fun approximationTest() {
    assert(Unary("3xcos(0)").approximation(), "3x")
    assert(Unary("3xcos(x)").approximation(), "3xcos(x)")
    assert(Unary("-5xyz").approximation(), "-5xyz")
    assert(Unary("max(5,2)min(3,-2)abs(-12i)").approximation(), "-10abs(-12i)")
  }

  @Test
  fun functionsTest() {
    assert(Unary("sqrt(20/9)").evaluate(), "2sqrt(5)/3")
    assert(Unary("sqrt(36)").evaluate(), "6")
    assert(Unary("sqrt(-12)").evaluate(), "2isqrt(3)")
    assert(Unary("sqrt(2)*2sqrt(3)").evaluate(), "2sqrt(6)")
    assert(Unary("sqrt(-2)*2sqrt(-3)").evaluate(), "-2sqrt(6)")
    assert(Unary("sqrt(18/5)sqrt(16/5)").evaluate(), "12sqrt(2)/5")
    assert(Unary("sqrt(18/5)sqrt(-16)").evaluate(), "12isqrt(2/5)")
    assert(Unary("pow(x,2x)pow(x,3y)").evaluate(), "x^(2x+3y)")
  }

  private fun assert(a: Any?, b: Any?) = assertEquals(b.toString(), a.toString())
}