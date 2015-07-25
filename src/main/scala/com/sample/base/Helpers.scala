package com.sample.base

import scala.util.Random
import java.util.Date

abstract class Helpers {

  def str(elem: Any): String = elem.toString

  def bool(elem: Any): Boolean = elem.asInstanceOf[Boolean]

  def int(elem: Any): Int = elem.asInstanceOf[Int]

  def long(elem: Any): Long = elem.asInstanceOf[Long]

  def double(elem: Any): Double = elem.asInstanceOf[Double]

  def as[T](elem: Any): T = elem.asInstanceOf[T]
  
  def gen(elem: Any) = elem.asInstanceOf[Model]

  def isEmpty(data: Any): Boolean = {
    if (data == None || data == "None") return true
    if (data.isInstanceOf[List[Any]]) return as[List[Any]](data).isEmpty
    if (data.isInstanceOf[String]) return str(data).isEmpty || str(data).equals("None")
    if (data.isInstanceOf[Map[_, Any]]) return as[Map[_, Any]](data).isEmpty
    return false
  }

  def getTime = (new Date).getTime

  def asMap(elem: Any): Map[String, Any] = elem.asInstanceOf[Map[String, Any]]

  def getUniqNumId: String = {
    str(Math.abs(Random.nextLong()))
  }

  def getId: String = {
    Random.alphanumeric.take(15).mkString
  }

  case class Bool(b: Boolean) {
    def ?[X](t: => X) = new {
      def |(f: => X) = if (b) t else f
    }
  }

  implicit def BooleanBool(b: Boolean) = Bool(b)
}