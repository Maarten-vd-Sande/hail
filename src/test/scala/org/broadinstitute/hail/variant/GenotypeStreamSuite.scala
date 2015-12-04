package org.broadinstitute.hail.variant

import org.scalacheck.{Gen, Properties}
import org.scalacheck.Prop._
import org.scalatest.testng.TestNGSuite
import org.testng.annotations.Test

object GenotypeStreamSuite {
  val v = Variant("chr1", 1234, "A", "T")
  val b = new GenotypeStreamBuilder(v)
  object Spec extends Properties("GenotypeStream") {
    property("iterateBuild") = forAll(Gen.buildableOf[Array[Genotype], Genotype](Genotype.gen(2))) { a: Array[Genotype] =>
      b.clear()
      b ++= a
      val gs = b.result()
      val a2 = gs.toArray
      a.sameElements(a2)
    }
  }
}

class GenotypeStreamSuite extends TestNGSuite {
  import GenotypeStreamSuite._

  @Test def testGenotypeStream() {
    Spec.check
  }
}
