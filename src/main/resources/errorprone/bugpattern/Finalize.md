Do not override `Object.finalize`.

Starting in JDK 18, the method is deprecated for removal, see
[JEP 421: Deprecate Finalization for Removal](https://openjdk.org/jeps/421).

The [Google Java Style Guide §6.4][style] states:

> It is extremely rare to override `Object.finalize`.
>
> Tip: Don't do it. If you absolutely must, first read and understand
> [Effective Java Item 8][ej3e-8], "Avoid finalizers and cleaners" very
> carefully, and then don't do it.

[ej3e-8]: https://books.google.com/books?id=BIpDDwAAQBAJ
[style]: https://google.github.io/styleguide/javaguide.html#s6.4-finalizers
