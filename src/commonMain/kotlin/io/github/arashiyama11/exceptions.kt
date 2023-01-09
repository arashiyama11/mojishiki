package io.github.arashiyama11

class ParseException(input: String, at: Int, message: String? = null) : Exception(
  """${message ?: "Unexpected token ${input[at]}"}
$input
${" ".repeat(at)}^
  """
)