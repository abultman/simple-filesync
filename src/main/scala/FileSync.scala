package filesync

import config.SyncConfig
import scala.collection.JavaConverters._
import java.io.{FileInputStream, FileOutputStream, IOException, File}
import notification.Notify._

class BruteForceFileCopier() {

  def sync(source: File, dest: File): Statistics = {
    source.listFiles().toList.par.foldLeft(Statistics.empty)((a,b) => a + syncFile(b, dest))
  }

  private def syncFile(file: File, dest: File): Statistics = {
    val destFile = mkDestFile(dest, file)
    if (file.isDirectory) syncDir(file, destFile)
    else if (shouldSkip(file, destFile)) skip(destFile)
    else (copy(file, destFile))
  }

  private def syncDir(file: File, destFile: File) = {
    if(!(BruteForceFileCopier.config.ignoredDirs.exists(file.getAbsolutePath.toLowerCase.contains(_)))) {
      val stats = Statistics.directory + sync(file,mkDir(destFile))
      dirDone(destFile.getAbsolutePath)
      stats
    }
    Statistics.empty
  }

  private def shouldSkip(source: File, dest: File) = {
    isIgnored(source) || isUnchanged(source, dest)
  }

  private def isUnchanged(source: File, dest: File) = {
    dest.exists && source.length() == dest.length() && source.lastModified == dest.lastModified
  }

  private def isIgnored(source: File) = BruteForceFileCopier.config.ignoredExts.exists(source.getAbsolutePath.toLowerCase.endsWith(_))

  private def skip(file: File) = {
    Statistics.skip
  }

  private def mkDir(file: File) = {
    if (file.exists || file.mkdir) file
    else throw new IOException("Unable to create dir %s" format (file.getAbsolutePath))
  }

  private[this] def mkDestFile(dest: File, source: File): File = {
    new File(dest.getAbsolutePath, source.getName)
  }

  private def copy(source: File, dest: File) = {
    val out = new FileOutputStream(dest)
    val in = new FileInputStream(source)
    out getChannel() transferFrom(
       in getChannel, 0, Long.MaxValue )
    out.flush()
    out.close()
    in.close()
    dest.setLastModified(source.lastModified())
    Statistics.copy
  }

}

case class Statistics(copied: Int = 0, skipped: Int = 0, directories: Int = 0, totalTime: Long = 0) {
  def + (that: Statistics) = new Statistics(this.copied + that.copied, this.skipped + that.skipped, this.directories + that.directories);

  def withRuntime(time: Long) = copy(totalTime = time);
}

object Statistics {
  val empty = new Statistics(0,0,0);
  val skip = new Statistics(0,1,0);
  val copy = new Statistics(1,0,0);
  val directory = new Statistics(0,0,1);
}

object BruteForceFileCopier {

  def config = new SyncConfig

  def main(args: Array[String]) {
    println("Base dir: %s" format (config.baseDir))
    val (source, dest) = getValidDirectories

    val start = System.currentTimeMillis();
    started
    try {
      val stats = new BruteForceFileCopier().sync(source, dest)
      val stop = System.currentTimeMillis();
      ok(stats.withRuntime(stop - start))
    } catch {
      case e => {
        failed(e.getMessage)
        e.printStackTrace()
      }
    }
  }

  def getValidDirectories = {
    val source = BruteForceFileCopier.config.source
    val dest = BruteForceFileCopier.config.dest
    if (!(source.exists && dest.exists )) throw new IllegalStateException("source and dest must exist")
    (source, dest)
  }
}


