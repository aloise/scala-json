package com.example.json

import zio.test._
import zio.test.Assertion._
import zio.json._
import com.example.json.Animal._

object AnimalSpec extends ZIOSpecDefault:
  def spec = suite("AnimalSpec")(
    test("Dog should be encoded and decoded correctly"):
      val dog = Dog(10)
      val json = (dog: Animal).toJson
      val decoded = json.fromJson[Animal]
      assert(decoded)(isRight(equalTo(dog: Animal)))
      && assert(dog.asInstanceOf[Dog].sound)(equalTo("woof"))
      && assert(dog.asInstanceOf[Dog].animalName)(equalTo("Dog"))
    ,
    test("Cat should be encoded and decoded correctly"):
      val cat = Cat(9)
      val json = (cat: Animal).toJson
      val decoded = json.fromJson[Animal]
      assert(decoded)(isRight(equalTo(cat: Animal)))
      && assert(cat.asInstanceOf[Cat].sound)(equalTo("meow"))
      && assert(cat.asInstanceOf[Cat].animalName)(equalTo("Cat"))
    ,
    test("Bird should be encoded and decoded correctly"):
      val bird = Bird(true)
      val json = (bird: Animal).toJson
      val decoded = json.fromJson[Animal]
      assert(decoded)(isRight(equalTo(bird: Animal)))
      && assert(bird.asInstanceOf[Bird].sound)(equalTo("chirp"))
      && assert(bird.asInstanceOf[Bird].animalName)(equalTo("Bird"))
    ,
    test("Animal sealed trait should handle Dog correctly"):
      val animal: Animal = Dog(10)
      val json = animal.toJson
      val decoded = json.fromJson[Animal]
      assert(decoded)(isRight(equalTo(animal)))
    ,
    test("Animal sealed trait should handle Cat correctly"):
      val animal: Animal = Cat(9)
      val json = animal.toJson
      println(json)
      val decoded = json.fromJson[Animal]
      assert(decoded)(isRight(equalTo(animal)))
    ,
    test("Animal sealed trait should handle Bird correctly"):
      val animal: Animal = Bird(true)
      val json = animal.toJson
      val decoded = json.fromJson[Animal]
      assert(decoded)(isRight(equalTo(animal)))
    ,
    test("JSON should have the correct flat structure with automatic discriminator"):
      val dog: Animal = Dog(10)
      val json = dog.toJson
      assert(json)(containsString(""""animalName":"Dog""""))
      && assert(json)(containsString(""""barkLoudness":10"""))
      && assert(json)(containsString(""""sound":"woof""""))
  )
