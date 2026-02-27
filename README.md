# scala-json

Compile-time automatic JSON discriminator derivation for Scala 3 enums using literal string types and [ZIO JSON](https://github.com/zio/zio-json).

Instead of wrapping variants in a tagged object like `{"Dog": {...}}`, this approach uses literal string type fields (e.g. `animalName: "Dog" = "Dog"`) as flat discriminators embedded directly in the JSON.

## How it works

Define an enum with literal string type fields as discriminators:

```scala
enum Animal:
  case Dog(barkLoudness: Int, animalName: "Dog" = "Dog", breed: String, sound: String = "woof")
  case Cat(livesLeft: Int, animalName: "Cat" = "Cat", sound: String = "meow")
  case Bird(canFly: Boolean, animalName: "Bird" = "Bird", birdKind: "Bird" = "Bird", sound: String = "chirp")
  case BirdParrot(animalName: "Bird" = "Bird", birdKind: "Parrot" = "Parrot", sound: String = "pi-pi-li", canTalk: Boolean = true)
  case BirdKiwi(animalName: "Bird" = "Bird", birdKind: "Kiwi" = "Kiwi", sound: String = "chirp2", eggSize: String = "huge")
```

Derive the codec with a single line:

```scala
object Animal:
  implicit val codec: JsonCodec[Animal] = AutoDiscriminator.derived[Animal]
```

This produces flat JSON with discriminator fields inline:

```json
{"barkLoudness":10,"animalName":"Dog","breed":"Labrador","sound":"woof"}
{"livesLeft":9,"animalName":"Cat","sound":"meow"}
{"canFly":true,"animalName":"Bird","birdKind":"Bird","sound":"chirp"}
{"animalName":"Bird","birdKind":"Parrot","sound":"pi-pi-li","canTalk":true}
```

## Multiple discriminators

Variants can share a discriminator value as long as they are uniquely identified by the full set of their literal string fields. For example, `Bird`, `BirdParrot`, and `BirdKiwi` all share `animalName: "Bird"` but are distinguished by `birdKind`.

If two variants have identical discriminator sets, compilation fails:

```
Duplicate discriminator set (animalName=Bird, birdKind=Kiwi) found in types: BirdParrot, BirdKiwi
```

## Compile-time guarantees

- Every variant must have at least one literal string type field
- The combination of literal string fields must be unique across all variants
- Violations are reported as compile-time errors

## Running

```bash
sbt run        # runs the example main
sbt test       # runs the test suite
```

## Dependencies

- Scala 3.8.2
- ZIO JSON 0.7.3
