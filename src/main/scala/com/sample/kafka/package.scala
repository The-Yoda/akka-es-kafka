package com.sample

import com.sample.base.Model
import scala.util.Random
import java.util.Date
package object kafka2es {

  implicit class CustomAny(val self: Any) extends AnyVal {
    def as[T] = self.asInstanceOf[T]
  }

  def model(elem: Any): Model = elem.asInstanceOf[Model]

  implicit def str(elem: Any): String = elem.toString

  implicit def bool(elem: Any): Boolean = elem.asInstanceOf[Boolean]

  implicit def int(elem: Any): Int = elem.asInstanceOf[Int]

  implicit def long(elem: Any): Long = elem.asInstanceOf[Long]

  implicit def double(elem: Any): Double = elem.asInstanceOf[Double]

  implicit def asMap(elem: Any): Map[String, Any] = elem.asInstanceOf[Map[String, Any]]

  def as[T](elem: Any): T = elem.asInstanceOf[T]

  /**
   * Simulate ternary operator
   */
  case class Ternary(b: Boolean) {
    def ?[X](first: => X) = new {
      def |(second: => X) = if (b) first else second
    }
  }

  implicit def ternaryOp(b: Boolean) = Ternary(b)

  def isEmpty(data: Any): Boolean = {
    if (data == None) return true
    if (data.isInstanceOf[Model]) return model(data).isEmpty
    if (data.isInstanceOf[List[Any]]) return as[List[Any]](data).isEmpty
    if (data.isInstanceOf[String]) return str(data).isEmpty || str(data).equals("None")
    if (data.isInstanceOf[Map[_, Any]]) return as[Map[_, Any]](data).isEmpty
    return false
  }

  def getId = Random.alphanumeric.take(15).mkString

  def getUniqNumId = str(Math.abs(Random.nextLong))

  def getUniqId = Random.alphanumeric.take(15).mkString

  def getTime = (new Date).getTime
}