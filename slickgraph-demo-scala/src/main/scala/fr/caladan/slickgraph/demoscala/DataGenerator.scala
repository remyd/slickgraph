package fr.caladan.slickgraph.demoscala

/**
 * Generate a timeseries that follows the Poisson distribution
 */
object DataGenerator {

  protected def poisson(rate: Double): Double = {
    return -Math.log(1.0 - Math.random) / rate
  }

  /**
   * Generate a timeseries of a given size
   *
   * @param size Number of time points in the timeseries
   * @return Timeseries
   */
  def generateTimeseries(size: Integer): Seq[Double] = {
    var prev: Double = 0
    val data = for (i <- 0 until size) yield {
      prev += poisson(.01)
      prev
    }

    return data
  }
}
