Nothing makes you miss ADTs more than using languages that don't have them.
Scala supports ADTs using sealed traits, allowing the user to define child
classes which act as the type constructors, and since the parent class is
sealed, the compiler is able to check for exhaustiveness when using pattern
matching.

In addition to all of this, Scala's support for OOP makes for some really
interesting uses of ADTs with inheritance. For me, the best example to show
this off is a small compiler with shared data structures for the lexing,
parsing, and evaluation steps.

Let's say we have a language that supports only arithmetic expressions:

```ebnf
expr  = num | arith ;
arith = expr op expr ;
op    = '+' | '-' | '*' ;
num   = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
```

It's a simple grammar but it's one that demonstrates the usefulness of ADTs
plus OOP. Finding yourself in a situation where you have to model this as
tokens, an AST, or runtime data, you may opt for a solution like the following:

```scala
sealed trait Token

sealed trait Operator extends Token
case object Plus extends Operator
case object Minus extends Operator
case object Mult extends Operator

sealed trait Expr
case class Number(num: Double) extends Token with Expr
case class Arithmetic(lhs: Expr, op: Operator, rhs: Expr) extends Expr
```

Notice the top-level `Token` type, with it's two children `Operator` and
`Number`, and `Expr` with `Arithmetic` and `Number` as well. `Operator` and
`Number` are special, since they are both separate types and of type `Token` as
well -- this will be useful later on when we start treating certain tokens as
valid expressions. Let's see this hierarchy in a more visual form:

![Class diagram](/posts/1531094894-adt-type-meaning.svg)

With the diagram it's easier to see that when we are working with a `Token`
type, we have to know how to handle both `Number`, a concrete type, and
`Operator`, a sum type.

Another way of thinking about this is using sets:

```
Token        = {Number, Operator}
Expr         = {Number, Arithmetic}
Operator     = {Plus, Minus, Mult}

where Number = Expr ∩ Token
```

With this, we are able to define three functions which act as the full
interpreter for our language:

- `tokenize`, which is defined as `String => list of Token`,
- `parse`, which is defined as `list of Token => Expr`, and
- `eval`, which is defined as `Expr => Expr`.

This leaves us with a flow of data that goes from a strings, to tokens, to
finally expressions.


## Implementation

We can bring this full circle by building our interpreter. Starting with
`tokenize` and a helper:

```scala
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
```

And to test things are working as expected:

```text
scala> tokenize("123").toList
res32: List[Token] = List(Number(123))

scala> tokenize("1+2").toList
res33: List[Token] = List(Number(1), Operator(+), Number(2))
```

Now we can move on to `parse`, which we previously defined as `list of Token =>
Expr`. This is an incomplete implementation since it doesn't parse arithmetic
expressions yet, but it is complete in the sense that it handles every possible
input. A list of tokens could only be made up of `Operator`'s and `Number`'s,
both of which are checked for in the code below.

```scala
def parse(tokens: List[Token]): Either[String, Expr] =
  tokens match {
    case Nil                   => Left("invalid: empty input")
    case (_: Operator) :: _    => Left("invalid: cannot start expression with operator")
    case (token : Number) :: _ => Right(token)
  }
```

If we wanted to test out the exhaustive checks provided by the compiler, we
could comment out any of those cases and the results would be a warning (or
error) such as:

```text
<pastie>:18: warning: match may not be exhaustive.
It would fail on the following input: List(Number(_))
  tokens match {
  ^
```

For an implementation that is able to parse arithmetic expressions, we could do
something like:

```scala
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
```

And we overload `parse` to take an `Iterator`, making it easier to work with
the rest of the code:

```scala
def parse(tokens: Iterator[Token]): Either[String, Expr] = parse(tokens.toList)
```

At this point we're handling all possible inputs and output in the parsing
phase. We can now follow similar patterns for implementing the `eval` function,
which converts expressions into simpler representations. For the first take, we
implementing a version which only handles numbers and arithmetic expressions
without nested expressions:

```scala
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
```

Let's try it out to make sure things work:

```text
scala> parse(tokenize("40 + 2")).flatMap(eval)
res27: scala.util.Either[String,Expr] = Right(Number(42.0))
```

And now for the second take where we handle nested expressions:

```scala
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
```

The second implementation is needed since `Arithmetic` is able to hold `Expr`
values on the left or right hand side. And since these values have yet to be
evaluated, we do so before completing the evaluation of the first expression.
For quick check that it works:

```text
scala> parse(tokenize("10 * 4 + 2")).flatMap(eval)
res3: scala.util.Either[String,Number] = Right(Number(42.0))
```

## Discussion


