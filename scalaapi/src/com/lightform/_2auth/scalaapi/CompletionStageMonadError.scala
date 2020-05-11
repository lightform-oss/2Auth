package com.lightform._2auth.scalaapi

import cats.MonadError
import java.util.concurrent.CompletionStage
import java.util.concurrent.CompletableFuture
import cats.StackSafeMonad

object CompletionStageMonadError
    extends MonadError[CompletionStage, Throwable]
    with StackSafeMonad[CompletionStage] {

  def pure[A](x: A) = CompletableFuture.completedStage(x)

  def raiseError[A](e: Throwable) = CompletableFuture.failedStage(e)

  def handleErrorWith[A](
      fa: CompletionStage[A]
  )(f: Throwable => CompletionStage[A]) = {
    var outcome: CompletionStage[A] = null
    fa.whenComplete {
      case (null, e) => outcome = f(e)
      case _         => outcome = fa
    }
    outcome
  }

  def flatMap[A, B](fa: CompletionStage[A])(f: A => CompletionStage[B]) =
    fa.thenCompose(a => f(a))

  def instance: MonadError[CompletionStage, Throwable] = this
}
