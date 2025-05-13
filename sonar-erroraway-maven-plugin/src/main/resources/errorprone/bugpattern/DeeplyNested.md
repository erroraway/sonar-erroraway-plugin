Having an extremely long Java statement with many chained method calls can cause
compilation to fail with a `StackOverflowError` when the compiler tries to
recursively process it.

This is a common problem in generated code.

As an alternative to extremely long chained method calls, e.g. for builders,
consider something like the following for collections with hundreds or thousands
of entries:

```java
private static final ImmutableList<String> FEATURES = createFeatures();

private static final ImmutableList<String> createFeatures() {
  ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
  builder.add("foo");
  builder.add("bar");
  ...
  return builder.build();
}
```

over code like this:

```java
private static final ImmutableList<String> FEATURES =
  ImmutableList.<String>builder()
    .add("foo")
    .add("bar")
    ...
    .build();
```
