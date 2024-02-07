package controllers

import javax.inject._
import play.api.mvc._
import services.TaskService

@Singleton
class TaskController @Inject()(cc: ControllerComponents, taskService: TaskService) extends AbstractController(cc) {

  def createTask = Action(parse.json) { request =>
    val csvUri = (request.body \ "csvUri").asOpt[String]

    csvUri match {
      case Some(uri) =>
        taskService.createTask("services/sample.csv")
        Ok("Task created successfully")
      case None =>
        BadRequest("Missing parameter: csvUri")
    }
  }

  def getTask(taskId: String): Action[AnyContent] = Action {
    taskService.getTask(taskId) match {
      case Some(task) => Ok(task.toString)
      case None => NotFound("Task not found")
    }
  }

  def getAllTasks: Action[AnyContent] = Action {
    Ok(taskService.getAllTasks.toString)
  }

  def cancelTask(taskId: String): Action[AnyContent] = Action {
    taskService.cancelTask(taskId)
    Ok(s"Task $taskId canceled")
  }
}
