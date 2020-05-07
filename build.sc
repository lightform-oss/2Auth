// build.sc
import mill._, scalalib._

object core extends JavaModule

object scalaapi extends ScalaModule {
  def scalaVersion = "2.13.1"

  def moduleDeps = Seq(core)
  def ivyDeps = Agg(ivy"org.typelevel::cats-core:2.1.1")

  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.1.1")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object javaapi extends ScalaModule {
  def scalaVersion = "2.13.1"
  def moduleDeps = Seq(core, scalaapi)

  // workaround https://github.com/lihaoyi/mill/issues/860
  def artifactSuffix = ""
}
