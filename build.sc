import mill._, scalalib._, scalanativelib._, publish._, scalafmt._

val scala213 = "2.13.6"
val scala3 = "3.0.0"
val dottyCustomVersion = Option(sys.props("dottyVersion"))

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.10")
  def testFramework = "utest.runner.Framework"
}
trait CmdrModule
    extends CrossScalaModule
    with ScalafmtModule
    with PublishModule {

  def scalacOptions = Seq("-deprecation", "-release", "8")

  def ivyDeps = Agg(ivy"com.lihaoyi::os-lib::0.7.8")

  def publishVersion = "0.11.0"
  def pomSettings = PomSettings(
    description = "cmdr",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/cmdr",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("jodersky", "cmdr"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky", "https://github.com/jodersky")
    )
  )
  def artifactName = "cmdr"
}

object cmdr extends Module {

  class JvmModule(val crossScalaVersion: String) extends CmdrModule {
    def millSourcePath = super.millSourcePath / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-jvm")))
    object test extends Tests with Utest
  }
  object jvm extends Cross[JvmModule]((Seq(scala213, scala3) ++ dottyCustomVersion):_*)

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends CmdrModule
      with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    def sources = T.sources(super.sources() ++ Seq(PathRef(millSourcePath / "src-native")))
    object test extends Tests with Utest
  }
  object native extends Cross[NativeModule]((scala213, "0.4.0"))

}

object examples extends Module {
  class ExampleApp(val crossScalaVersion: String) extends CrossScalaModule {
    def scalaVersion = cmdr.jvm(crossScalaVersion).scalaVersion
    def scalacOptions = cmdr.jvm(crossScalaVersion).scalacOptions
    def moduleDeps = Seq(cmdr.jvm(crossScalaVersion))
    def dist = T {
      val jar = assembly().path
      os.copy.over(jar, os.pwd / millSourcePath.last)
    }
    object test extends Tests {
      def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.10")
      def testFramework = "utest.runner.Framework"
    }
  }
  object `readme-imperative` extends Cross[ExampleApp](scala213, scala3)
  object `readme-declarative` extends Cross[ExampleApp](scala3)
  object commands extends Cross[ExampleApp](scala213, scala3)
}
