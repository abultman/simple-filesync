package filesync.config

import org.streum.configrity._
import converter.Extra._
import java.io.File
import org.streum.configrity.converter.Extra

class SyncConfig {

  def baseDir = {
    val location = getClass.getProtectionDomain.getCodeSource.getLocation.getFile
    if (location.contains("\\.jar")) {
      location.replaceFirst("^file:", "").replaceFirst("""(.*)[/\\].*?\.jar.*""", "$1")
    } else {
      new File("").getAbsolutePath
    }

  }

  private val config = Configuration.load("%s%ssettings.properties" format (baseDir, File.separator))

  def useProwl = config[Boolean]("useProwl", false)
  def prowlKey = config[String]("prowlkey", "")

  def backupName = config[String]("backupname", "backuptask")

  def source = config[File]("source")
  def dest = config[File]("dest")

  def ignoredExts = config[String]("ignoreExtensions").split(",").toList.map(_.trim.toLowerCase)
  def ignoredDirs = config[String]("ingoreFolders").split(",").toList.map(_.trim.toLowerCase)


}
