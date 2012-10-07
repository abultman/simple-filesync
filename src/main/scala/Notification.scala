package notification

import filesync.{Statistics, BruteForceFileCopier}
import net.sf.prowl.{ProwlEvent, ProwlClient}
import actors.threadpool.{LinkedBlockingQueue, BlockingQueue}
import java.io.{BufferedWriter, File, FileWriter}

/**
 *
 *
 * Date: 10/7/12
 * Team Awesome!
 */

private class Prowl extends Notifications {
  private[this] val client = new ProwlClient(BruteForceFileCopier.config.prowlKey, "filesync")
  client.setTimeouts(10000, 10000);

  override def started = send(new ProwlEvent("%s started" format (BruteForceFileCopier.config.backupName), "starting copy task"))
  override def ok(stats: Statistics) = send(new ProwlEvent("Sync done", "All files copied: %s" format(stats)))
  override def failed(message: String) = send(new ProwlEvent("%s failed" format(BruteForceFileCopier.config.backupName), "The sync has failed: %s" format (message)))


  private[this] def send(event: ProwlEvent) = {
    if (BruteForceFileCopier.config.useProwl) client.pushEvent(event)
  }
}

private class Log extends Notifications {
  private val queue = new LinkedBlockingQueue[String](512)
  private val logFile = new BufferedWriter(new FileWriter(new File(BruteForceFileCopier.config.baseDir, "run.log")))

  val thread = new Thread(new Runnable {
    def run() {
      while(true) {
        logFile.write(queue.take() + "\n\r");
        logFile.flush()
      }
    }
  })
  thread.setDaemon(true)
  thread.start()

  Runtime.getRuntime.addShutdownHook(new Thread(new Runnable {
    def run() {
      while(queue.size() > 0){
        println(queue.size)
      }
    }
  }));

  override def started = log("%s started" format (BruteForceFileCopier.config.backupName))
  override def ok(stats: Statistics) = log("%s ync done all files copied: %s" format(BruteForceFileCopier.config.backupName, stats))
  override def failed(message: String) = log("%s failed with error %s" format(BruteForceFileCopier.config.backupName, message))
  override def dirDone(message: String) = log("processed: %s" format(message))

  private[this] def log(message: String) {
    queue.put(message)
  }
}

private class Notifications {
  def started {}
  def ok(stats: Statistics) {}
  def failed(message: String) {}
  def dirDone(message: String) {}
}

object Notify {
  private[this] val notifiers: List[Notifications] = List(new Prowl, new Log)

  def started = notifiers.foreach(_.started)
  def ok(stats: Statistics) = notifiers.foreach(_.ok(stats))
  def failed(message: String) = notifiers.foreach(_.failed(message))
  def dirDone(message: String) = notifiers.foreach(_.dirDone(message))
}
