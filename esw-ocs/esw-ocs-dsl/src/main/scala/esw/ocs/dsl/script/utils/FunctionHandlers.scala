package esw.ocs.dsl.script.utils

import scala.collection.mutable

private[esw] class FunctionHandlers[I, O] {
  private val handlers: mutable.Buffer[I => O] = mutable.Buffer.empty

  def add(handler: I => O): Unit = handlers += handler

  def execute(input: I): List[O] = handlers.map(f => f(input)).toList

  def ++(that: FunctionHandlers[I, O]): FunctionHandlers[I, O] = {
    this.handlers ++= that.handlers
    this
  }
}
