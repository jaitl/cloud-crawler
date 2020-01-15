package com.github.jaitl.crawler.worker.validators

import com.github.jaitl.crawler.models.task.Task

trait BatchTasksValidator {
  def validateBatchItem(task: Task): Boolean
}
