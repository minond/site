# Same sum type, different meaning

Recently I wrote a small Scheme like language for a presentation I gave at a
local conference, the point of the presentation was to talk about the whole
implementation, it had to be concise. As a result, I ended up stumbling across
a pattern in Scala that I'm very happy with since it allowed me to share types
all through out the lexing to the evaluation stages.

Let's say we have a grammar like this one:

```ebnf
arith = num '+' | '-' num ;
op    = '+' | '-' ;
num   = '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' ;
```

It's a simple grammar but it's one that demostrates both the problem and the
proposed solution.
