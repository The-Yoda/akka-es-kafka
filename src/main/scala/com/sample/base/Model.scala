package com.sample.base

import scala.language.dynamics
import scala.collection.mutable.ListBuffer

case class Model(mData: Map[String, Any] = Map()) extends Dynamic {

  val data = new scala.collection.mutable.LinkedHashMap[String, Any]()

  val setter = new Function[Any, Any] {
    def apply(value: Any): Any = {
      value match {
        case list: List[Any] => list.map { element => apply(element) }
        case map: Map[_, _]  => new Model(map.asInstanceOf[Map[String, Any]])
        case _               => value
      }
    }
  }

  def applyDynamic(methodName: String)(args: Any*): Any = {

    var (callType, fieldName) = methodName.toLowerCase().splitAt(3)
    callType match {
      case "set" => return set(fieldName, args.head)
      case "add" => return add(fieldName, args.head)
      case "get" => return get(fieldName)
      case "del" => return del(fieldName)
      case "has" => return has(fieldName)
      case "pop" => return pop(fieldName)
      case _     => throw new RuntimeException("Unknown Call Type")
    }
  }

  def get(fieldName: String): Any = {
    data.getOrElse(fieldName, None)
  }

  def del(fieldName: String) {
    data.remove(fieldName)
  }

  def add(fieldName: String, value: Any) {
    var list = data.get(fieldName)
    if (list.isEmpty) data.put(fieldName, ListBuffer(value))
    else list.get.asInstanceOf[ListBuffer[Any]].append(value)
  }

  def pop(fieldName: String): Any = {
    var value = data.get(fieldName)
    if (value.isEmpty) return None
    var list = value.get.asInstanceOf[ListBuffer[Any]]
    if (list.isEmpty) None else list.remove(list.size - 1)
  }

  def set(fieldName: String, value: Any) {
    data.put(fieldName, setter.apply(value))
  }

  def isEmpty = data.isEmpty

  def getClassFields(): List[String] = data.keys.toList

  def has(key: String): Boolean = data.contains(key.toLowerCase)

  mData foreach { case (key, value) => this.set(key.toLowerCase, value) }

  def asMap(): Map[String, Any] = {
    data.mapValues { element => convert(element) }.toMap
  }

  def convert(value: Any): Any = {
    value match {
      case model: Model    => model.asMap
      case list: List[Any] => list.map { element => convert(element) }
      case _               => value
    }
  }

  def +(model: Model): Model = {
    model.asMap foreach { case (key, value) => this.set(key.toLowerCase, value) }
    this
  }

  override def toString(): String = data.toString
}