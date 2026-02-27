package com.example.json

import zio.json._
import zio.json.ast.Json
import zio.json.internal.Write
import scala.deriving.Mirror
import scala.compiletime._
import scala.quoted._

object AutoDiscriminator:

  inline def derived[A](using m: Mirror.Of[A]): JsonCodec[A] =
    inline m match
      case s: Mirror.SumOf[A] => deriveSumCodec[A](using s)
      case p: Mirror.ProductOf[A] => deriveProductCodec[A](using p)

  private inline def deriveSumCodec[A](using s: Mirror.SumOf[A]): JsonCodec[A] =
    checkUniqueDiscriminators[s.MirroredElemTypes]
    val encoders = summonAllEncoders[s.MirroredElemTypes]
    val decoders = summonAllDecoders[s.MirroredElemTypes]
    val discriminators = findDiscriminators[s.MirroredElemTypes]

    val encoder = new JsonEncoder[A]:
      def unsafeEncode(a: A, indent: Option[Int], out: Write): Unit =
        val ord = s.ordinal(a)
        encoders(ord).asInstanceOf[JsonEncoder[A]].unsafeEncode(a, indent, out)

    val decoder = new JsonDecoder[A]:
      def unsafeDecode(trace: List[JsonError], in: zio.json.internal.RetractReader): A =
        val obj = Json.Obj.decoder.unsafeDecode(trace, in)
        val fields = obj.fields.toMap
        val matchResult = discriminators.zipWithIndex.find { case (discList, idx) =>
          discList.nonEmpty && discList.forall { case (fieldName, fieldValue) =>
            fields.get(fieldName).exists {
              case Json.Str(v) if v == fieldValue => true
              case _ => false
            }
          }
        }
        matchResult match
          case Some((_, idx)) =>
            decoders(idx).asInstanceOf[JsonDecoder[A]].decodeJson(obj.toJson) match
              case Left(err) => throw new RuntimeException(err)
              case Right(v) => v
          case None => throw new RuntimeException("No matching discriminator found")

    JsonCodec(encoder, decoder)

  private inline def checkUniqueDiscriminators[T <: Tuple]: Unit = ${ checkUniqueDiscriminatorsImpl[T] }

  private def checkUniqueDiscriminatorsImpl[T <: Tuple: Type](using Quotes): Expr[Unit] =
    import quotes.reflect._

    def extractTupleTypes(tpe: TypeRepr): List[TypeRepr] =
      tpe.dealias match
        case AppliedType(tycon, args) if tycon.typeSymbol.name == "*:" =>
          args match
            case List(head, tail) => head :: extractTupleTypes(tail)
            case _ => Nil
        case _ => Nil

    def getDiscriminators(tpe: TypeRepr): List[(String, String)] =
      val symbol = tpe.typeSymbol
      val fields = symbol.caseFields
      fields.flatMap { field =>
        val fieldTpe = tpe.memberType(field)
        fieldTpe.dealias match
          case ConstantType(StringConstant(v)) => Some(field.name -> v)
          case _ => None
      }

    val tpe = TypeRepr.of[T]
    val types = extractTupleTypes(tpe)

    val typesWithoutDiscriminators = types.filter(t => getDiscriminators(t).isEmpty)
    if typesWithoutDiscriminators.nonEmpty then
      val names = typesWithoutDiscriminators.map(_.typeSymbol.name).mkString(", ")
      report.errorAndAbort(s"Types without any literal string discriminator field: $names")

    val discriminatorsWithTypes = types.map { t =>
      (getDiscriminators(t), t)
    }

    val duplicates = discriminatorsWithTypes.groupBy(_._1).filter(_._2.size > 1)

    if duplicates.nonEmpty then
      val msg = duplicates.map { case (discs, list) =>
        val typesStr = list.map(_._2.typeSymbol.name).mkString(", ")
        val discsStr = discs.map((k, v) => s"$k=$v").mkString(", ")
        s"Duplicate discriminator set ($discsStr) found in types: $typesStr"
      }.mkString("\n")
      report.errorAndAbort(msg)
    else
      '{ () }

  private inline def deriveProductCodec[A](using p: Mirror.ProductOf[A]): JsonCodec[A] =
    // Use ZIO JSON's own derivation for product types (case classes)
    DeriveJsonCodec.gen[A]

  private inline def summonAllEncoders[T <: Tuple]: List[JsonEncoder[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => summonInline[JsonEncoder[h]] :: summonAllEncoders[t]

  private inline def summonAllDecoders[T <: Tuple]: List[JsonDecoder[_]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => summonInline[JsonDecoder[h]] :: summonAllDecoders[t]

  private inline def findDiscriminators[T <: Tuple]: List[List[(String, String)]] =
    inline erasedValue[T] match
      case _: EmptyTuple => Nil
      case _: (h *: t) => findLiteralFields[h] :: findDiscriminators[t]

  transparent inline def findLiteralFields[A]: List[(String, String)] = ${ findLiteralFieldsImpl[A] }

  private def findLiteralFieldsImpl[A: Type](using Quotes): Expr[List[(String, String)]] =
    import quotes.reflect._
    val tpe = TypeRepr.of[A]
    val symbol = tpe.typeSymbol
    val fields = symbol.caseFields

    val refinedLiteralFields = fields.flatMap { field =>
      val fieldTpe = tpe.memberType(field)
      fieldTpe.dealias match {
        case ConstantType(StringConstant(v)) => Some(field.name -> v)
        case _ => None
      }
    }

    if refinedLiteralFields.isEmpty then
      report.errorAndAbort(s"No literal string field found in ${symbol.name}")

    val exprs = refinedLiteralFields.map { case (name, value) =>
      '{ (${Expr(name)}, ${Expr(value)}) }
    }
    Expr.ofList(exprs)
