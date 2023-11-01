`@AutoValue` is used to represent pure data classes. Mocking these should not be
necessary: prefer constructing them in the same way production code would.

To make the argument another way: the fact that `AutoValue` classes are not
`final` is an implementation detail of the way they're generated. They should be
regarded as logically final insofar as they must not be extended by
non-generated code. If they were final, they also would not be mockable.

Instead of mocking:

```java
@Test
public void test() {
  MyAutoValue myAutoValue = mock(MyAutoValue.class);
  when(myAutoValue.getFoo()).thenReturn("foo");
}
```

Prefer simply constructing an instance:

```java
@Test
public void test() {
  MyAutoValue myAutoValue = MyAutoValue.create("foo");
}
```

If your `AutoValue` has multiple required fields, and only one is relevant for a
test, consider using a [builder] or [`with`-style methods][wither] to create
test instances with just the fields you care about. Consider using

[builder]: https://github.com/google/auto/blob/master/value/userguide/builders.md
[wither]: https://github.com/google/auto/blob/master/value/userguide/builders-howto.md#withers

```java
private MyAutoValue.Builder myAutoValueBuilder() {
  return MyAutoValue.builder().bar(42).baz(false);
}

@Test
public void test() {
  MyAutoValue myAutoValue = myAutoValueBuilder.foo("foo").build();
}
```

or:

```java
private static final MyAutoValue MY_AUTO_VALUE =
    MyAutoValue.create(/* foo= */ "", /* bar= */ 42, /* baz= */ false);

@Test
public void test() {
  MyAutoValue myAutoValue = MY_AUTO_VALUE.withFoo("foo");
}
```
