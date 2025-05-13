In
[*Detecting Argument Selection Defects*](https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/46317.pdf)
by Rice et. al., the authors argue that methods should have 5 of fewer
parameters (see section 7.1). APIs with larger than this number often lead to
parameter mismatch bugs (e.g, calling `create(firstName, lastName)` instead of
`create(lastName, firstName)`).

In *Effective Java* (Item 2), Bloch recommends "consider[ing] a builder
[pattern] when faced with many [constructor] parameters". You may consider
encapsulating your parameters into a single
[`@AutoValue`](https://github.com/google/auto/tree/master/value) object, which
is created using an
[AutoValue Builder](https://github.com/google/auto/blob/master/value/userguide/builders.md).
