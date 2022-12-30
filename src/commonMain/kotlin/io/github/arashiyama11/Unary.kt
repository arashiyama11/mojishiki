package io.github.arashiyama11
import kotlin.math.floor
import kotlin.math.pow

class Unary(unaryString: String) :TermBase() {
  var polynomials: List<TermBase>
  var denoPolynomial: List<TermBase>

  init {
    val (p, d) = parse(unaryString)
    polynomials = p
    denoPolynomial = d
  }

  constructor(p:TermBase=Rational.ONE,dp:TermBase=Rational.ONE):this(""){
    polynomials=listOf(p)
    denoPolynomial=listOf(dp)
  }

  constructor(ps: List<TermBase>, dps: List<TermBase>? = null) : this("") {
    polynomials = ps
    denoPolynomial = dps ?: listOf(Rational.ONE)
  }

  companion object {
    val ZERO get() = Rational.ZERO.toUnary()
    val ONE get() = Rational.ONE.toUnary()
    val MINUS_ONE get()= Rational.MINUS_ONE.toUnary()
  }

  fun desuger(input:String):String{
    var a=0
    var str=input.trim()
    if(str[0]=='-'&&str[a+1].isLetter()){
      str="-1"+str.substring(1)
    }
    while(a<str.length){
      if(str[a].isDigit()){
        while(str[a].isDigit()){
          a++
          if(a==str.length)break
        }
        if(a!=str.length&&str[a]!=')'&&str[a]!='/') str=str.substring(0,a)+"*"+str.substring(a)
      }

      if(str[a]==')'&&a!=str.length-1&&(str[a+1].isLetterOrDigit()||str[a+1]=='(')){
        str=str.substring(0,a+1)+"*"+str.substring(a+1)
      }

      if(a>0&&str[a]=='('&&!validFunctions.any { str.substring(0, a).endsWith(it) }){
        val char=str[a-1]
        if(char.isLetterOrDigit()){
          str = str.substring(0, a) + "*" + str.substring(a)
        }
      }

      //—Ýæ‚Ìˆ—
      if (str[a] == '^') {
        var isBaseBrk = false
        var isFn = false

        val base = when (str[a - 1]){
          ')'->{
            isBaseBrk = true
            var depth = 0
            val r = str.substring(0, a - 1).takeLastWhile {
              when (it) {
                ')' -> {
                  depth++
                  true
                }
                '(' -> if (depth == 0) false else {
                  depth--
                  true
                }
                else -> true
              }
            }
            val s = str.substring(0, a - r.length - 2)
            val f = validFunctions.find { s.endsWith(it) }
            if (f != null) {
              isFn = true
              "$f($r)"
            } else r
          }
          in "1234567890"->{
            str.substring(0,a).takeLastWhile { it.isDigit() }
          }
          else->str[a-1].toString()
        }

        var isDBrk = false
        val d =when(str[a+1]){
          '('->{
            isDBrk = true
            var depth = 0
            str.substring(a + 2).takeWhile {
              when (it) {
                '(' -> {
                  depth++
                  true
                }
                ')' -> if (depth == 0) false else {
                  depth--
                  true
                }
                else -> true
              }
            }
          }
          '-'->"-"+str.substring(a+2).takeWhile { it.isDigit() }
          in "1234567890"-> str.substring(a+1).takeWhile { it.isDigit() }
          else->str[a+1].toString()
        }

        val p = "pow($base,$d)"
        str = str.substring(
          0,
          a - base.length - if (isBaseBrk && !isFn) 2 else 0
        ) + p + str.substring(a + d.length + if (isDBrk) 2 else 1)
      }

      a++
    }
    return str
  }

  private fun parse(input: String): Pair<List<TermBase>, List<TermBase>> {
    if (input.isEmpty()) return emptyList<TermBase>() to emptyList()
    val str = desuger(input)
    val strUnarys = mutableListOf<String>()
    var j = 0
    var depth = 0
    for (i in str.indices) {
      when (str[i]) {
        '(' -> depth++
        ')' -> depth--
        '*' -> if (depth == 0) {
          strUnarys += str.substring(j, i).trim()
          j = i + 1
        }
        '/' -> if (depth == 0) {
          strUnarys += str.substring(j, i).trim()
          j = i
        }
      }
    }
    strUnarys += str.substring(j).trim()
    val nums = mutableListOf<TermBase>()
    val denos = mutableListOf<TermBase>()
    strUnarys.filter{it.isNotEmpty()}.forEach {
      if (it[0] == '/') {
        denos += if (it.length > 1 && it[1] == '(' && it.last() == ')') {
          Polynomial(it.substring(2, it.length - 1).trim())
        } else {
          ExpressionUnit.parse(it.substring(1).trim())
        }
      } else {
        nums += if (it[0] == '(' && it.last() == ')') {
          Polynomial(it.substring(1, it.length - 1))
        } else {
          ExpressionUnit.parse(it)
        }
      }
    }
    if (denos.isEmpty()) denos += Rational.ONE
    return nums to denos
    /*
    strUnarys.filter { it.isNotEmpty() }.forEach {
      if (it[0] == '/') {
        denos += if (it.length > 1 && it[1] == '(' && it.last() == ')') {
          Polynomial(it.substring(2, it.length - 1).trim())
        } else {
          ExpressionUnit.parse(it.substring(1).trim())
        }
      } else {
        nums += if (it[0] == '(' && it.last() == ')') {
          Polynomial(it.substring(1, it.length - 1))
        } else {
          ExpressionUnit.parse(it)
        }
      }
    }
    if (denos.isEmpty()) denos += Rational.ONE
    return nums to denos*/
  }

  /*fun substitute(arg: Map<Char, Rational>): Unary {
    return Unary(polynomials.map {
      if (it is Unary) it.substitute(arg) else (it as Polynomial).substitute(arg)
    }, denoPolynomial.map {
      if (it is Unary) it.substitute(arg) else (it as Polynomial).substitute(arg)
    })
  }*/

  private fun evalPs(pols: List<TermBase>): TermBase {
    if (pols.isEmpty()) return Rational.ZERO
    if (pols.any { it.toPolynomial().arranged().isZero() }) return Rational.ZERO
    var ps = pols/*.map {
      //pow‚Ìˆ—
      if (it !is Unary && !it.canBeTerm()) return@map it
      val Unary = it.toTerm()
      if (!Unary.functions.containsKey("pow")) return@map it
      val fn = Unary.functions["pow"]!!
      if (!fn.args[1].canBeTerm()) return@map it
      val t = fn.args[1].toTerm()
      if (t.functions.isNotEmpty() || t.letters.isNotEmpty()) return@map it
      fn.args[0].toPolynomial().pow(t.coefficient.toInt()) * Unary(
        Unary.coefficient,
        Unary.letters,
        Unary.functions.filterKeys { k -> k != "pow" })
    }*/

    val sqrts = ps/*.filter { it is Unary && it.functions.containsKey("sqrt") }
      .map { (it as Unary).toTerm() }
    ps = if (sqrts.isNotEmpty()) {
      //sqrt‚Ìˆ—
      var coef = sqrts.map { Unary ->
        Unary(
          Unary.coefficient,
          Unary.letters,
          Unary.functions.filterKeys { it != "sqrt" }
        )
      }.reduce { acc, Unary -> acc * Unary }.toPolynomial()
      val funs = sqrts.map { it.functions["sqrt"]!! }
      var arg = funs.map {
        var d = it.degree
        val arg = it.args[0]
        while (d !in -1..1) {
          if (d > 1) {
            d -= 2
            coef *= arg.toPolynomial()
          } else {
            d += 2
            coef /= arg.toTerm()
          }
        }
        when (d) {
          1 -> arg
          -1 -> Unary(listOf(Unary.ONE), listOf(arg)).toPolynomial()
          else -> Unary.ONE
        }
      }.reduce { acc, tb -> if (acc is Unary && tb is Unary) acc * tb else acc.toPolynomial() * tb.toPolynomial() }
        .toPolynomial().evaluate()
      if (arg.canBeTerm()) {
        var t = arg.toTerm()
        var n = Rational.ONE
        if (t.coefficient.toDouble() < 0) {
          t *= -1.0
          coef *= Unary("i")
        }
        divisors(t.coefficient.numerator).groupBy { it.toInt() }.forEach { (t, u) ->
          if (u.size > 1) {
            n *= t.toDouble().pow(floor(u.size.toDouble() / 2)).toInt()
          }
        }
        divisors(t.coefficient.denominator).groupBy { it.toInt() }.forEach { (t, u) ->
          if (u.size > 1) {
            n /= t.toDouble().pow(floor(u.size.toDouble() / 2)).toInt()
          }
        }
        arg = (t.toPolynomial() / Unary(n * n)).evaluate()
        coef *= n
      }
      ps.filter { it !is Unary || !it.functions.containsKey("sqrt") } + coef.toTerm() + if (arg.isOne()) Unary(
        Rational.ONE
      ) else Unary(
        Rational.ONE,
        null,
        mapOf("sqrt" to FunctionValue(1, listOf(arg)))
      )
    } else ps*/
    return ps.reduce { acc, cur ->acc*cur
      /*when (cur) {
        is Unary -> if (acc is Unary) acc * cur else acc.toPolynomial().evaluate() * cur
        else -> if (acc is Unary) acc * cur.toPolynomial().evaluate() else acc.toPolynomial() * cur.toPolynomial()
          .evaluate()
      }*/
    }
  }

  fun evaluate(): TermBase {
    val u =
      evalPs(polynomials).toUnary()//.let { if (it is Polynomial) it.evaluate().factorization() else it.toUnary() }
    val d =
      evalPs(denoPolynomial).toUnary()/*.let { if (it is Polynomial) it.evaluate() else it }.toPolynomial().factorization()
    if (u.isZero()) return Rational.ZERO
    val us = (u.polynomials + d.denoPolynomial).toMutableList()
    val ds = (d.polynomials + u.denoPolynomial).toMutableList()
    var i = 0
    while (i < us.size && i < ds.size) {
      val j = us.indexOf(ds[i])
      if (j != -1) {
        ds.removeAt(i)
        us.removeAt(j)
      }
      i++
    }*/
    val us = (u.polynomials + d.denoPolynomial).toMutableList()
    val ds = (d.polynomials + u.denoPolynomial).toMutableList()
    if (us.size == 0) us += Rational.ONE
    if (ds.size == 0) ds += Rational.ONE

    val result = Unary(
      listOf(us.reduce { acc, p ->
        if(acc is Polynomial && p is Polynomial) (acc*p).evaluate()
        else acc*p
      }),
      listOf(ds.reduce { acc, p ->
        if(acc is Polynomial && p is Polynomial) (acc*p).evaluate()
        else acc*p
      })
    )
    return if (result.polynomials.size == 1 && result.denoPolynomial.size == 1 /*&& result.denoPolynomial[0].isOne()*/) {
      result.polynomials[0]
    } else result.toPolynomial()
  }

  /*fun approximation(): Unary {
    return Unary(polynomials.map { if (it is Unary) it.approximation() else (it as Polynomial).approximation() },
      denoPolynomial.map { if (it is Unary) it.approximation() else (it as Polynomial).approximation() }
    )
  }*/

  private fun psToString(ps: List<TermBase>, options: Set<String>?): String {
    val op = options ?: emptySet()
    return ps.joinToString("*")

    /*return ps
      .filter { if (it is Unary) !it.isOne() else true }
      .mapIndexed { index, it ->
        if (it is Unary || it.toPolynomial().unaries.size == 1) {
          val a = if (it is Unary) it else it.toTerm()
          //Œ»Ý‚Æ‘O‚ª—¼•û”Žš‚©‚Ç‚¤‚©
          val b = if (index > 0 && polynomials[index - 1] is Unary) polynomials[index - 1] as Unary else null
          val f =
            b != null && b.letters.isEmpty() && b.functions.isEmpty() && a.letters.isEmpty()
          if (f || index > 0 && polynomials[index - 1] is Unary && (polynomials[index - 1] as Unary).letters.isNotEmpty()) {
            "*${a.toStringWith(op)}"
          } else {
            a.toStringWith(op)
          }
        } else {
          "(${it})"
        }
      }.joinToString("")*/
  }

  override fun toString(): String {
    return when {
      denoPolynomial.size == 1 && denoPolynomial[0] is Rational &&(denoPolynomial[0] as Rational).toDouble()==1.0 -> if (polynomials.size == 1) polynomials[0].toString()
      else psToString(polynomials, null)
      denoPolynomial.isEmpty() || polynomials.isEmpty() -> ""
      else -> {
        val n = psToString(polynomials, null).ifEmpty { "1" }
        val d = psToString(denoPolynomial, null)
        if (d.isEmpty()) n
        else "$n/$d"
      }
    }
  }

  fun toStringWith(options: Set<String>): String {
    if (polynomials.size == 1) return polynomials[0].toString()
    return if (denoPolynomial.map { it.toPolynomial() }.reduce { acc, p -> acc + p }.isOne())
      psToString(polynomials, options)
    else "(${psToString(polynomials, options)})/${psToString(denoPolynomial, options)}"
  }

  operator fun times(double: Double): Unary {
    return times(Rational(double))
  }

  operator fun times(rational: Rational): Unary {
    return Unary(
      polynomials + Rational(rational.numerator),
      denoPolynomial + Rational(rational.denominator)
    )
  }


  operator fun times(unary: Unary): Unary {
    return Unary(polynomials + unary.polynomials, denoPolynomial + unary.denoPolynomial)
  }

  operator fun times(pol: Polynomial): Unary {
    return Unary(polynomials + pol, denoPolynomial)
  }

  operator fun div(double: Double): Unary {
    return Unary(polynomials, denoPolynomial + Rational(double))
  }


  operator fun div(unary: Unary): Unary {
    return Unary(polynomials, denoPolynomial + unary.toPolynomial())
  }

  operator fun div(pol: Polynomial): Unary {
    return Unary(polynomials, denoPolynomial + pol)
  }

  operator fun plus(unary: Unary): Unary {
    TODO()
    //return evaluate().toPolynomial() + unary.evaluate().toPolynomial()
  }

  operator fun unaryPlus(): Unary {
    return toUnary()
  }

  operator fun unaryMinus(): Unary {
    return times(Rational.MINUS_ONE)
  }

  fun pow(i: Int): Unary {
    var r: Unary
    val t: Unary
    var j = i
    when (i) {
      0 -> return ONE
      else -> if (i > 0) {
        r = toUnary()
        t = this
      } else {
        j = -i
        r = Unary(denoPolynomial, polynomials)
        t = r.toUnary()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r
  }



  override fun toUnary(): Unary {
    return Unary(polynomials.map {it.copy()
    }, denoPolynomial.map {it.copy() })
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(listOf(toUnary()))
  }

  fun isZero(): Boolean {
    return polynomials.any {
      if(it is Polynomial){
        it.isZero()
      }else when(it as ExpressionUnit) {
        is Letter -> false
        is Func -> false
        is Rational -> (it as Rational).numerator == 0L
      }
    }
  }

  fun isOne(): Boolean {
    val t = evaluate()
    return if(t is Polynomial){
      t.isOne()
    }else when(t as ExpressionUnit){
      is Letter->false
      is Func->false
      is Rational->(t as Rational).numerator==1L
    }
  }

  fun hasSameFuncAndLetter(other:Unary):Boolean{
    TODO()
  }

  override fun equals(other: Any?): Boolean {
    if (other is Unary) {
      val a = polynomials
      val b = other.polynomials
      val c = denoPolynomial
      val d = other.denoPolynomial
      return a.size == b.size && a.containsAll(b) && c.size == b.size && c.containsAll(d)
    }
    return false
  }


  private fun divisors(long: Long): MutableList<Long> {
    var n = long
    var i = 2L
    val result = mutableListOf<Long>()
    if (n < 0L) {
      n *= -1L
      result += -1L
    }
    while (true) {
      if (i * i > n) {
        result += n
        break
      }
      if (n % i == 0L) {
        result += i
        n /= i
        i = 1L
      }
      i++
    }
    return result
  }

  override fun hashCode(): Int {
    var result = polynomials.hashCode()
    result = 31 * result + denoPolynomial.hashCode()
    return result
  }

  override fun times(other: TermBase): TermBase {
    TODO("Not yet implemented")
  }

  override fun copy()=toUnary()
}