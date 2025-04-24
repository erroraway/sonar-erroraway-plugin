A setter invoked with a value from the corresponding getter is often a mistake,
for example:

```java
if (from.hasFrobnicator()) {
  to.setFrobnicator(to.getFrobnicator());
}
```

This is easy to accidentally write, but is clearly meant to be,

```java
if (from.hasFrobnicator()) {
  to.setFrobnicator(from.getFrobnicator());
}
```

The Java proto API is tolerant enough that the former code will compile and
execute fine, but it will set `frobnicator` to the default value for that field.

This pattern is occasionally used to ensure that a field is always present, even
if it takes the default value, for example,

```java
// ensure "always_present" is present
builder.setAlwaysPresent(builder.getAlwaysPresent());
```

This is not a no-op, but we'd encourage being more explicit about the condition,

```java
if (!builder.hasAlwaysPresent()) {
  builder.setAlwaysPresent(false);
}
```

Or if `builder` is otherwise untouched, `builder.setAlwaysPresent(false)`.
