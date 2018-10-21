# Same (sum) type, different meaning

Recently I wrote a small, Scheme like language for a presentation I gave at a
local conference. The point of the presentation was to talk about the
implementation as a whole, so the code had to be concise. As a result, I ended
up stumbling across a pattern in Scala that allowed me to share types all
through out the lexing to the evaluation stages of the interpreter. Doing so
meant that I didn't have to translate an AST into a runtime representation of
the program and its state, and instead was able to continue using the same tree
throughout the execution.

Let me start by giving an example of the pattern I'm used to running into and
using myself. Let's say we have a grammar like this one:

```ebnf
arith = num '+' | '-' num ;
op    = '+' | '-' ;
num   = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
```

It's a simple grammar but it's one that demostrates both the problem and the
proposed solution. Finding yourself in a situation where you have to model this
as tokens and/or an AST, you may opt for a solution like the following:

```scala
class Token(lexeme: String)
case class Operator(lexeme: String) extends Token(lexeme)
case class Number(lexeme: String) extends Token(lexeme)

class Expression
case class Arithmetic(lhs: Number, op: Operator, rhs: Number)
  extends Expression
```

At this point
