/*
 * Copyright 2009-2011 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftweb
package http
package provider
package servlet

import java.io.InputStream
import java.util.Locale
import jakarta.servlet.http.{HttpServletRequest}
//import jakarta.servlet.{MultipartConfigElement}
import net.liftweb.common._
import net.liftweb.util._
import Helpers._

class HTTPRequestServlet(@transient val req: HttpServletRequest, @transient val provider: HTTPProvider)
    extends HTTPRequest
    with Logger {
  private val thisRequestServlet = this
  private lazy val ctx = {
    new HTTPServletContext(req.getSession.getServletContext)
  }
  /*
  protected lazy val multipartConfigElement:MultipartConfigElement = new MultipartConfigElement(System.getProperty("java.io.tmpdir"))
  req.setAttribute(HttpServletRequest.MULTIPART_CONFIG_ELEMENT, multipartConfigElement)
   */
  lazy val cookies: List[HTTPCookie] = {
    req.getSession(false) // do this to make sure we capture the JSESSIONID cookie
    (Box !! req.getCookies).map(
      _.toList.map(c =>
        HTTPCookie(
          c.getName,
          Box !! (c.getValue),
          Box !! (c.getDomain),
          Box !! (c.getPath),
          Box !! (c.getMaxAge),
          Box !! (c.getVersion),
          Box !! (c.getSecure)
        )
      )
    ) openOr Nil
  }

  lazy val authType: Box[String] = Box !! req.getAuthType

  def headers(name: String): List[String] =
    for {
      h  <- (Box !! req.getHeaders(name)).asA[java.util.Enumeration[String]].toList
      li <- enumToList[String](h) if null != li
    } yield li

  lazy val headers: List[HTTPParam] =
    for {
      hne <- (Box !! req.getHeaderNames).asA[java.util.Enumeration[String]].toList
      n   <- enumToList[String](hne) if null != n
      hl  <- Full(headers(n)) if !hl.isEmpty
    } yield HTTPParam(n, hl)

  def contextPath: String = req.getContextPath

  def context: HTTPContext = ctx

  def contentType = Box !! req.getContentType

  // don't cache... allow multiple sessions for the request
  // necessary for session destruction on login
  def session = new HTTPServletSession(req.getSession)

  def uri = req.getRequestURI

  def url = req.getRequestURL.toString

  lazy val queryString: Box[String] = Box !! req.getQueryString

  def param(name: String): List[String] = req.getParameterValues(name) match {
    case null => Nil
    case x    => x.toList
  }

  lazy val params: List[HTTPParam] =
    enumToList[String](req.getParameterNames.asInstanceOf[java.util.Enumeration[String]]).map(n =>
      HTTPParam(n, param(n))
    )

  lazy val paramNames: List[String] = params map (_.name)

  def remoteAddress: String = req.getRemoteAddr

  /** The User-Agent of the request
    */
  lazy val userAgent: Box[String] = headers find (_.name equalsIgnoreCase "user-agent") flatMap (_.values.headOption)

  def remotePort: Int = req.getRemotePort

  def remoteHost: String = req.getRemoteHost

  def serverName = req.getServerName

  def scheme: String = req.getScheme

  def serverPort = req.getServerPort

  def method: String = req.getMethod

  def locale: Box[Locale] = Box !! req.getLocale

  def inputStream: InputStream = req.getInputStream

  protected lazy val reqParts = req.getParts()
  def multipartContent_? =
    try {
      reqParts
      true
    } catch {
      case e: jakarta.servlet.ServletException => {
        false
      }
    }

  /** Destroy the underlying servlet session
    */
  def destroyServletSession() {
    for {
      httpSession <- Box !! req.getSession(false)
    } yield httpSession.invalidate()
  }

  /** @return
    *   the sessionID (if there is one) for this request. This will *NOT* create a new session if one does not already
    *   exist
    */
  def sessionId: Box[String] =
    for {
      httpSession <- Box !! req.getSession(false)
      id          <- Box !! httpSession.getId
    } yield id

  def extractFiles: List[ParamHolder] = {
    trace("extracting files from req");
    val iter = new Iterator[ParamHolder] {
      val liftReq = CurrentReq.value
      val parts   = reqParts
      import scala.collection.JavaConverters._
      val iterable = parts.asScala.toIterator

      def hasNext = {
        val res = iterable.hasNext
        if (res == false) {
          trace("Next: %s".format(res))
          if (liftReq != null) {
            UploadNotifier ! UploadFinished(thisRequestServlet, S.session, liftReq)
          } else {
            UploadNotifier ! OrphanUploadFinished(thisRequestServlet, S.session)
          }
        }
        res
      }

      protected var count: Int = 0
      def next = iterable.next match {
        case null => null
        case f if (f.getSize > 0 && f.getContentType != null && f.getSubmittedFileName != null) => {
          val headerNames: java.util.Collection[String] = f.getHeaderNames()
          val names: List[String] = if (headerNames eq null) {
            Nil
          } else {
            headerNames.asScala.toList
          }
          val map: Map[String, List[String]] = Map(names.map(n => n -> List(f.getHeader(n))): _*)
          count = count + 1
          val target = f.getSize
          LiftRules.withMimeHeaders(map) {
            UploadNotifier ! UploadInformation(thisRequestServlet, S.session, 0, target, count, liftReq)
            trace("extracting file: %s".format(map))
            LiftRules.handleMimeFile(
              f.getName,
              f.getContentType,
              f.getSubmittedFileName,
              new InputStreamTracker(
                f.getInputStream,
                (bytesRead: Long) => {
                  trace("progress: %s / %s".format(bytesRead, target))
                  UploadNotifier ! UploadInformation(thisRequestServlet, S.session, bytesRead, target, count, liftReq)
                }
              )
            )
          }
        }
        case f => {
          NormalParamHolder(f.getName, new String(readWholeStream(f.getInputStream), "UTF-8"))
        }
      }
    }
    iter.toList
  }

  def setCharacterEncoding(encoding: String) = req.setCharacterEncoding(encoding)

  def snapshot: HTTPRequest = new OfflineRequestSnapshot(this, provider)

  private lazy val asyncProvider: Box[ServletAsyncProvider] =
    LiftRules.theServletAsyncProvider.map(_(this))

  def resumeInfo: Option[(Req, LiftResponse)] = asyncProvider.flatMap(_.resumeInfo)

  def suspend(timeout: Long): RetryState.Value =
    asyncProvider
      .openOrThrowException("open_! is bad, but presumably, the suspendResume support was checked")
      .suspend(timeout)

  def resume(what: (Req, LiftResponse)): Boolean =
    asyncProvider
      .openOrThrowException("open_! is bad, but presumably, the suspendResume support was checked")
      .resume(what)

  lazy val suspendResumeSupport_? = {
    LiftRules.asyncProviderMeta.map(
      _.suspendResumeSupport_? &&
        (asyncProvider.map(_.suspendResumeSupport_?) openOr
          false)
    ) openOr false
  }
}
