package ml.combust.bundle.dsl

import ml.combust.bundle.HasBundleRegistry

/** This trait provides easy access to reading/writing attributes
  * to objects that contain an [[AttributeList]].
  *
  * @tparam T class type that contains the attribute list
  */
trait HasAttributeList[T] {
  /** The attribute list */
  def attributes: Option[AttributeList]

  /** Get an attribute.
    *
    * Throws an error if it does not exist.
    *
    * @param name name of the attribute
    * @return the attribute
    */
  def attr(name: String): Attribute = attributes.get(name)

  /** Get an attribute as an option.
    *
    * @param name name of the attribute
    * @return Some(attribute) or None if it is not in the list
    */
  def getAttr(name: String): Option[Attribute] = attributes.get.get(name)

  /** Get the [[Value]] of an attribute.
    *
    * Throws an error if that attribute does not exist.
    *
    * @param name name of the attribute
    * @return [[Value]] of the attribute
    */
  def value(name: String): Value = attr(name).value

  /** Get the [[Value]] of an attribute as an Option.
    *
    * @param name name of the attribute
    * @return [[Value]] of the attribute as an Option[Value], None if it does not exist
    */
  def getValue(name: String): Option[Value] = getAttr(name).map(_.value)

  /** Add an attribute to the list.
    *
    * @param name name of the attribute
    * @param value value of the attribute
    * @return a copy of T with the attribute added
    */
  def withAttr(name: String, value: Value): T = attributes match {
    case Some(l) => replaceAttrList(Some(l.withAttr(name, Attribute(value))))
    case None => replaceAttrList(Some(AttributeList(name -> Attribute(value))))
  }

  /** Add an optional attribute to the list.
    *
    * @param name name of the attribute
    * @param value optional value
    * @return a copy of T with the attribute optionally added
    */
  def withAttr(name: String, value: Option[Value]): T = (attributes, value) match {
    case (Some(attrs), Some(v)) => replaceAttrList(Some(attrs.withAttr(name, Attribute(v))))
    case (Some(attrs), None) => replaceAttrList(Some(attrs))
    case (None, Some(v)) => replaceAttrList(Some(AttributeList(name -> Attribute(v))))
    case (None, None) => replaceAttrList(None)
  }

  /** Add a list of attributes to [[attributes]].
    *
    * Adds all attributes in the list argument to [[attributes]].
    *
    * @param list list of attributes to add
    * @return copy of T with all attributes added to [[attributes]]
    */
  def withAttrList(list: AttributeList): T = attributes match {
    case Some(l) => replaceAttrList(Some(l.withAttrList(list)))
    case None => replaceAttrList(Some(list))
  }

  /** Replace the [[attributes]] with another list.
    *
    * @param list attributes to use to replace
    * @return copy of T with [[attributes]] replaced by list arg
    */
  def replaceAttrList(list: Option[AttributeList]): T
}

/** Companion class for construction and conversion of [[AttributeList]] objects.
  */
object AttributeList {
  /** Construct an [[AttributeList]] from a list of [[Attribute]].
    *
    * @param attrs list of [[Attribute]]
    * @return [[AttributeList]] with all attrs passed in
    */
  def apply(attrs: Seq[(String, Attribute)]): AttributeList = {
    AttributeList(attrs.toMap)
  }

  /** Construct an [[AttributeList]] from a list of [[Attribute]].
    *
    * @param attr1 first [[Attribute]] in list
    * @param attrs tail of [[Attribute]] list
    * @return [[AttributeList]] with attr1 and attrs in the list
    */
  def apply(attr1: (String, Attribute), attrs: (String, Attribute) *): AttributeList = {
    AttributeList(Seq(attr1) ++ attrs)
  }

  /** Create an attribute list from a bundle attribute list.
    *
    * @param list bundle attribute list
    * @param hr bundle registry for custom types
    * @return dsl attribute list
    */
  def fromBundle(list: ml.bundle.AttributeList.AttributeList)
                (implicit hr: HasBundleRegistry): AttributeList = {
    val attrs = list.attributes.map {
      case (key, attr) => (key, Attribute.fromBundle(attr))
    }
    AttributeList(attrs)
  }

  /** Construct an optional [[AttributeList]].
    *
    * Returns None if there are no attributes in the list.
    *
    * @param attrs [[Attribute]] in the list
    * @return None if {{{attrs.isEmpty}}} otherwise Some(attributeList)
    */
  def option(attrs: Map[String, Attribute]): Option[AttributeList] = {
    if(attrs.nonEmpty) { Some(apply(attrs)) } else { None }
  }
}

/** Class that holds a list of [[Attribute]] objects.
  *
  * Can only have one attribute with a given name at a time.
  *
  * @param lookup map of attribute name to [[Attribute]] object
  */
case class  AttributeList(lookup: Map[String, Attribute]) {
  /* make sure the list is not empty */
  require(lookup.nonEmpty, "attribute list cannot be empty")

  /** Convert to bundle attribute list.
    *
    * @param hr bundle registry for custom types
    * @return bundle attribute list
    */
  def asBundle(implicit hr: HasBundleRegistry): ml.bundle.AttributeList.AttributeList = {
    val attrs = lookup.map {
      case (key, attr) => (key, attr.asBundle)
    }
    ml.bundle.AttributeList.AttributeList(attrs)
  }

  /** Get an attribute.
    *
    * Alias for [[attr]]
    *
    * @param name name of the attribute
    * @return the attribute
    */
  def apply(name: String): Attribute = attr(name)

  /** Get an [[Attribute]] from the list.
    *
    * Throws an error if the attribute doesn't exist.
    *
    * @param name name of the attribute
    * @return the attribute
    */
  def attr(name: String): Attribute = lookup(name)

  /** Get an option of an [[Attribute]] from the list.
    *
    * @param name name of the attribute
    * @return optional [[Attribute]] from list
    */
  def get(name: String): Option[Attribute] = lookup.get(name)

  /** Add an attribute to the list.
    *
    * @param name name of attribute
    * @param attribute attribute to add
    * @return copy of [[AttributeList]] with attribute added
    */
  def withAttr(name: String, attribute: Attribute): AttributeList = {
    copy(lookup = lookup + (name -> attribute))
  }

  /** Add a list of attributes to the list.
    *
    * @param list list of attributes to add
    * @return copy of [[AttributeList]] with all attributes added
    */
  def withAttrList(list: AttributeList): AttributeList = {
    list.lookup.foldLeft(this) {
      case (l, (name, attr)) => l.withAttr(name, attr)
    }
  }
}
