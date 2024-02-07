package models
import play.api.libs.json.{Json, Writes}

case class Task(taskId: String,
                csvUri: String,
                var linesProcessed: Long = 0,
                var avgLinesProcessed: Double = 0,
                var state: String = "SCHEDULED", // Possible states: SCHEDULED, RUNNING, DONE, FAILED, CANCELED
                var resultUri: Option[String] = None
               )

object Task {
  implicit val taskWrites: Writes[Task] = Json.writes[Task]
}