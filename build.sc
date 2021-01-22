import mill._, scalalib._, scalanativelib._, publish._, scalafmt._

val scala213 = "2.13.3"
val scala3 = "3.0.0-M2"

trait Utest extends ScalaModule with TestModule {
  def ivyDeps = Agg(ivy"com.lihaoyi::utest::0.7.5")
  def testFrameworks = Seq("utest.runner.Framework")
}
trait CmdrModule
    extends CrossScalaModule
    with ScalafmtModule
    with PublishModule {

  def scalacOptions = Seq("-target:jvm-1.8", "-deprecation")

  def ivyDeps = Agg(ivy"com.lihaoyi::os-lib::0.7.1")

  def publishVersion = "0.5.1"
  def pomSettings = PomSettings(
    description = "cmdr",
    organization = "io.crashbox",
    url = "https://github.com/jodersky/cmdr",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("jodersky", "cmdr"),
    developers = Seq(
      Developer("jodersky", "Jakob Odersky", "https://githhub.com/jodersky")
    )
  )
  def artifactName = "cmdr"
  def sources = if (crossScalaVersion.startsWith("2.11")) {
    T.sources{
      super.sources() ++
      Seq(PathRef(millSourcePath / s"src-2.11"))
    }
  } else {
    T.sources{
      super.sources() ++
      Seq(PathRef(millSourcePath / s"src-post-2.11"))
    }
  }
}

object cmdr extends Module {

  class JvmModule(val crossScalaVersion: String) extends CmdrModule {
    def millSourcePath = super.millSourcePath / os.up
    object test extends Tests with Utest
  }
  object jvm extends Cross[JvmModule](scala213, scala3)

  class NativeModule(val crossScalaVersion: String, val crossScalaNativeVersion: String)
      extends CmdrModule
      with ScalaNativeModule {
    def scalaNativeVersion = crossScalaNativeVersion
    def millSourcePath = super.millSourcePath / os.up / os.up
    object test extends Tests with Utest
  }
  object native extends Cross[NativeModule](("2.11.12", "0.4.0-M2"))

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
      def ivyDeps = Agg(ivy"com.lihaoyi::utest:0.7.5")
      def testFrameworks = Seq("utest.runner.Framework")
    }
  }
  //object annotation extends Cross[ExampleApp](scala213)
  object readme extends Cross[ExampleApp](scala213, scala3)
  object commands extends Cross[ExampleApp](scala213, scala3)
}
