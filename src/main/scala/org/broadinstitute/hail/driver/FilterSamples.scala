package org.broadinstitute.hail.driver

import org.broadinstitute.hail.Utils._
import org.broadinstitute.hail.methods._
import org.broadinstitute.hail.variant._
import org.broadinstitute.hail.annotations._
import org.kohsuke.args4j.{Option => Args4jOption}

import scala.io.Source

object FilterSamples extends Command {

  class Options extends BaseOptions {
    @Args4jOption(required = false, name = "--keep", usage = "Keep only listed samples in current dataset")
    var keep: Boolean = false

    @Args4jOption(required = false, name = "--remove", usage = "Remove listed samples from current dataset")
    var remove: Boolean = false

    @Args4jOption(required = true, name = "-c", aliases = Array("--condition"),
      usage = "Filter condition: expression or .sample_list file (one sample name per line)")
    var condition: String = _
  }

  def newOptions = new Options

  def name = "filtersamples"

  def description = "Filter samples in current dataset"

  def run(state: State, options: Options): State = {
    val vds = state.vds
    val sas: AnnotationSignatures = state.vds.metadata.sampleAnnotationSignatures

    if (!options.keep && !options.remove)
      fatal(name + ": one of `--keep' or `--remove' required")

    val indexOfSample: Map[String, Int] = state.vds.sampleIds.zipWithIndex.toMap

    val p = options.condition match {
      case f if f.endsWith(".sample_list") =>
        val samples = Source.fromInputStream(hadoopOpen(f, state.hadoopConf))
          .getLines()
          .filter(line => !line.isEmpty)
          .map(indexOfSample)
          .toSet
        (s: Int, sa: AnnotationData) => samples.contains(s)
      case c: String =>
        try {
          val cf = new FilterSampleCondition(c, sas)
          cf.typeCheck()

          val sampleIdsBc = state.sc.broadcast(state.vds.sampleIds)
          (s: Int, sa: AnnotationData) => cf(Sample(sampleIdsBc.value(s)), state.vds.metadata.sampleAnnotations(s))
        } catch {
          case e: scala.tools.reflect.ToolBoxError =>
            /* e.message looks like:
               reflective compilation has failed:

               ';' expected but '.' found. */
            fatal("parse error in condition: " + e.message.split("\n").last)
        }
    }

    val newVDS = vds.filterSamples(if (options.keep)
      p
    else
      (s: Int, sa: AnnotationData) => !p(s, sa))

    state.copy(vds = newVDS)
  }
}
