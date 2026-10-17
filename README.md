# Lint

Simple library to parse and validate yml configuration files

## Example

Lets imagine this simple item definition

```yaml
example_item:
  name: "<red>Example Item"
  material: PAPPER
  value: "idk"
  components:
    item_model: "example_model"
```

We can parse and validation this file:

```java
// Predefined set of valid materials
Set<String> MATERIALS = Set.of("PAPER", "STONE", "ORANGE_WOOL", "ORANGE_DYE");

Lint lint = Lint.builder()
        .ansiRenderer()
        .standardExceptionTranslation()
        .build();

Result result = lint.load(Path.of("/example_config.yml"), (id, ctx, cursor) -> {
    String material = cursor.oneOf("material", MATERIALS);
    Float decimal = cursor.getFloat("value");
    
    cursor.section("components", c -> {
        Optional<String> itemModel = c.optString("item_model");
    });
    
    if (material == null || decimal == null) return;
    
    // We can use the values to create item or something
});

if (result.hasErrors()) {
    System.out.println("example_config has errors");
}
```

This code reports following issues:
```
warning[lint:UNKNOWN]: unknown material `PAPPER`
 --> ./config/example_config.yml:3:13
  |
3 |   material: PAPPER
  |             ^^^^^^ no such material
  |
  = help: did you mean `PAPER`?

error[lint:WRONG_VALUE_TYPE]: expected a float number for `value`, found `idk`
 --> ./config/example_config.yml:4:10
  |
4 |   value: "idk"
  |          ^^^^^ not a float number

warning[lint:UNKNOWN]: unknown key `name`
 --> ./config/example_config.yml:2:3
  |
2 |   name: "<red>Example Item"
  |   ^^^^ not a recognised option
```

## Features

- Primitive types (string, int, float, double, long, bool)
- Sections
- Lists
- Optional values
- Change severity of errors using config file
- Changing renderers

## Why

I really hate how minecraft plugins handle configs.
If you misspell or forget a value, nothing tells you whats wrong.
You need to look into plugin documentation, which may be outdated or completely missing.

This library tries to minimize that and make users life easier.

## How to use

Use [jitpack](https://jitpack.io/#AdamBurdik/lint) to include it in your project

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.AdamBurdik:Lint:<commit>")
}
```

## Credits
- Lint uses [snakeyaml](https://github.com/snakeyaml/snakeyaml) for the actual yml parsing
- Error messages are inspired by rust compiler ones

## Licence
[MIT](./LICENCE)