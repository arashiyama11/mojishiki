package io.github.arashiyama11

import io.github.arashiyama11.ExpressionUnit.Companion.parse
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpressionUnitTest {
  @Test
  fun parseAndToStringTest(){
    assert(parse("3"),"3")
    assert(parse("3.14"),"157/50")
    assert(parse("22/7"),"22/7")
    assert(parse("-2"),"-2")
    assert(parse("- 12"),"-12")
    assert(parse("-12.5"),"-25/2")
    assert(parse("-1/2"),"-1/2")

    assert(parse("x"),"x")
    assert(parse("y"),"y")

    assert(parse("sin(x)"),"sin(x)")
    assert(parse("pow(2x,3)"),"(2x)^3")
    assert(parse("pow(x,3x+1)"),"x^(3x+1)")
  }

  private fun assert(a:Any?, b:Any?)= assertEquals(b.toString(),a.toString())
}