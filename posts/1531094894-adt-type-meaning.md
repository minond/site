There are instances where the semantics of distinct types overlap. Through ADTs
and OOP, it is possible to represent this using different sets of types, while
still being able to work with the set unions and intersections in a way that is
type safe.


## Introduction

[ADTs (algebraic data
types)](https://en.wikipedia.org/wiki/Algebraic_data_type) is a feature that
many functional programming languages have. An ADT is an abstract type made up
of concrete types. The concrete types act as the constructors for the top-level
type, meaning they are needed in order to create a value of the top-level type.
Many of these same languages support a feature called [pattern
matching](https://en.wikipedia.org/wiki/Pattern_matching), which is a form of
checking and matching the pattern of a given value.

Languages like Scala, Standard ML, and OCaml are able to check for the
exhaustiveness of a pattern match, meaning they are able to check that the
patterns in a pattern match expression account for all possible values of the
input. For example, in Scala there exists an `Option[T]` [sum
type](https://en.wikipedia.org/wiki/Tagged_union) which is made up of two
types: `None` and `Some[T]`. When matching a value of type `Option[T]`, the
Scala compiler is able to check for the existence of  patterns matching both of
the possible values (and any additional matching ensuring `T` in `Some` is
accounted for as well.)

Exhaustive checking acts as a safety net that ensures that all possible values
are handled, and there are no runtime errors due to unhandled values.

Scala's support for OOP makes for some really interesting uses of distinct ADTs
with inheritance. A good example to show this off is a small interpreter with
shared data structures for the lexing, parsing, and evaluation steps.

Let's say we have a language that supports only arithmetic expressions:

```ebnf
expr  ::= num | arith ;
arith ::= expr op expr ;
op    ::= '+' | '-' | '*' ;
num   ::= '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
```

It's a simple grammar but it's one that demonstrates the usefulness of ADTs
plus OOP.


## Data Structures

Finding yourself in a situation where you have to model this as
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
type, we have to know how to handle both `Number`, a type constructor, and
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
finally expressions. Since `Number` is both a `Token` and an `Expr`, we will
see it in use in all three functions, all in a type safe way which ensures any
pattern matching is exhaustive.


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

Here is the first use of `Number`, a valid `Token`. To test things are working
as expected:

```text
scala> tokenize("123").toList
res32: List[Token] = List(Number(123))

scala> tokenize("1+2").toList
res33: List[Token] = List(Number(1), Operator(+), Number(2))
```

Now we can move on to `parse`, which we previously defined as `list of Token =>
Expr`. Below is an incomplete implementation since it doesn't parse arithmetic
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

Note that since we're working with lists we have to pattern match the list
itself before we can get to the values it holds. You can read the matching
above as follows: first matching `Nil`, which represents an empty `List`, then
match a list with an `Operator` as the first element and anything else
(including an empty list) afterwards, finally do the same but for lists with a
`Number` as the first element in the list.

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
def parse(tokens: Iterator[Token]): Either[String, Expr] =
  parse(tokens.toList)

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

_We overload `parse` to take an `Iterator`, making it easier to work with the
rest of the code._

There are more matches in this expression but they operate on the list of
`Token`s in the same way that the previous example does -- all we are doing is
peeking at the values at the start of the list and ignoring what ever values
may come afterwards.

At this point we're handling all possible inputs and outputs in the parsing
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

Since the only constructors for `Expr` are `Number` and `Arithmetic`, this is
an exhaustive match of all possible inputs. Let's try it out to make sure
things work:

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
evaluated, we do so before completing the evaluation of the first expression:

```text
scala> parse(tokenize("10 * 4 + 2")).flatMap(eval)
res3: scala.util.Either[String,Number] = Right(Number(42.0))
```

Notice the continuous use of the `Number` type constructor. Because `Number` is
both a `Token` and an `Expr`, we are able to use it throughout the whole
process of evaluation, and all in a way where the compiler is there to help us.


## Conclusion

Class hierarchies and ADTs are nothing new. We could represent the very same
hierarchy in another language, and keep most of our signatures the same as
well. The same could be said about ADTs. What's distinguishing about this is
the combination of the two, allowing us to able to represent the flow of data
as data structures and do so in a way where the type system has the ability to
ensure we're handling all of the possible cases.

In addition, we are able to reuse types where it makes sense to do so. The
`Number` type is a valid return value for a tokenizer, a parser, and an
evaluator, and we can convey this by making it both a `Token` and an `Expr` at
the same time. If our language were bigger, perhaps the same could be said
about other scalar types.

And like with anything else, reusability can be taken too far. Once the use or
meaning of a type (or value) starts to change an increase in scope, it makes
less sense to continue using the same type. For example, if our language had
typing information that we needed to pass around, the data structures used in
the parsing phase will not be enough during the type checking phase. And
extending the types so that they could be used in the type checker would expand
the scope too much, leaving you with the information that you need but
completely stripping the constructors of their ergonomics.

With that in mind, there are many instances where types and their semantics
overlap, and there is a need to represent the distinct sets, their union, and
their intersections. When this is the case, OOP and ADTs are a great mix.

<link rel="stylesheet" href="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.15.8/build/styles/ascetic.min.css">
<script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.15.8/build/highlight.min.js"></script>
<script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.15.8/build/languages/scala.min.js"></script>
<script src="//cdn.jsdelivr.net/gh/highlightjs/cdn-release@9.15.8/build/languages/ebnf.min.js"></script>
