package com.sample.converter

import java.util.Date
import scala.util.parsing.json.JSONArray
import scala.util.parsing.json.JSONObject
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.json4s.jackson.Serialization
import org.json4s.string2JsonInput
import com.sample.base.Model

object JsonConverter {

  implicit val formats = DefaultFormats

  def convert(json: String): Model = {
    try {
      if (json == null || json.isEmpty()) return new Model
      val parsedJson = parse(json).values
      if (parsedJson.isInstanceOf[Map[_, _]])
        return new Model(asMap(parsedJson))
      new Model(Map("collection" -> parsedJson))
    } catch {
      case ex: Exception => {
        throw ex
      }
    }
  }

  def convert(gen: Model): String = {
    return (convert(gen.asMap()))
  }

  def convert(json: JSONObject): Model = {
    return convert(json.toString())
  }

  def convert(json: JSONArray): Model = {
    return convert(json.toString())
  }

  def convert(map: Map[String, Any]): String = {
    Serialization.write(map)
  }

  def convert(list: List[Model]): String = {
    Serialization.write(list.map { element => element.asMap() })
  }
}