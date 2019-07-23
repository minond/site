sealed trait Token
sealed trait Operator extends Token
case object Plus extends Operator
case object Minus extends Operator
case object Mult extends Operator
sealed trait Expr
case class Number(num: Double) extends Token with Expr
case class Arithmetic(lhs: Expr, op: Operator, rhs: Expr) extends Expr

def tokenize(input: String): Iterator[Token] = {
  val chars = input.toIterator.buffered
  for (c <- chars.toIterator.buffered if !c.isWhitespace)
    yield c match {
      case '+' => Plus
      case '-' => Minus
      case '*' => Mult

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
    case Nil                   => Left("invalid: empty input")
    case (_: Operator) :: _    => Left("invalid: cannot start expression with operator")
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
    case (lhs1 : Number) :: (op1 : Operator) :: (rhs1 : Number) :: (op2 : Operator) :: t =>
      val rhs2 = parse(t).fold(err => return Left(err), ok => ok)
      Right(Arithmetic(Arithmetic(lhs1, op1, rhs1), op2, rhs2))

    // Invalid expressions
    case Nil => Left("syntax error: empty input")
    case _   => Left("syntax error: expressions are binary expressions or single numbers")
  }


def eval(expr: Expr): Either[String, Expr] =
  expr match {
    case num : Number => Right(num)
    case Arithmetic(Number(lhs), op, Number(rhs)) =>
      op match {
        case Plus  => Right(Number(lhs + rhs))
        case Minus => Right(Number(lhs - rhs))
        case Mult  => Right(Number(lhs * rhs))
      }
  }


parse(tokenize("40 + 2")).flatMap(eval)

def eval(expr: Expr): Either[String, Number] =
  expr match {
    case num : Number => Right(num)
    case Arithmetic(lhsExpr, op, rhsExpr) =>
      (eval(lhsExpr), eval(rhsExpr)) match {
        case (Left(err), _) => Left(err)
        case (_, Left(err)) => Left(err)

        case (Right(Number(lhs)), Right(Number(rhs))) =>
          op match {
            case Plus  => Right(Number(lhs + rhs))
            case Minus => Right(Number(lhs - rhs))
            case Mult  => Right(Number(lhs * rhs))
          }
      }
  }

parse(tokenize("10 * 4 + 2")).flatMap(eval)

parse(tokenize("10 a 4 + 2")).flatMap(eval)
