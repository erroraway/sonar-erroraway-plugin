Implicit conversions from `int` to `float` may lose precision when when calling
methods with overloads that accept both`float` and `double`.

For example, `Math.scalb` has overloads
[`Math.scalb(float, int)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html#scalb\(float,int\))
and
[`Math.scalb(double, int)`](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Math.html#scalb\(double,int\)).

When passing an `int` as the first argument, `Math.scalb(float, int)` will be
selected. If the result of `Math.scalb(float, int)` is then used as a `double`,
this may result in a loss of precision.

To avoid this, an explicit cast to `double` can be used to call the
`Match.scalb(double, int)` overload:

```java
int x = ...
int y = ...
double f = Math.scalb((double) x, 2);
```
