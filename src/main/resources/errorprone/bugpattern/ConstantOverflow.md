Compile-time constant expressions that overflow are a potential source of bugs.

Literals without an explicit `L` suffix have type `int`, so the following
multiplication expression is evaluated as an integer before being widened to
`long`. The value is greater than `Integer.MAX_VALUE`, so it wraps around to
`-1857093632`.

```java
static final long NANOS_PER_DAY = 24  * 60 * 60 * 1000 * 1000 * 1000;
```

The intent was probably for the multiplication expression to be evaluated as a
`long` instead of an `int`.

```java
static final long NANOS_PER_DAY = 24L * 60 * 60 * 1000 * 1000 * 1000;
```

If you find yourself doing this kind of time-based math, consider using an API
that provides a safer, more readable and strongly-typed solution like the
[`java.time.Duration`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/time/Duration.html)
API.
