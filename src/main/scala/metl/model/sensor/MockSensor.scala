package metl.model.sensor

import metl.model.{Sensor, SensorMetaData}
import net.liftweb.util.Helpers._

case class CountingSensor(metadata:SensorMetaData, time:TimeSpan = 5 seconds) extends Sensor(metadata){
  override val pollInterval = time
  protected var count = 0
  def status = {
    count += 1
    count.toString
  }
  override def performCheck = succeed(status.toString)
}

case class WaitingSensor(metadata:SensorMetaData, minTime:Long, maxAdditionalTime:Int, time:TimeSpan = 5 seconds) extends Sensor(metadata){
  override val pollInterval = time
  def status = {
    val waitTime = minTime + (scala.util.Random.nextInt(maxAdditionalTime))
    Thread.sleep(waitTime)
    "waited %s".format(waitTime)
  }
  override def performCheck = succeed(status.toString)
}