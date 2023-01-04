package io.github.arashiyama11

import kotlin.test.Test
import kotlin.test.assertEquals

class PolynomialTest {
  @Test
  fun parseAndToStringTest(){
    assert(Polynomial("1 + 5 * 2 - 8 / 2"), "1+10-4")
    assert(Polynomial("2x^2-5x+ 3x -1"), "2x^2-5x+3x-1")
    assert(Polynomial("x^2+2x"), "x^2+2x")
    assert(Polynomial("sin(x)+cos(y)"), "sin(x)+cos(y)")
    assert(Polynomial("max(min(sin(x),cos(y)),max(1,3))"), "max(min(sin(x),cos(y)),max(1,3))")
  }

  @Test
  fun evaluateTest(){
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
  fun powTest(){
    assert(Polynomial("x+2").pow(2), "x^2+4x+4")
    assert(Polynomial("x+1").pow(3), "x^3+3x^2+3x+1")
  }

  //@Test
  /*fun simplifyTest(){
    assert(Polynomial("x^2/2+x").simplify(), "x^2+2x")
    assert(Polynomial("6x^2+12x+4").simplify(), "3x^2+6x+2")
    assert(Polynomial("x^3+12x^2/5+24x/5+12/5").simplify().evaluate(), "5x^3+12x^2+24x+12")
  }*/

  @Test
  fun substituteTest(){
    assert(Polynomial("x^2+x+3").substitute(mapOf(Letter('x') to Rational(1))).evaluate(), "5")
    assert(Polynomial("(x+1)(x+2)-2").substitute(mapOf(Letter('x') to Rational(1.0))).evaluate(), "4")
    assert(Polynomial("x^2+y^2").substitute(mapOf(Letter('x') to Letter('y'))).evaluate(), "2y^2")
    assert(Polynomial("x^2").substitute(mapOf(Letter('x') to Polynomial("x+1"))).evaluate(), "x^2+2x+1")
    assert(Polynomial("sin(xsin(x))").substitute(mapOf(Letter('x') to Func("cos", listOf(Letter('x'))))).evaluate(), "sin(cos(x)sin(cos(x)))")
  }

  @Test
  fun approximationTest(){
    assert(Polynomial("max(2,3)-min(2,3)").approximation(),"3-2")
    assert(Polynomial("sin(0)cos(0)-sqrt(4)").approximation(),"-2")
  }

  @Test
  fun divTest(){
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
  fun factorizationTest(){
    assert(Polynomial("6x^2+7x+2").factorization(), "(2x+1)(3x+2)")
    assert(Polynomial("3x^2-2x-1").factorization(), "(x-1)(3x+1)")
    assert(Polynomial("x^3+2x^2-2x-12").factorization(), "(x-2)(x^2+4x+6)")
    assert(Polynomial("x^3-3x-2").factorization(), "(x+1)^2(x-2)")
    assert(Polynomial("6x^3+x^2+2x-1").factorization(), "(3x-1)(2x^2+x+1)")
    assert(Polynomial("2x^4+2x^3-4x^2-16x").factorization(), "2x(x-2)(x^2+3x+4)")
    assert(Polynomial("2a^2+2ax+ab+bx").factorization(), "(x+a)(b+2a)")
    assert(Polynomial("(a+b)(b+c)(c+a)").evaluate().factorization(), "(a+b)(c+a)(b+c)")
  }

  /*
  fun solveTest(){
    assert(Polynomial("2x-4").solve(), "[2]")
    assert(Polynomial("2a-4").solve('a'), "[2]")
    assert(Polynomial("a+2b").solve('b'), "[-a/2]")

    assert(Polynomial("3x+5").solve(), "[-5/3]")
    assert(Polynomial("x^2+5x+4").solve(), "[-1, -4]")
    assert(Polynomial("k^2+5k+4").solve('k'), "[-1, -4]")
    assert(Polynomial("x^2+4xy+4y^2").solve('x'), "[-2y, -2y]")

    assert(Polynomial("x^2+x+5").solve(), "[(-1+isqrt(19))/2, (-1-isqrt(19))/2]")
    assert(Polynomial("x^2-4").solve(), "[-2, 2]")
    assert(Polynomial("x^2+3x").solve(), "[0, -3]")
    assert(Polynomial("(x+1)(x-2)(x+3)").evaluate().solve(), "[-1, 2, -3]")
    assert(Polynomial("(x-a)(x-b)(x-c)").evaluate().solve(), "[a, b, c]")
    assert(Polynomial("x^3-1").solve(), "[1, (-1+isqrt(3))/2, (-1-isqrt(3))/2]")
    assert(Polynomial("x^3-6x^2+11x-30").solve(), "[5, (1+isqrt(23))/2, (1-isqrt(23))/2]")
  }

  @Test
  fun calculusTest(){
    assert(Polynomial("x^2").differential(), "2x")
    assert(Polynomial("3x^2+5x+1").differential(), "6x+5")
    assert(Polynomial("-a^3-2a+10").differential('a'), "-3a^2-2")
    assert(Polynomial("2sin(2x+1)").differential(), "4cos(2x+1)")
    assert(Polynomial("tan(x)").differential(), "cos(x)^-2")

    assert(Polynomial("2x^2+4x-1").integral(), "2x^3/3+2x^2-x+C")
    assert(Polynomial("v+at").integral('t'), "tv+at^2/2+C")
    assert(Polynomial("sin(x)").integral(), "-cos(x)+C")
  }*/

  /*@Test
  fun functionTest(){
    assert(Polynomial("3log(2)+log(5)-2log(3)").evaluate(), "log(40/9)")
    assert(Polynomial("sqrt(2)*2sqrt(3)").evaluate(), "2sqrt(6)")
    assert(Polynomial("sqrt(18/5)*sqrt(16/5)").evaluate(), "12sqrt(2)/5")
    //assert(Polynomial("min(5,-3)").approximation(), "-3")
    //assert(Polynomial("max(5,max(7,2))").approximation(), "7")
  }*/

  private fun assert(a:Any?, b:Any?)= assertEquals(b.toString(),a.toString())
}