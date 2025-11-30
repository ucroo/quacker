package metl.model

import net.liftweb.http._
import net.liftweb.common._
import net.liftweb.util._
import net.liftweb.util.Helpers._
import scala.xml._
import scala.collection.mutable.{HashMap, SynchronizedMap, ListBuffer}
import scala.collection.JavaConversions._
import com.mongodb.BasicDBObject
import com.metl.liftAuthenticator._
import com.metl.cas._

import scala.collection.JavaConverters._

object EnvVariable extends Logger {
  protected val environmentVariables:Map[String,String] = System.getenv.asScala.toMap;
  trace(environmentVariables)
  protected def trimSystemProp(in:String):Option[String] = {
    try {
      var value = in.trim
      if (value.startsWith("\"")){
        value = value.drop(1)
      }
      if (value.endsWith("\"")){
        value = value.reverse.drop(1).reverse
      }
      Option(value)
    } catch {
      case e:Exception => ParamFailure("exception while getting systemProperty",Full(e),Empty,in)
    }
  }
  def getProp(systemEnvName:String,javaPropName:String):Option[String] = {
    lazy val defaultValue = Props.get(javaPropName).toOption.flatMap(v => Option(v)).orElse(Option(System.getProperty(javaPropName)))

    val fromEnvironmentVariables = for {
      env <-    environmentVariables.get(systemEnvName)
      trim <- trimSystemProp(env)
    } yield trim

    fromEnvironmentVariables.orElse(defaultValue)
    
  }
}

object Globals extends Logger {
  //Globals for the system
  val configDirectoryLocation = EnvVariable.getProp("QUACKER_CONFIG_DIRECTORY_LOCATION","quacker.configDirectoryLocation").getOrElse("config")
  var isDevMode = false
	protected var validUserAccesses:List[UserAccessRestriction] = List.empty[UserAccessRestriction]
	def setValidUsers(newUsers:List[UserAccessRestriction]) = {
		validUserAccesses = (validUserAccesses ::: newUsers).toList
		currentUserAccessRestriction(updatedUserAccessRestriction)
	}
	protected def updatedUserAccessRestriction:UserAccessRestriction = {
		val me = currentUser.is
		val validUserAccessesForMe = validUserAccesses.filter(vua => vua.name == me)
		val myRestriction = validUserAccessesForMe.length match {
			case 0 => UserAccessRestriction(me,List(new ServicePermission("Public Access Only",None,None,None,None)))
			case other => UserAccessRestriction(me,validUserAccessesForMe.map(vua => vua.servicePermissions).flatten.toList)
		}
    trace("myUserAccessRestriction: %s".format(myRestriction))
    myRestriction
	}
	def clearValidUsers = {
		validUserAccesses = List.empty[UserAccessRestriction]
	}
	def isValidUser = validUserAccesses.exists(vua => vua.name == currentUser.is)
  //Globals for the current session
  object casState extends SessionVar[LiftAuthStateData](LiftAuthStateDataForbidden)
  object currentUser extends SessionVar[String](casState.is.username)
	object currentUserAccessRestriction extends SessionVar[UserAccessRestriction](updatedUserAccessRestriction)
}
