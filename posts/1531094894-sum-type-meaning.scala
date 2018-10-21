class Token(lexeme: String)
case class Operator(lexeme: String) extends Token(lexeme)
case class Number(lexeme: String) extends Token(lexeme)

class Expression
case class Arithmetic(lhs: Number, op: Operator, rhs: Number)
  extends Expression


val exp = Arithmetic(Number("2"), Operator("+"), Number("40"))

println(exp)

// object Main {
//   def main(args: Array[String]): Unit = {
//     println("hi")
//   }
// }
