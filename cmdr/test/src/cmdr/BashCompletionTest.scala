package cmdr

import utest._

object BashCompletionTest extends TestSuite {

  class CompletionReporter extends ArgParser.Reporter {
    val data = new java.io.ByteArrayOutputStream
    override val stdout = new java.io.PrintStream(data)

    def completions = data.toString("utf-8").split("\n").toList
  }

  class CompletionParser(
      line: String,
      override val reporter: CompletionReporter = new CompletionReporter()
  ) extends ArgParser(
        "",
        "",
        "",
        reporter,
        Map(
          "COMP_LINE" -> line,
          "COMP_POINT" -> line.length.toString()
        )
      )

  val tests = Tests {
    test("basic") {
      val parser = new CompletionParser("cmd")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions.isEmpty
    }
    test("named single") {
      val parser = new CompletionParser("cmd -")
      parser.requiredParam[String]("--a")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("--help ", "--a ")
    }
    test("named multiple") {
      val parser = new CompletionParser("cmd -")
      parser.requiredParam[String]("--a")
      parser.requiredParam[String]("--b")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("--help ", "--a ", "--b ")
    }
    test("named multiple partial match 1") {
      val parser = new CompletionParser("cmd --opt")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("--option ", "--opt ", "--optimize ")
    }
    test("named multiple partial match 2") {
      val parser = new CompletionParser("cmd --opti")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("--option ", "--optimize ")
    }
    test("named multiple partial match 3") {
      val parser = new CompletionParser("cmd --optim")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("--optimize ")
    }
    test("named multiple full match") {
      val parser = new CompletionParser("cmd --optimize")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String]("--optimize")
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("--optimize ")
    }
    test("arg completions") {
      val parser = new CompletionParser("cmd --optimize ")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String](
        "--optimize",
        completer =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("L1 ", "L2 ", "L11 ")
    }
    test("arg completions partial") {
      val parser = new CompletionParser("cmd --optimize L1")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String](
        "--optimize",
        completer =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("L1 ", "L11 ")
    }
    test("arg completions partial embedded") {
      val parser = new CompletionParser("cmd --optimize=L1")
      parser.requiredParam[String]("--option")
      parser.requiredParam[String]("--opt")
      parser.requiredParam[String](
        "--optimize",
        completer =
          prefix => Seq("L1 ", "L2 ", "L11 ").filter(_.startsWith(prefix))
      )
      parser.parseResult(Seq()) ==> ArgParser.EarlyExit
      parser.reporter.completions ==> List("L1 ", "L11 ")
    }
  }

}
