package net.liftweb
package http
package provider
package servlet

import net.liftweb.actor.{LiftActor, LAFuture}

import net.liftweb.http._
import net.liftweb.http.rest._
import net.liftweb.common._
import org.apache.commons.fileupload.servlet._
import jakarta.servlet.http.{HttpServletRequest}
import org.apache.commons.fileupload.{ProgressListener, FileUploadBase, FileItemIterator, FileItemStream}
import org.apache.commons.io.IOUtils

import net.liftweb.util._
import Helpers._
import scala.collection.mutable.{HashMap => MuteMap}
import scala.language.postfixOps
import java.io.InputStream

class InputStreamTracker(stream: InputStream, onChange: Long => Unit) extends InputStream {
  protected var readBytes: Long                  = 0
  protected var mark: Option[Tuple2[Long, Long]] = None
  protected var readSinceMark: Option[Long]      = None
  protected def updateMark(readCount: Long): Unit = {
    readSinceMark = for {
      rsm <- readSinceMark
      m   <- mark
      if (m._2 > rsm)
    } yield {
      rsm + readCount
    }
  }
  override def read(b: Array[Byte]): Int = {
    val readCount = stream.read(b)
    readBytes = readBytes + readCount
    updateMark(readCount)
    onChange(readBytes)
    readCount
  }
  override def read(b: Array[Byte], offset: Int, len: Int): Int = {
    val readCount = stream.read(b, offset, len)
    readBytes = readBytes + readCount
    updateMark(readCount)
    onChange(readBytes)
    readCount
  }
  override def skip(n: Long): Long = {
    val readCount = stream.skip(n)
    readBytes = readBytes + readCount
    updateMark(readCount)
    onChange(readBytes)
    readCount
  }
  override def read: Int = {
    val readCount = stream.read()
    if (readCount != -1) {
      readBytes = readBytes + readCount
      updateMark(readCount)
    }
    onChange(readBytes)
    return readCount
  }
  override def reset: Unit = {
    stream.reset
    if (markSupported) {
      for {
        m   <- mark
        rsm <- readSinceMark
      } yield {
        readBytes = m._1
      }
    }
  }
  override def close: Unit = {
    stream.close
  }
  override def markSupported: Boolean = {
    stream.markSupported
  }
  override def available: Int = {
    stream.available
  }
  override def mark(readLimit: Int): Unit = {
    if (markSupported) {
      mark = Some(Tuple2(readBytes, readLimit))
      readSinceMark = Some(0)
      stream.mark(readLimit)
    }
  }
}

case class Subscribe(actor: LiftActor, session: Option[LiftSession] = S.session)
case class UnSubscribe(actor: LiftActor, session: Option[LiftSession] = S.session)
case class SubscriptionConfirmation(actor: LiftActor, session: Option[LiftSession], subscribed: Boolean)

object EmptyFileItemIterator extends FileItemIterator {
  def hasNext = false
  def next    = null
}

object UploadNotifier extends LiftActor with Logger {
  protected val sessionListeners = new MuteMap[LiftSession, List[LiftActor]]()
  protected var globalListeners  = List.empty[LiftActor]
  protected val aggregators      = new MuteMap[String, UploadAggregator]()
  protected def sendUiToChildren(ui: UploadInformation) = {
    ui.session.map(s => {
      sessionListeners
        .get(s)
        .toList
        .flatten
        .foreach(a => {
          trace("Sending %s to child %s".format(ui, a))
          a ! ui
        })
    })
    globalListeners.foreach(a => {
      a ! ui
    })
  }
  protected var failedReqs: List[net.liftweb.http.Req] = Nil
  override def messageHandler = {
    case Subscribe(a, session) => {
      session
        .map(s => {
          val currentList = sessionListeners.get(s).getOrElse(Nil)
          sessionListeners += ((s, a :: currentList))
        })
        .getOrElse({
          globalListeners
            .find(_ == a)
            .getOrElse({
              globalListeners = a :: globalListeners
              globalListeners
            })
        })
      reply(SubscriptionConfirmation(a, session, true))
    }
    case UnSubscribe(a, session) => {
      session
        .map(s => {
          sessionListeners.get(s).map {
            case List(a) => {
              sessionListeners - s
            }
            case Nil => {}
            case l => {
              sessionListeners += ((s, l.filterNot(_ == a)))
            }
          }
        })
        .getOrElse({
          globalListeners = globalListeners.filterNot(_ == a)
        })
      reply(SubscriptionConfirmation(a, session, false))
    }
    case uf @ UploadFinished(req, session, liftReq) => {
      val agId = UploadAggregator.createId(liftReq)
      aggregators
        .get(agId)
        .map(ag => {
          if (UploadAggregator.shouldClose(ag)) {
            aggregators -= agId
            trace("finished so removing aggregator: %s (%s)".format(aggregators, uf))
          } else {
            trace("delaying upload aggregator shutdown: %s (%s)".format(aggregators, uf))
            Schedule.schedule(this, uf, 1 minute)
          }
        })
      ()
    }
    case f @ UploadFailed(req, session, liftReq, e) => {
      failedReqs = (liftReq :: failedReqs).distinct
      session.map(s => {
        sessionListeners
          .get(s)
          .toList
          .flatten
          .foreach(a => {
            trace("Sending to child %s".format(a))
            a ! f
          })
      })
      globalListeners.foreach(a => {
        a ! f
      })
      trace("failed so not yet removing aggregator: %s (%s) (%s)".format(aggregators, f, failedReqs))
    }
    case ui: UploadInformation => {
      val agId = UploadAggregator.createId(ui.liftReq)
      val ag = aggregators
        .get(agId)
        .getOrElse({
          val newAg = new UploadAggregator(
            ui.liftReq,
            (ui) => {
              sendUiToChildren(ui)
            },
            (ui) => {
              sendUiToChildren(ui)
            }
          )
          aggregators += ((agId, newAg))
          trace("creating aggregator: %s".format(newAg))
          newAg
        })
      ag.addItem(ui)
    }
    case other => {
      trace("UploadNotifier received unknown message: %s".format(other))
    }
  }
}

import net.liftweb.json._
import JsonDSL._
case class FileSummary(field: String, name: String, size: Long, lastModified: Long, `type`: String)
object UploadAggregator extends Logger {
  protected val idleTimeout: Long = 2 * 60 * 1000
  def shouldClose(ua: UploadAggregator): Boolean = {
    (new java.util.Date().getTime - ua.getRecentInterest) > idleTimeout
  }
  def createId(req: Req): String = {
    require(req != null)
    val reqId = "%s|%s|%s".format(req.path.wholePath, req.request.params, req.request.headers)
    trace("createId: %s".format(reqId))
    reqId
  }
}
class UploadAggregator(req: Req, onSignificantChange: UploadInformation => Unit, onComplete: UploadInformation => Unit)
    extends Logger {
  trace("creating uploadAggregator: %s:::%s".format(this, req, req.nanoStart))
  protected var prevItem: Option[UploadInformation] = None
  protected var lastSoFar: Long                     = 0
  protected var finished: Boolean                   = false
  protected lazy val threshold                      = 50 * 1024 // 50KB threshold
  protected var lastInterest: Long                  = 0L
  def getRecentInterest: Long                       = lastInterest
  protected implicit lazy val formats               = DefaultFormats
  protected lazy val fileUploadSummaries: Box[List[FileSummary]] = for {
    iSummString <- req.param("fileUploadSummaries")
    iSumms <- tryo({
      val json = parse(iSummString)
      json.extract[List[FileSummary]]
    })
  } yield {
    iSumms
  }
  protected var completedTargets = List.empty[Tuple2[FileParamHolder, FileSummary]]
  def targets(item: UploadInformation) = {
    item.liftReq.uploadedFiles.flatMap(fph => {
      paramSummaryForParamHolder(fph).map(ps => (fph, ps))
    })
  }
  def currentTarget(item: UploadInformation) = {
    targets(item).dropWhile(t => completedTargets.contains(t)).headOption
  }
  protected def paramSummaryForParamHolder(item: FileParamHolder): Box[FileSummary] = {
    fileUploadSummaries.getOrElse(Nil).find(fus => fus.name == item.fileName && fus.field == item.name)
  }
  protected def possiblySendForCompletingItems(item: UploadInformation): Unit = {
    val currTarget          = currentTarget(item)
    val currentItemProgress = item.soFar - completedTargets.map(_._2.size).sum

    if (prevItem.exists(_.fieldNumber != item.fieldNumber)) {
      if (
        currTarget.exists(ct => {
          currentItemProgress > ct._2.size
        })
      ) {
        completedTargets = completedTargets ::: currTarget.toList
        val nm = item.copy(
          soFar = completedTargets.map(_._2.size).sum,
          itemProgress = Some((currTarget.map(_._2.size).getOrElse(-1), currTarget.map(_._2.size).getOrElse(-1))),
          paramHolder = currTarget.map(_._1),
          paramSummary = currTarget.map(_._2)
        )
        trace("Possibly send significant change %s".format(item))
        onSignificantChange(nm)
        possiblySendForCompletingItems(item)
      }
    }
  }
  def addItem(item: UploadInformation) = {
    lastInterest = new java.util.Date().getTime
    if (!finished) {
      if (item.failure.isDefined) {
        val ct = currentTarget(item)
        warn("failure in aggregator: %s => %s (%s)".format(item.liftReq.params.toList, req.params.toList, ct))
        // if (req.params.toList != Nil){ // not yet sure how to calculate this correctly - seems like we never have the information we need
        finished = true
        onComplete(
          item.copy(
            itemProgress =
              None, // Some((item.soFar - completedTargets.map(_._2.size).sum,ct.map(_._2.size).getOrElse(-1L))),
            paramHolder = ct.map(_._1),
            paramSummary = ct.map(_._2),
            complete = true
          )
        )
        // }
      } else {
        val currTarget          = currentTarget(item)
        val currentItemProgress = item.soFar - completedTargets.map(_._2.size).sum
        possiblySendForCompletingItems(item) // this is a threshold to send completions for each complete file
        val ct = currentTarget(item)
        val nm =
          if ((item.soFar - lastSoFar) > threshold) { // this is a total size threshold, to send progress messages according to a total upload rate
            lastSoFar = item.soFar
            trace("Add item significant change %s".format(item))
            onSignificantChange(
              item.copy(
                itemProgress =
                  Some((item.soFar - completedTargets.map(_._2.size).sum, ct.map(_._2.size).getOrElse(-1L))),
                paramHolder = ct.map(_._1),
                paramSummary = ct.map(_._2)
              )
            )
          }
        if (item.soFar == item.target) { // this is a threshold to send completed total
          trace("Marking agg finished %s %s".format(item.soFar, item.target))
          finished = true
          onComplete(
            item.copy(
              itemProgress = Some((item.soFar - completedTargets.map(_._2.size).sum, ct.map(_._2.size).getOrElse(-1L))),
              paramHolder = ct.map(_._1),
              paramSummary = ct.map(_._2),
              complete = true
            )
          )
        }
      }
      prevItem = Some(item)
    }
  }
}
case class UploadFinished(request: HTTPRequestServlet, session: Box[LiftSession], liftReq: Req)
case class OrphanUploadFinished(request: HTTPRequestServlet, session: Box[LiftSession])
case class UploadInformation(
    request: HTTPRequestServlet,
    session: Box[LiftSession],
    soFar: Long,
    target: Long,
    fieldNumber: Int,
    liftReq: Req,
    itemProgress: Option[Tuple2[Long, Long]] = None,
    paramHolder: Box[FileParamHolder] = None,
    paramSummary: Box[FileSummary] = None,
    complete: Boolean = false,
    failure: Box[Throwable] = Empty
)
case class UploadFailed(request: HTTPRequestServlet, session: Box[LiftSession], liftReq: Req, failure: Box[Throwable]) {
  def getReqId: String = UploadAggregator.createId(liftReq)
}
