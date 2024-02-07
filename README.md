# Description
This is the backend of a web application that converts csv files into json objects.

# Endpoints implemented:
### 1. POST /task/
Create a task with URI pointing to CSV dataset which will be converted to Json and reuturns taskId. Two tasks can be run at the same time
the task is executed immediately if running-tasks < 2

### 2. GET /task/
List Tasks

### 3. GET /task/[taskId]
Returns informations about the task. Eg: lines processed, avg lines processed (count/sec), state (SCHEDULED/RUNNING/DONE/FAILED/CANCELED), result (uri where the JSON file can be downloaded)

### 4. DELETE /task/[taskId]
Cancel Tasks in SCHEDULED or RUNNING state.

# Technology stack:
Play Framework, akka HTTP server

# Instructions to run the application:
1. Download the repo from github.
2. sbt run
3. The application listens at port 9000 by default.
