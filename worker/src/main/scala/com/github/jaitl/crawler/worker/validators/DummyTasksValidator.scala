package com.github.jaitl.crawler.worker.validators
import com.github.jaitl.crawler.models.task.Task

class DummyTasksValidator extends BatchTasksValidator {
  override def validateBatchItem(task: Task): Boolean = false
}
