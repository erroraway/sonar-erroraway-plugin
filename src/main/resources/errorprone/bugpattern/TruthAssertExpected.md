Arguments to a fluent [Truth][truth] assertion appear to be reversed based on
the argument names.

```java
  int expected = 1;
  assertThat(expected).isEqualTo(codeUnderTest());
```

This is problematic as the quality of Truth's error message depends on the
argument order. If `codeUnderTest()` returns `2`, this code will output:

```
expected: 2
but was : 1
```

Which will likely make debugging the problem harder. Truth assertions should
follow the opposite order to JUnit assertions. Compare:

```java
  assertThat(actual).isEqualTo(expected);
  assertEquals(expected, actual);
```

See https://truth.dev/faq#order for more details.

[truth]: https://truth.dev
