package utils

object MathUtils {

  def median(s: Seq[Long]): Double = {
    val (lower, upper) = s.sortWith(_ < _).splitAt(s.size / 2)
    if (s.size % 2 == 0) (lower.last + upper.head) / 2.0 else upper.head
  }

  def avarage(s: Seq[Long]): Double = {
    s.sum / s.size
  }

  def avarage2(s: Seq[Long]): Double = {
    val s_max = s.max
    val s_min = s.min
    val s_filtered = s.filter(_ != s_max).filter(_ != s_min)
    avarage(s_filtered)
  }

}
