sealed trait Token
case class Operator(lexeme: String) extends Token
sealed trait Expr
case class Number(num: Double) extends Token with Expr
case class Arithmetic(lhs: Expr, op: Operator, rhs: Expr) extends Expr

def tokenize(input: String): Iterator[Token] = {
  val chars = input.toIterator.buffered
  for (c <- chars.toIterator.buffered if !c.isWhitespace)
    yield c match {
      case '+' | '-' =>
        Operator(c.toString)

      case n if n.isDigit =>
        Number((n + takeWhile[Char](chars, { _.isDigit }).mkString).toDouble)
    }
}

def takeWhile[T](src: BufferedIterator[T], predicate: (T) => Boolean): List[T] =
  if (src.isEmpty)
    Nil
  else if (!predicate(src.head))
    Nil
  else
    src.next :: takeWhile(src, predicate)

def parse(tokens: List[Token]): Either[String, Expr] =
  tokens match {
    case Nil => Left("invalid: empty input")
    case (_: Operator) :: _ => Left("invalid: cannot start expression with operator")
    case (token : Number) :: _ => Right(token)
  }

def parse(tokens: Iterator[Token]): Either[String, Expr] = parse(tokens.toList)
def parse(tokens: List[Token]): Either[String, Expr] =
  tokens match {
    // Valid expressions
    case (num : Number) :: Nil =>
      Right(num)
    case (lhs : Number) :: (op : Operator) :: (rhs : Number) :: Nil =>
      Right(Arithmetic(lhs, op, rhs))
    case (lhs : Number) :: (op1 : Operator) :: (rhs : Number) :: (op2 : Operator) :: t =>
      val rhs = parse(t).fold(err => return Left(err),
                              ok => ok)
      Right(Arithmetic(Arithmetic(lhs, op1, rhs), op2, rhs))

    // Invalid expressions
    case Nil => Left("syntax error: empty input")
    case _ => Left("syntax error: expressions are binary expressions or single numbers")
  }


def eval(expr: Expr): Either[String, Expr] =
  expr match {
    case num : Number => Right(num)
    case Arithmetic(Number(lhs), Operator(op), Number(rhs)) =>
      op match {
        case "+" => Right(Number(lhs + rhs))
        case "-" => Right(Number(lhs - rhs))
        case _   => Left(s"error: invalid operator `$op`")
      }
  }

parse(tokenize("40 + 2")).flatMap(eval)
