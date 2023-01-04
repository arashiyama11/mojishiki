package io.github.arashiyama11
import kotlin.math.absoluteValue

class Unary(unaryString: String) :TermBase() {
  var termBases: List<TermBase>
  var denoTermBases: List<TermBase>

  var rational=Rational.ONE
  var letters= mutableMapOf<Letter,Int>()
  var funcs= mutableMapOf<Func,Int>()
  var pols= mutableMapOf<Polynomial,Int>()

  init {
    val (p, d) = parse(unaryString)
    termBases = p
    denoTermBases = d
    autoEvaluate()
    classification()
  }

  constructor(p:TermBase=Rational.ONE,dp:TermBase=Rational.ONE):this(""){
    termBases=listOf(p)
    denoTermBases=listOf(dp)
    autoEvaluate()
    classification()
  }

  constructor(ps: List<TermBase>, dps: List<TermBase>? = null) : this("") {
    termBases = ps
    denoTermBases = dps ?: listOf(Rational.ONE)
    autoEvaluate()
    classification()
  }

  constructor(rat: Rational=Rational.ONE,lts:Map<Letter,Int>?=null,fns:Map<Func,Int>?=null,ps:Map<Polynomial,Int>?=null):this(""){
    val tb= mutableListOf<TermBase>(Rational(rat.numerator))
    val dtb= mutableListOf<TermBase>(Rational(rat.denominator))

    lts?.forEach { (l,i)->
      if(i>0) for(j in 0 until i) tb+=l
      else if(i<0) for(j in 0 until -i) dtb+=l
    }

    fns?.forEach { (f,i)->
      if(i>0) for(j in 0 until i) tb+=f
      else if(i<0) for(j in 0 until -i) dtb+=f
    }

    ps?.forEach { (p,i)->
      if(i>0) for(j in 1 until i) tb+=p
      else if(i<0) for(j in 1 until -i) dtb+=p
    }

    termBases=tb
    denoTermBases=dtb

    autoEvaluate()
    classification()
  }

  companion object {
    val ZERO get() = Rational.ZERO.toUnary()
    val ONE get() = Rational.ONE.toUnary()
    val MINUS_ONE get() = Rational.MINUS_ONE.toUnary()
  }

  private fun autoEvaluate(){
    val tb= mutableListOf<TermBase>()
    val dtb= mutableListOf<TermBase>()

    for(t in termBases.map{if(it is Polynomial&&it.canBeUnary()) it.toUnary() else it}){
      when(t){
        is Unary->{
          tb+=t.termBases
          dtb+=t.denoTermBases
        }
        is Func->when(t.name){
            "pow"->if(t.args[0].canBeUnary()&&t.args[1].canBeUnary()){
              val b=t.args[0].toUnary()
              val d=t.args[1].toUnary()
              if(d.canBeRational()&&b.pols.isEmpty()){
                val a=d.toRational().toInt()
                if(a>0){
                  for(i in 1 .. a){
                    val copy=b.toUnary()
                    tb+=copy.termBases
                    dtb+=copy.denoTermBases
                  }
                }else if(a<0){
                  for(i in 1 until  -a){
                    val copy=b.toUnary()
                    dtb+=copy.denoTermBases
                    tb+=copy.termBases
                  }
                }
              }else tb+=t
            }else tb+=t
          else->tb+=t
          }
        else->tb+=t
      }
    }

    for(t in denoTermBases.map{if(it is Polynomial&&it.canBeUnary()) it.toUnary() else it}){
      when(t){
        is Unary->{
          tb+=t.denoTermBases
          dtb+=t.termBases
        }

        is Func->when(t.name){
          "pow"->if(t.args[0] is Unary&&t.args[1] is Unary){
            val b=t.args[0] as Unary
            val d=t.args[1] as Unary
            if(d.canBeRational()&&b.pols.isEmpty()){
              val a=d.toRational().toInt()
              if(a>0){
                for(i in 1 .. a){
                  val copy=b.copy()
                  tb+=copy.denoTermBases
                  dtb+=copy.termBases
                }
              }else if(a<0){
                for(i in 1 until  -a){
                  val copy=b.copy()
                  dtb+=copy.termBases
                  tb+=copy.denoTermBases
                }
              }
            }
          }
          else->dtb+=t
        }
        else->dtb+=t
      }
    }

    termBases=tb
    denoTermBases=dtb
  }

  private fun classification(){
    termBases.forEach {
      if(it is ExpressionUnit) when(it){
        is Rational->rational*=it
        is Letter->if(letters.containsKey(it)) letters[it]=letters[it]!!+1 else letters+=it to 1
        is Func->if(funcs.containsKey(it)) funcs[it]=funcs[it]!!+1 else funcs+=it to 1
      }else{
        val p=it as Polynomial
        if(pols.containsKey(p)) pols[p]=pols[p]!!+1 else pols+=p to 1
      }
    }

    denoTermBases.forEach {
      if(it is ExpressionUnit)when(it){
        is Rational->rational/=it
        is Letter->if(letters.containsKey(it)) letters[it]=letters[it]!!-1 else letters+=it to -1
        is Func->if(funcs.containsKey(it)) funcs[it]=funcs[it]!!-1 else funcs+=it to -1
      }else{
        val p=it as Polynomial
        if(pols.containsKey(p)) pols[p]=pols[p]!!-1 else pols+=p to -1
      }
    }
  }

  fun desuger(input:String):String{
    var a=0
    var str=input.trim()

    while(a<str.length){
      if(str[a].isDigit()){
        while(str[a].isDigit()){
          a++
          if(a==str.length)break
        }
        if(a!=str.length&&str[a] !in ")/,*^") str=str.substring(0,a)+"*"+str.substring(a)
      }

      if(a+1<str.length&&str[a].isLetter()&&str[a+1].isLetter()){
        val i= validFunctions.indexOfFirst { str.substring(a).startsWith(it) }
        if(i>=0){
          a+=validFunctions[i].length
          while(str[a]!=')'){
            a++
          }
        }else str=str.substring(0,a+1)+"*"+str.substring(a+1)
      }

      if(a+1<str.length&&str[a]==')'&&a!=str.length-1&&(str[a+1].isLetterOrDigit()||str[a+1]=='(')){
        str=str.substring(0,a+1)+"*"+str.substring(a+1)
      }

      if(a+1<str.length&&a>0&&str[a]=='('&&!validFunctions.any { str.substring(0, a).endsWith(it) }){
        val char=str[a-1]
        if(char.isLetterOrDigit()){
          str = str.substring(0, a) + "*" + str.substring(a)
        }
      }

      //ó›èÊÇÃèàóù
      if (a+1<str.length&&str[a] == '^') {
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

    if(str.length>1&&str[0]=='-'&&str[1].isLetter()){
      str="-1*"+str.substring(1)
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
  }

  override fun substitute(entries: Map<Letter, TermBase>)=
    Unary(termBases.map { it.substitute(entries) },denoTermBases.map { it.substitute(entries) })

  private fun evalPs(pols: List<TermBase>): TermBase {
    if (pols.isEmpty()) return Rational.ZERO
    if (pols.any { it.toPolynomial().arranged().isZero() }) return Rational.ZERO
    val ps = mutableListOf<TermBase>()
    pols.forEach {
      when(it){
        is Func->when(it.name){
            "pow"->{
              val b=it.args[0]
              val d=it.args[1]
              if(d.canBeUnary()&&d.toUnary().canBeRational()){
                for(i in 0 until d.toUnary().toRational().toInt())ps+=b
              }else ps+=it
            }
            else->ps+=it
          }
        else->ps+=it
      }
    }
    return ps.reduce { acc, cur ->(acc*cur) }
  }


  fun evaluate(): TermBase {
    val u =
      evalPs(termBases).let{if(it is Polynomial) it.arranged() else it}//.toPolynomial().evaluate()//.let { if (it is Polynomial) it.evaluate().factorization() else it.toUnary() }
    val d =
      evalPs(denoTermBases).let{if(it is Polynomial) it.arranged() else it}
    return if(d.toString()=="1") u else Unary(u,d)

    /*.let { if (it is Polynomial) it.evaluate() else it }.toPolynomial().factorization()
    if (u.isZero()) return Rational.ZERO
    val us = (u.termBases + d.denoTermBases).toMutableList()
    val ds = (d.termBases + u.denoTermBases).toMutableList()
    var i = 0
    while (i < us.size && i < ds.size) {
      val j = us.indexOf(ds[i])
      if (j != -1) {
        ds.removeAt(i)
        us.removeAt(j)
      }
      i++
    }
    val us = (u.termBases + d.denoTermBases).toMutableList()
    val ds = (d.termBases + u.denoTermBases).toMutableList()
    if (us.size == 0) us += Rational.ONE
    if (ds.size == 0) ds += Rational.ONE

    val result = Unary(
      listOf(us.reduce { acc, p ->acc*p
      }),
      listOf(ds.reduce { acc, p ->acc*p
      })
    )
    return if (result.termBases.size == 1 && result.denoTermBases.size == 1 /*&& result.denoTermBases[0].isOne()*/) {
      result.termBases[0]
    } else result.toPolynomial()*/
  }

  override fun approximation()
    =Unary(termBases.map{it.approximation()},denoTermBases.map{it.approximation()})


  private fun psToString(ps: List<TermBase>, options: Set<String>?=null): String {

    val op = options ?: emptySet()
    return ps
      .mapIndexed { i, it ->
        if(it is Polynomial){
          "($it)"
        }else{
          if(i>0&&((ps[i-1] is Unary&&(ps[i-1] as Unary).canBeRational())||ps[i-1] is Rational)) "*$it"
          else "$it"
        }
        /*if (it is Unary || it.toPolynomial().unaries.size == 1) {
          val a = if (it is Unary) it else it.toTerm()
          //åªç›Ç∆ëOÇ™óºï˚êîéöÇ©Ç«Ç§Ç©
          val b = if (index > 0 && termBases[index - 1] is Unary) termBases[index - 1] as Unary else null
          val f =
            b != null && b.letters.isEmpty() && b.functions.isEmpty() && a.letters.isEmpty()
          if (f || index > 0 && termBases[index - 1] is Unary && (termBases[index - 1] as Unary).letters.isNotEmpty()) {
            "*${a.toStringWith(op)}"
          } else {
            a.toStringWith(op)
          }
        } else {
          "(${it})"
        }*/
      }.joinToString("")
  }

  fun exprToString(exp:List<MutableMap.MutableEntry<out TermBase,Int>>?):String{
    if(exp==null)return ""
    return exp.let { if(it.isNotEmpty()&&it[0].key is Letter) it.sortedBy { (it.key as Letter).letter } else it }.mapIndexed { i, (tb,d) ->
      if(d==0)return@mapIndexed ""
      (if(tb is Polynomial) "($tb)" else "$tb")+when(d){
        1->""
        else->"^${d.absoluteValue}"
      }
    }.joinToString("")
  }

  override fun toString(): String {
    val lts = letters.entries.groupBy { it.value > 0 }
    val fns = funcs.entries.groupBy { it.value > 0 }
    val ps = pols.entries.groupBy { it.value > 0 }

    var n = "${rational.numerator}${exprToString(lts[true])}${exprToString(fns[true])}${exprToString(ps[true])}"
    var d = "${rational.denominator}${exprToString(lts[false])}${exprToString(fns[false])}${exprToString(ps[false])}"

    if (n.length > 1 && n[0] == '1'&&!n[1].isDigit()) n = n.substring(1)
    else if(n.length>2&&n[0]=='-'&&n[1]=='1'&&!n[2].isDigit())n='-'+n.substring(2)
    if (d.length > 1 && d[0] == '1'&&!n[1].isDigit()) d = d.substring(1)
    else if(d.length>2&&d[0]=='-'&&d[1]=='1'&&!d[2].isDigit())d='-'+d.substring(2)

    return if (d == "1") n
    else "$n/$d"
  }


  fun toStringWith(options: Set<String>): String {
    if (termBases.size == 1) return termBases[0].toString()
    return if (denoTermBases.map { it.toPolynomial() }.reduce { acc, p -> acc + p }.isOne())
      psToString(termBases, options)
    else "(${psToString(termBases, options)})/${psToString(denoTermBases, options)}"
  }

  operator fun times(double: Double): Unary {
    return times(Rational(double))
  }

  operator fun times(rational: Rational): Unary {
    return Unary(
      termBases + Rational(rational.numerator),
      denoTermBases + Rational(rational.denominator)
    )
  }

  operator fun times(letter:Letter):Unary{
    return Unary(termBases+letter,denoTermBases.toList())
  }

  operator fun times(func:Func):Unary{
    return Unary(termBases+func,denoTermBases.toList())
  }

  operator fun times(unary: Unary): Unary {
    return Unary(termBases + unary.termBases, denoTermBases + unary.denoTermBases)
  }

  operator fun times(pol: Polynomial): Unary {
    return Unary(termBases + pol, denoTermBases)
  }

  operator fun times(exp:ExpressionUnit):Unary{
    return when(exp){
      is Rational->times(exp)
      is Letter->times(exp)
      is Func->times(exp)
    }
  }

  override fun times(other: TermBase): TermBase {
    return when(other){
      is ExpressionUnit->times(other)
      is Unary->times(other)
      is Polynomial->times(other)
      else->throw Exception("")
    }
  }

  operator fun div(double: Double): Unary {
    return Unary(termBases, denoTermBases + Rational(double))
  }


  operator fun div(unary: Unary): Unary {
    return Unary(termBases, denoTermBases + unary.toPolynomial())
  }

  operator fun div(pol: Polynomial): Unary {
    return Unary(termBases, denoTermBases + pol)
  }

  operator fun plus(unary: Unary): Unary {
    if(!hasSameFuncAndLetter(unary))throw Exception("Cannot plus")
    return Unary(rational+unary.rational,letters,funcs,pols)
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
        r = Unary(denoTermBases, termBases)
        t = r.toUnary()
      }
    }
    for (a in 1 until j) {
      r *= t
    }
    return r
  }

  override fun canBeUnary() = true

  override fun toUnary(): Unary {
    return Unary(termBases.map {it.copy()
    }, denoTermBases.map {it.copy() })
  }

  override fun toPolynomial(): Polynomial {
    return Polynomial(listOf(toUnary()))
  }

  fun isZero(): Boolean {
    return termBases.any {
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

  fun toRational():Rational{
    if(!canBeRational()) throw ClassCastException("")
    return rational.toRational()
  }

  fun canBeRational()=letters.isEmpty()&&funcs.isEmpty()&&pols.isEmpty()

  fun hasSameFuncAndLetter(other:Unary)=letters==other.letters&&funcs==other.funcs&&pols==other.pols

  override fun equals(other: Any?): Boolean {
    if (other is Unary) {
      return rational==other.rational&&letters==other.letters&&funcs==other.funcs
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
    var result = termBases.hashCode()
    result = 31 * result + denoTermBases.hashCode()
    return result
  }

  override fun copy()=toUnary()

  fun reciprocal()=Unary(denoTermBases.map{it.copy()},termBases.map{it.copy()})
}