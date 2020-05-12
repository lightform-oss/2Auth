// build.sc
import mill._, scalalib._

val scalaV = "2.13.1"

object `2auth` extends JavaModule

object scalaapi extends ScalaModule {
  def scalaVersion = scalaV

  def moduleDeps = Seq(`2auth`)
  def ivyDeps = Agg(ivy"org.typelevel::cats-core:2.1.1")

  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.1.1")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
    def moduleDeps = Seq(services)
  }
}

object services extends ScalaModule {
  def scalaVersion = scalaV

  def moduleDeps = Seq(scalaapi)

  object Jose {
    val version = "0.3.1"
    val core    = ivy"black.door::jose:$version"
    val json    = ivy"black.door::jose-json-play:$version"
  }

  def ivyDeps = Agg(
    Jose.core, Jose.json, 
    ivy"de.mkammerer:argon2-jvm:2.7"
  )

  object test extends Tests {
    def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.1.1")
    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }
}

object javaapi extends ScalaModule {
  def scalaVersion = scalaV
  def moduleDeps = Seq(services)

  // workaround https://github.com/lihaoyi/mill/issues/860
  // def artifactSuffix = ""
}
