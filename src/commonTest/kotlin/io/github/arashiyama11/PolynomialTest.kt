package io.github.arashiyama11

import kotlin.test.Test
import kotlin.test.assertEquals

class PolynomialTest {
  @Test
  fun parseAndToStringTest() {
    assert(Polynomial("1 + 5 * 2 - 8 / 2"), "1+10-4")
    assert(Polynomial("1.2+5.4-3.2/2"), "6/5+27/5-8/5")
    assert(Polynomial("2x^2-5x+ 3x -1"), "2x^2-5x+3x-1")
    assert(Polynomial("x^2+2x"), "x^2+2x")
    assert(Polynomial("sin(x)+cos(y)"), "sin(x)+cos(y)")
    assert(Polynomial("max(min(sin(x),cos(y)),max(1,3))"), "max(min(sin(x),cos(y)),max(1,3))")
  }

  @Test
  fun evaluateTest() {
    assert(Polynomial("x^2+2x+3x+3x^3").evaluate(), "3x^3+x^2+5x")
    assert(Polynomial("(x+1)(x+2)").evaluate(), "x^2+3x+2")
    assert(Polynomial("(x+2)^2(x+1)").evaluate(), "x^3+5x^2+8x+4")
    assert(Polynomial("(x+2y-z)^2").evaluate(), "x^2+4xy-2xz+4y^2-4yz+z^2")
    assert(
      Polynomial("(x+1)(x+2)(x+3)").evaluate(),
      (Polynomial("x+1") * Polynomial("x+2") * Polynomial("x+3")).evaluate().toString()
    )
    assert(Polynomial("(x+1)(x-2)").evaluate(), "x^2-x-2")
    assert(Polynomial("xyz^2-xy").evaluate(), "xyz^2-xy")
    assert(Polynomial("(x+1)(x+y)").evaluate(), "x^2+xy+x+y")
    assert(Polynomial("a(x^2+2y)").evaluate(), "ax^2+2ay")
    assert(Polynomial("(x+y)(x-y)").evaluate(), "x^2-y^2")
  }

  @Test
  fun powTest() {
    assert(Polynomial("x+2").pow(2), "x^2+4x+4")
    assert(Polynomial("x+1").pow(3), "x^3+3x^2+3x+1")
  }

  @Test
  fun substituteTest() {
    assert(Polynomial("x^2+x+3").substitute(mapOf(Letter('x') to Rational(1))).evaluate(), "5")
    assert(Polynomial("(x+1)(x+2)-2").substitute(mapOf(Letter('x') to Rational(1.0))).evaluate(), "4")
    assert(Polynomial("x^2+y^2").substitute(mapOf(Letter('x') to Letter('y'))).evaluate(), "2y^2")
    assert(Polynomial("x^2").substitute(mapOf(Letter('x') to Polynomial("x+1"))).evaluate(), "x^2+2x+1")
    assert(
      Polynomial("sin(xsin(x))").substitute(mapOf(Letter('x') to Func("cos", listOf(Letter('x'))))).evaluate(),
      "sin(cos(x)sin(cos(x)))"
    )
  }

  @Test
  fun approximationTest() {
    assert(Polynomial("max(2,3)-min(2,3)").approximation(), "3-2")
    assert(Polynomial("sin(0)cos(0)-sqrt(4)").approximation(), "-2")
  }

  @Test
  fun divTest() {
    assert(Polynomial("x^3-4x^2+x+6").divSafe(Polynomial("x-2")), "(x^2-2x-3, 0)")
    assert(Polynomial("x^2+4x+3").divSafe(Polynomial("x+1")), "(x+3, 0)")
    assert(Polynomial("(x+2)(x+1)(x+3)").evaluate() / Polynomial("x^2+5x+6"), "x+1")
    assert(Polynomial("x^2+x+3").divSafe(Polynomial("x+1")), "(x, 3)")
    assert(Polynomial("x^2+xy+y").divSafe(Polynomial("x+y")), "(x, y)")
    assert(Polynomial("x^2+xy+y").divSafe(Polynomial("x+y")), "(x, y)")
    assert(Polynomial("a^2-a-6").divSafe(Polynomial("a-3")), "(2+a, 0)")
    assert(Polynomial("a^2-a-8").divSafe(Polynomial("a-3")), "(2+a, -2)")
    assert(Polynomial("2x^2+3x+1").divSafe(Polynomial("x+1/2")), "(2x+2, 0)")
    assert(Polynomial("(3x+2)(2x+1)").evaluate().divSafe(Polynomial("x+2/3")), "(6x+3, 0)")
  }

  @Test
  fun factorizationTest() {
    assert(Polynomial("6x^2+7x+2").factorization(), "(2x+1)(3x+2)")
    assert(Polynomial("3x^2-2x-1").factorization(), "(x-1)(3x+1)")
    assert(Polynomial("x^3+2x^2-2x-12").factorization(), "(x-2)(x^2+4x+6)")
    assert(Polynomial("x^3-3x-2").factorization(), "(x+1)^2(x-2)")
    assert(Polynomial("6x^3+x^2+2x-1").factorization(), "(3x-1)(2x^2+x+1)")
    assert(Polynomial("2x^4+2x^3-4x^2-16x").factorization(), "2x(x-2)(x^2+3x+4)")
    assert(Polynomial("2a^2+2ax+ab+bx").factorization(), "(x+a)(b+2a)")
    assert(Polynomial("(a+b)(b+c)(c+a)").evaluate().factorization(), "(a+b)(c+a)(b+c)")
  }

  @Test
  fun solveTest() {
    assert(Polynomial("2x-4").solve(), "[2]")
    assert(Polynomial("2a-4").solve(Letter('a')), "[2]")
    assert(Polynomial("a+2b").solve(Letter('b')), "[-a/2]")

    assert(Polynomial("3x+5").solve(), "[-5/3]")
    assert(Polynomial("x^2+5x+4").solve(), "[-1, -4]")
    assert(Polynomial("k^2+5k+4").solve(Letter('k')), "[-1, -4]")
    assert(Polynomial("x^2+4xy+4y^2").solve(Letter('x')), "[-2y, -2y]")

    //assert(Polynomial("x^2+x+5").solve(), "[(-1+isqrt(19))/2, (-1-isqrt(19))/2]")
    assert(Polynomial("x^2+x+5").solve(), "[(-1+sqrt(-19))/2, (-1-sqrt(-19))/2]")
    assert(Polynomial("x^2-4").solve(), "[-2, 2]")
    assert(Polynomial("x^2+3x").solve(), "[0, -3]")
    assert(Polynomial("(x+1)(x-2)(x+3)").evaluate().solve(), "[-1, 2, -3]")
    assert(Polynomial("9x(x-a)(x-b)(x-c)").solve(), "[0, a, b, c]")
    //assert(Polynomial("x^3-1").solve(), "[1, (-1+isqrt(3))/2, (-1-isqrt(3))/2]")
    assert(Polynomial("x^3-1").solve(), "[1, (-1+sqrt(-3))/2, (-1-sqrt(-3))/2]")
    //assert(Polynomial("x^3-6x^2+11x-30").solve(), "[5, (1+isqrt(23))/2, (1-isqrt(23))/2]")
    assert(Polynomial("x^3-6x^2+11x-30").solve(), "[5, (1+sqrt(-23))/2, (1-sqrt(-23))/2]")
  }

  @Test
  fun calculusTest() {
    assert(Polynomial("x^2").differential(), "2x")
    assert(Polynomial("3x^2+5x+1").differential(), "6x+5")
    assert(Polynomial("(x+1)(x-2)+3").differential(), "2x-1")
    assert(Polynomial("-a^3-2a+10").differential(Letter('a')), "-3a^2-2")
    assert(Polynomial("2sin(2x+1)").differential(), "4cos(2x+1)")
    assert(Polynomial("tan(2x)").differential(), "2/cos(2x)^2")
    assert(Polynomial("log(x)").differential().evaluate(), "1/x")
    assert(Polynomial("3sin(3cos(4x))sin(zcos(o)(a-b))").differential(), "-36sin(4x)cos(3cos(4x))sin(zcos(o)(a-b))")
    assert(Polynomial("sin(x)cos(2x)").differential(), "cos(x)cos(2x)-2sin(x)sin(2x)")
    assert(Polynomial("3sin(a)/(2a)").differential(Letter('a')), "(6acos(a)-6sin(a))/4a^2")

    assert(Polynomial("2x^2+4x-1").integral(), "2x^3/3+2x^2-x+C")
    assert(Polynomial("v+at").integral(Letter('t')), "tv+at^2/2+C")
    assert(Polynomial("sin(x)").integral(), "-cos(x)+C")
    assert(Polynomial("2cos(x)y").integral(), "2ysin(x)+C")
    assert(Polynomial("2/x^3").integral(), "C-1/x^2")
    assert(Polynomial("2/x").integral(), "2log(x)+C")
  }

  @Test
  fun functionTest() {
    assert(Polynomial("log(2)+log(5)").evaluate(), "log(10)")
    assert(Polynomial("3log(2)+log(5)-2log(3)").evaluate(), "log(40/9)")
  }

  private fun assert(a: Any?, b: Any?) = assertEquals(b.toString(), a.toString())
}