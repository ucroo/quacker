package code.core.logback

import ch.qos.logback.core.boolex.PropertyConditionBase

class PropertyExistsCondition extends PropertyConditionBase {
  private var key: String = _

  def getKey: String              = key
  def setKey(value: String): Unit = key = value

  override def evaluate(): Boolean = {
    if (key == null) return false
    isDefined(key)
  }
}
