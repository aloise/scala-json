package com.example.json

import com.example.json.Vehicle.Car
import zio.json.*

enum Animal:
  case Dog(barkLoudness: Int, animalName: "Dog" = "Dog", breed: String, sound: String = "woof")
  case Cat(livesLeft: Int, animalName: "Cat" = "Cat", sound: String = "meow")
  case Bird(canFly: Boolean, animalName: "Bird" = "Bird", birdKind: "Bird" = "Bird", sound: String = "chirp")
  case BirdParrot(animalName: "Bird" = "Bird", birdKind: "Parrot" = "Parrot", sound: String = "pi-pi-li", canTalk: Boolean = true)
  case BirdKiwi(animalName: "Bird" = "Bird", birdKind: "Kiwi" = "Kiwi", sound: String = "chirp2", eggSize: String = "huge")

object Animal:
  // TODO - this can be auto-generated
  implicit val dogCodec: JsonCodec[Dog] = DeriveJsonCodec.gen
  implicit val catCodec: JsonCodec[Cat] = DeriveJsonCodec.gen
  implicit val birdCodec: JsonCodec[Bird] = DeriveJsonCodec.gen
  implicit val birdParrotCodec: JsonCodec[BirdParrot] = DeriveJsonCodec.gen
  implicit val birdKiwiCodec: JsonCodec[BirdKiwi] = DeriveJsonCodec.gen

  // this is where automatic discriminator is computed compile time
  implicit val codec: JsonCodec[Animal] = AutoDiscriminator.derived[Animal]

enum Vehicle:
  case Car(kind: "Car", make: String, model: String, year: Int)
  case Truck(kind: "Truck", make: String, model: String, year: Int)
  case Motorcycle(make: String, model: String, year: Int, harley: "Harley" = "Harley")

object Vehicle:
  implicit val carCodec: JsonCodec[Car] = DeriveJsonCodec.gen
  implicit val truckCodec: JsonCodec[Truck] = DeriveJsonCodec.gen
  implicit val motorcycleCodec: JsonCodec[Motorcycle] = DeriveJsonCodec.gen

  implicit val codec: JsonCodec[Vehicle] = AutoDiscriminator.derived[Vehicle]


@main def animalMain(): Unit =
  import Animal._

  val animals: List[Animal] = List(
    Dog(10, breed = "Labrador"),
    Cat(9),
    Bird(true),
    BirdParrot(),
    BirdKiwi()
  )

  animals.foreach { animal =>
    val json = animal.toJson
    val decoded = json.fromJson[Animal]
    println(s"Original: $animal")
    println(s"JSON:     $json")
    println(s"Decoded:  $decoded")
    println()
  }
