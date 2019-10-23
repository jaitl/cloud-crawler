package com.github.jaitl.crawler.worker.pipeline

private[pipeline] class WarmUpPipelineBuilder[T] {
  private var taskType: Option[String] = None

  def withTaskType(taskType: String): this.type = {
    this.taskType = Some(taskType)
    this
  }

  def build(): WarmUpPipeline[T] = {
    if (taskType.isEmpty) {
      throw new PipelineBuilderException("task type is not defined")
    }

    WarmUpPipeline(
      taskType = taskType.get
    )
  }
}

object WarmUpPipelineBuilder {
  def apply[T](): WarmUpPipelineBuilder[T] = new WarmUpPipelineBuilder[T]()
}

class PipelineBuilderException(message: String) extends Exception(message)
