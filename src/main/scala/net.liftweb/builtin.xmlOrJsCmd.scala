package net.liftweb
package http

import net.liftweb.common._
import net.liftweb.actor._
import net.liftweb.util.Helpers._
import net.liftweb.util._
import net.liftweb.json._
import scala.xml.{NodeSeq, Text, Elem, Node, Group, Null, PrefixedAttribute, UnprefixedAttribute}
import scala.collection.mutable.ListBuffer
import net.liftweb.http.js._
import JsCmds._
import JE._
import java.util.Locale
import provider.HTTPCookie

/* overriding the XmlOrJsCmd class from net.liftweb, so that the CometRenderOrder will be consulted for the construction of cometActors (fixed vs dynamic), on lines 54 and 57.
https://github.com/lift/framework/blob/ac4b07eb00de2364e4fcb23dc392fe89e8e2ae02/web/webkit/src/main/scala/net/liftweb/http/CometActor.scala
 */
private[http] class XmlOrJsCmd(
    val id: String,
    _xml: Box[NodeSeq],
    _fixedXhtml: Box[NodeSeq],
    val javaScript: Box[JsCmd],
    val destroy: Box[JsCmd],
    spanFunc: (NodeSeq) => NodeSeq,
    ignoreHtmlOnJs: Boolean,
    notices: List[(NoticeType.Value, NodeSeq, Box[String])]
) {
  def this(
      id: String,
      ro: RenderOut,
      spanFunc: (NodeSeq) => NodeSeq,
      notices: List[(NoticeType.Value, NodeSeq, Box[String])]
  ) =
    this(id, ro.xhtml, ro.fixedXhtml, ro.script, ro.destroyScript, spanFunc, ro.ignoreHtmlOnJs, notices)

  val xml = _xml.flatMap(content => S.session.map(s => s.processSurroundAndInclude("JS SetHTML id: " + id, content)))
  val fixedXhtml =
    _fixedXhtml.flatMap(content => S.session.map(s => s.processSurroundAndInclude("JS SetHTML id: " + id, content)))

  /** Returns the JsCmd that will be sent to client
    */
  def toJavaScript(session: LiftSession, displayAll: Boolean): JsCmd = {
    val updateJs =
      (if (ignoreHtmlOnJs) Empty else xml, javaScript, displayAll) match {
        case (Full(xml), Full(js), false) =>
          LiftRules.jsArtifacts.setHtml(id, Helpers.stripHead(xml)) & JsCmds.JsTry(js, false)
        case (Full(xml), _, false) => LiftRules.jsArtifacts.setHtml(id, Helpers.stripHead(xml))
        case (Full(xml), Full(js), true) =>
          LiftRules.jsArtifacts.setHtml(
            id + "_outer",
            (
              CometRenderOrder.combineFixedAndDynamic(fixedXhtml.openOr(Text("")), spanFunc(Helpers.stripHead(xml)))
            )
          ) & JsCmds.JsTry(js, false)
        case (Full(xml), _, true) =>
          LiftRules.jsArtifacts.setHtml(
            id + "_outer",
            (
              CometRenderOrder.combineFixedAndDynamic(fixedXhtml.openOr(Text("")), spanFunc(Helpers.stripHead(xml)))
            )
          )
        case (_, Full(js), _) => js
        case _                => JsCmds.Noop
      }
    val fullUpdateJs =
      LiftRules.cometUpdateExceptionHandler.vend.foldLeft(updateJs) { (commands, catchHandler) =>
        JsCmds.Run(
          "try{" +
            commands.toJsCmd +
            "}catch(e){" +
            catchHandler.toJsCmd +
            "}"
        )
      }

    var ret: JsCmd = JsCmds.JsTry(JsCmds.Run("destroy_" + id + "();"), false) &
      fullUpdateJs &
      JsCmds.JsTry(
        JsCmds.Run("destroy_" + id + " = function() {" + (destroy.openOr(JsCmds.Noop).toJsCmd) + "};"),
        false
      )

    S.appendNotices(notices)
    ret = S.noticesToJsCmd & ret
    ret
  }
  def inSpan: NodeSeq = xml.openOr(Text("")) ++ javaScript.map(s => Script(s)).openOr(Text(""))

  def outSpan: NodeSeq = {
    S.appendGlobalJs(Run("var destroy_" + id + " = function() {" + (destroy.openOr(JsCmds.Noop).toJsCmd) + "}"))
    fixedXhtml.openOr(Text(""))
  }
}
