package services

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{Materializer, OverflowStrategy}
import play.api.libs.json.{JsArray, JsObject, JsValue, Json}
import models.Task
import akka.actor.ActorRef

import scala.util.Success
import scala.util.Failure
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import com.opencsv.CSVReader
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.io.Source

import scala.concurrent.duration.DurationInt

class TaskService @Inject()(implicit ec: ExecutionContext, system: ActorSystem, materializer: Materializer){

  private var tasks: Map[String, Task] = Map.empty
  private val webSocketClients: Map[String, ActorRef] = Map.empty

  def getTask(taskId: String): Option[Task] = tasks.get(taskId)
  def getAllTasks: Seq[Task] = tasks.values.toSeq

  def cancelTask(taskId: String): Unit = {
    tasks.get(taskId) match {
      case Some(task) if task.state == "SCHEDULED" || task.state == "RUNNING" =>
        tasks += taskId -> task.copy(state = "CANCELED")
      case _ =>
      // Task not found or cannot be canceled
    }
  }

  def createTask(csvUri: String): String = {
    val taskId = java.util.UUID.randomUUID().toString
    val task = Task(taskId, csvUri)
    tasks += taskId -> task

    // Execute the task immediately if running tasks < 2
    if (tasks.count(_._2.state == "RUNNING") < 2) {
      executeTask(taskId)
    }

    taskId
  }

  private def executeTask(taskId: String): Unit = {
    tasks.get(taskId) match {
      case Some(task) if task.state == "SCHEDULED" =>
        tasks += taskId -> task.copy(state = "RUNNING")

        // Simulate CSV to JSON conversion
        val conversionFuture = convertCsvToJsonFromPc(task)

        // Create a separate source for periodic updates
        val updateSource: akka.stream.scaladsl.Source[JsValue, ActorRef] = akka.stream.scaladsl.Source.actorRef[JsValue](bufferSize = 10, OverflowStrategy.fail)
          .mapMaterializedValue { actorRef =>
            // Schedule periodic updates
            system.scheduler.scheduleWithFixedDelay(0.seconds, 2.seconds) { () =>
              actorRef ! Json.toJson(tasks.getOrElse(taskId, Task(taskId, "Unknown", 0, 0, "UNKNOWN")))
            }(system.dispatcher)
            actorRef
          }

        val conversionSource: akka.stream.scaladsl.Source[JsValue, NotUsed] = akka.stream.scaladsl.Source.future(conversionFuture.map(Json.toJson(_)))
        val mergedSource: akka.stream.scaladsl.Source[JsValue, NotUsed] = conversionSource.merge(updateSource)

        // Send the updates to the client through WebSocket
        webSocketClients.get(taskId).foreach { clientActor =>
          mergedSource.runWith(Sink.actorRef(clientActor, onCompleteMessage = None))
        }

        // Update task status when conversion completes
        conversionFuture.onComplete {
          case Success(resultUri) =>
            tasks += taskId -> task.copy(state = "DONE", resultUri = Some(resultUri))
          case Failure(_) =>
            tasks += taskId -> task.copy(state = "FAILED")
        }

      case _ =>
      // Task not found or cannot be executed
    }
  }

  private def simulateConversion(task: Task): Future[String] = {
    // Simulate the CSV to JSON conversion process
    Future {
      val conversionResult = s"result_${task.taskId}.json"
      Thread.sleep(5000) // Simulating a time-consuming process

      // Simulate cancellation (interrupt the thread)
      if (task.state == "CANCELED") {
        throw new InterruptedException("Task canceled")
      }

      conversionResult
    }
  }

  private def convertCsvToJsonFromPc(task: Task): Future[String] = Future {
    val csvReader = new CSVReader(new java.io.FileReader("sample.csv"))

    // Read the header
    val header = csvReader.readNext()

    // Read the remaining rows and convert to JSON objects
    val jsonArray = Iterator.continually(csvReader.readNext()).takeWhile(_ != null).map { row =>
      val jsonFields = header.zip(row).map { case (key, value) =>
        key -> Json.toJson(value)
      }
      JsObject(jsonFields)
    }.toSeq

    csvReader.close()

    JsArray(jsonArray).toString()
  }

  private def notifyClients(taskId: String, message: JsValue): Unit = {
    webSocketClients.get(taskId).foreach { clientActor =>
      clientActor ! message
    }
  }
}
