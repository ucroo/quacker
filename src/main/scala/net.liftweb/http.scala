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

/** Overrides the default behaviour and updates with this stuff Impersonates the actual comet response content
  */
object CometRenderOrder {
  protected var fixedBeforeDynamic: Boolean = true
  def combineFixedAndDynamic(fixed: NodeSeq, dynamic: NodeSeq): NodeSeq = fixedBeforeDynamic match {
    case true  => fixed ++ dynamic
    case false => dynamic ++ fixed
  }
}

class AdminReadableSessionVar[AnyType](default: AnyType) extends SessionVar[AnyType](default) {
  def getValueForSession(session: LiftSession): Option[AnyType] = session.get(name)
}
object ResponseCookieEnricher extends Logger {
  def enrich(in: LiftResponse, cookieFunc: List[HTTPCookie] => List[HTTPCookie]): Option[LiftResponse] = {
    lazy val originalCookies = in.toResponse.cookies
    lazy val nc              = cookieFunc(originalCookies)
    in match {
      /*
      case EmptyResponse => Some(InMemoryResponse(Array.empty[Byte],Nil,nc,200))
      case r:OkResponse => Some(InMemoryResponse(Array.empty[Byte],r.headers,nc,200))
      case r:AcceptedResponse => Some(InMemoryResponse(Array.empty[Byte],r.headers,nc,202))
      case r:NoContentResponse => Some(InMemoryResponse(Array.empty[Byte],r.headers,nc,204))
      case r:ResetContentResponse => Some(InMemoryResponse(Array.empty[Byte],r.headers,nc,205))
      case r:JavaScriptResponse => Some(r.copy(cookies = nc))
      case r:JsonResponse => Some(r.copy(cookies = nc))
      case r@ResponseWithReason(resp,reason) => enrich(resp,cookieFunc).map(tr => ResponseWithReason(tr,reason))
      case r:StreamingResponse => Some(r.copy(cookies = nc))
      case r:OutputStreamResponse => Some(r.copy(cookies = nc))
       */
      case x: XhtmlResponse => Some(x.copy(cookies = nc))
      // case i:InMemoryResponse => Some(i.copy(cookies = nc))
      case o => {
        None
      }
    }
  }
}
