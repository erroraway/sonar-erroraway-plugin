Using `Collection`s (and other types which rely on equality) to contain elements
with undefined equality is error-prone. For example, `Collection` itself does
not have well-defined equality: the `List` and `Set` subinterfaces are not
necessarily comparable.

```java
ImmutableList<Collection<Integer>> collectionsOfIntegers =
    ImmutableList.of(ImmutableSet.of(1, 2), ImmutableSet.of(3, 4));

collectionsOfIntegers.contains(ImmutableSet.of(1, 2)); // true
collectionsOfIntegers.contains(ImmutableList.of(1, 2)); // false
```

```java
boolean containsTest(Collection<CharSequence>> charSequences) {
  // True if `charSequences` actually contains Strings, but otherwise not necessarily.
  return charSequences.contains("test");
}
```

In this case, an appropriate fix may be,

```java
boolean containsTest(Collection<CharSequence>> charSequences) {
  return charSequences.stream().anyMatch("test"::contentEquals);
}
```
