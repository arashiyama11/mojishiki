<h1 align="center">mojishiki</h1>

# About

This is a Kotlin code that allows you to calculate literal-expression.

Mojishiki means literal-expression in Japanese
## Features
- Pure Kotlin
- Multiplatform

# Installation
## Grade
```
//gradle kotlin DSL
implementation("io.github.arashiyama11:mojishiki:1.0.2")

//gradle groovy DSL
implementation 'io.github.arashiyama11:mojishiki:1.0.2'
```

## Maven
```xml
<dependency>
  <groupId>io.github.arashiyama11</groupId>
  <artifactId>mojishiki</artifactId>
  <version>1.0.2</version>
</dependency>
```


# Example usage

1. Basic arithmetic operations

```kotlin
Polynomial("1+5*2-8/2").evaluate() //7
Polynomial("2x-y+3x+y").evaluate() //5x
Polynomial("2x+y+1") + Polynomial("2y-3").evaluate() //2x+3y-2
```

2. Expansion and factorization

```kotlin
Polynomial("(x+y)(x+2)").evaluate() //x^2+2x+xy+2y
Polynomial("6x^2+7x+2").factorization() //(2x+1)(3x+2)
```

3. Functions and Complex number

```kotlin
Polynomial("sin(1)")
  .approximation()
  .toStringWith(setOf("double")) //0.8414709851576089
Polynomial("sqrt(-25)").approximation()  //5i
Polynomial("3i*2i").evaluate() //-6.0
Polynomial("sqrt(-2)sqrt(-2)").evaluate() //-2.0
```

Supporting functions are

- sin cos tan
- log
- sqrt abs
- min max

4. Substitute

```kotlin
Polynomial("pr^2")
  .substitute(mapOf('p' to Rational(3.14), 'r' to Rational(3)))
// 1413/50 
```

5. Solve as equation

NOTE:Unfactorable equations of degree 3 or higher cannot be solved

```kotlin
Polynomial("2x-4").solve() //[2]
Polynomial("x^2+5x+4").solve() //[-1, -4]
```

6. Basic differentiation and integration

```kotlin
Polynomial("3x^2+5x+1").differential() //6x+5
Polynomial("3sin(3cos(4x))").differential() //-36cos(3cos(4x))sin(4x)
Polynomial("8x^3+4x").integral() //2x^4+2x^2+C
```


# Licence

MIT