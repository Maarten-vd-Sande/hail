package org.broadinstitute.hail.variant

import scala.collection.mutable.ArrayBuffer
import scala.language.implicitConversions

import org.apache.spark.sql.Row

object RichRow {
  implicit def fromRow(r: Row): RichRow = new RichRow(r)
}

class RichRow(r: Row) {

  import RichRow._

  def getIntOption(i: Int): Option[Int] =
    if (r.isNullAt(i))
      None
    else
      Some(r.getInt(i))

  def toAltAllele: AltAllele = {
    AltAllele(r.getString(0),
      r.getString(1))
  }

  def getVariant(i: Int): Variant = {
    val ir = r.getAs[Row](i)
    Variant(ir.getString(0),
      ir.getInt(1),
      ir.getString(2),
      ir.getAs[ArrayBuffer[Row]](3).map(_.toAltAllele))
  }

  def getGenotype(i: Int): Genotype = throw new UnsupportedOperationException

  def getGenotypeStream(i: Int): GenotypeStream = {
    val ir = r.getAs[Row](i)
    GenotypeStream(ir.getVariant(0),
      if (ir.isNullAt(1)) None else Some(ir.getInt(1)),
      ir.getAs[Array[Byte]](2))
  }
}
