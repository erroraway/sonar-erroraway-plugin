Casts have higher precedence than binary expressions, so `(int) 0.5f * 100` is
equivalent to `((int) 0.5f) * 100` = `0 * 100` = `0`, not `(int) (0.5f * 100)` =
`50`.

To avoid this common source of error, add explicit parentheses to make the
precedence explicit. For example, instead of this:

```java
long SIZE_IN_GB = (long) 1.5 * 1024 * 1024 * 1024; // this is 1GB, not 1.5GB!

// this is 0, not a long in the range [0, 1000000000]
long rand = (long) new Random().nextDouble() * 1000000000
```

Prefer:

```java
long SIZE_IN_GB = (long) (1.5 * 1024 * 1024 * 1024);

long rand = (long) (new Random().nextDouble() * 1000000000);
```
