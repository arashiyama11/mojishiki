package io.github.arashiyama11

import kotlin.test.Test
import kotlin.test.assertEquals

class TermTest {
  @Test
  fun parseAndToStringTest(){
    assert(Term("1"), "1")
    assert(Term("+ 2"), "2")
    assert(Term("1.2"), "6/5")
    assert(Term("x"), "x")
    assert(Term("- x"), "-x")
    assert(Term("-2x"), "-2x")
    assert(Term("2x^3"), "2x^3")
    assert(Term("2x^12"), "2x^12")
    assert(Term("-1.5x^12"), "-3x^12/2")

    assert(Term("y"), "y")
    assert(Term("y^2"), "y^2")
    assert(Term("x^3y^2"), "x^3y^2")
    assert(Term("a^3bc"), "a^3bc")

    assert(Term("i^2"), "-1")
    assert(Term("5i^5"), "5i")
    assert(Term("i^3"), "-i")
    assert(Term("-8i^3"), "8i")

    assert(Term("sin(x)"), "sin(x)")
    assert(Term("siny"), "sin(y)")
    assert(Term("3sin(2x)"), "3sin(2x)")
    assert(Term("-3sin(2x^2+3x-i)"), "-3sin(2x^2+3x-i)")
    assert(Term("sin(x)^2"), "sin(x)^2")
  }

  @Test
  fun substituteTest(){
    assert(Term("x").substitute(mapOf('x' to Rational(2.0))), "2")
    assert(Term("2x^2").substitute(mapOf('x' to Rational(2.0))), "8")
    assert(Term("x^3").substitute(mapOf('x' to Rational(3.0))), "27")
    assert(Term("abc").substitute(mapOf('a' to Rational(1), 'b' to Rational(2), 'c' to Rational(3))), "6")
  }

  @Test
  fun approximationTest(){
    assert(Term("3cos(0)").approximation(), "3")
    assert(Term("sqrt(4)").approximation(), "2")
    assert(Term("sqrt(-9)").approximation(), "3i")
  }

  @Test
  fun calculateTest(){
    assert(Term("1") + Term("2"), "3")
    assert(Term("1") * Term("2"), "2")
    assert(Term("2x") + Term("3x"), "5x")
    assert(Term("2x^2") + Term("3x^2"), "5x^2")
    assert(Term("-2x^2") * Term("3x^2"), "-6x^4")
    assert(Term("-3x^2") * Term("-6"), "18x^2")

    assert(Term("3xsin(y)") + Term("7xsiny"), "10xsin(y)")

    assert(Term("x") * Term("3y"), "3xy")
    assert(Term("5xy^2z") + Term("2xy^2z"), "7xy^2z")
    assert(Term("-ab^2") * Term("x^3y"), "-ab^2x^3y")

    assert(Term("sinx") * Term("tany"), "sin(x)tan(y)")

    assert(Term("8") / 2.0, "4")
    assert(Term("100") / Term("20"), "5")
    assert(Term("xy^2") / Term("xy"), "y")
  }

  private fun assert(a:Any?, b:Any?)= assertEquals(a.toString(),b.toString())
}