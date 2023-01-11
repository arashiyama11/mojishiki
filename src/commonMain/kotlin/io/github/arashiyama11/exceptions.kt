package io.github.arashiyama11

class ParseException(input: String, at: Int, message: String? = null) : Exception(
  """${message ?: "Unexpected token ${input[at]}"}
$input
${" ".repeat(at)}^
  """
)

class UnknownTermBaseInstanceException : Exception()

class InvalidLetterException(char: Char) : Exception("$char is invalid as Letter.\nValid letters are '${Letter.valid}'")

class InvalidFuncException(override val message: String?) : Exception()