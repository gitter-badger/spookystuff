package org.tribbloid.spookystuff.pages

/**
 * Created by peng on 11/27/14.
 */
trait Unstructured extends Serializable {

   final def apply(selector: String) = children(selector)

   val uri: String

   def children(selector: String): Seq[Unstructured]

   def markup: Option[String]

   def attr(attr: String, noEmpty: Boolean = true): Option[String]

   final def href = attr("abs:href")

   final def src = attr("abs:src")

   def text: Option[String]

   def ownText: Option[String]

   def boilerPipe(): Option[String]
}

final class UnstructuredSeqView(self: Seq[Unstructured]) {

   def allChildren(selector: String): Seq[Unstructured] = self.flatMap(_.children(selector))

   def markups: Seq[String] = self.flatMap(_.markup)

   def attrs(attr: String, noEmpty: Boolean = true): Seq[String] = self.flatMap(_.attr(attr, noEmpty))

   def hrefs(abs: Boolean) = attrs("abs:href")

   def srcs = attrs("abs:src")

   def texts: Seq[String] = self.flatMap(_.text)

   def ownTexts: Seq[String] = self.flatMap(_.ownText)

   def boilerPipes(): Seq[String] = self.flatMap(_.boilerPipe())
}

