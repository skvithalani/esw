import java.io.File
import java.nio.file.Files

import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport.{Universal, stage}
import com.typesafe.sbt.packager.universal.ZipHelper
import ohnosequences.sbt.GithubRelease.keys.{ghreleaseAssets, ghreleaseRepoName, ghreleaseRepoOrg, githubRelease}
import ohnosequences.sbt.SbtGithubReleasePlugin
import sbt.Keys._
import sbt.io.{IO, Path}
import sbt.{AutoPlugin, Def, Plugins, ProjectReference, ScopeFilter, Task, ThisProject, librarymanagement, _}

import scala.collection.JavaConverters._

object GithubRelease extends AutoPlugin {
  val coverageReportZipKey = taskKey[File]("Creates a distributable zip file containing the coverage report.")
  val testReportsKey       = taskKey[(File, File)]("Creates test reports in html and zip format.")

  val aggregateFilter = ScopeFilter(inAggregates(ThisProject), inConfigurations(librarymanagement.Configurations.Compile))

  override def requires: Plugins = SbtGithubReleasePlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    ghreleaseRepoOrg := "tmtsoftware",
    ghreleaseRepoName := EswKeys.projectName,
    aggregate in githubRelease := false,
    // this creates scoverage report zip file and required for GithubRelease task, it assumes that scoverage-report is already generated
    // and is available inside target folder (if it is not present, empty zip will be created)
    coverageReportZipKey := coverageReportZipTask.value,
    testReportsKey := testReportsTask.value
  )

  private def coverageReportZipTask = Def.task {
    lazy val coverageReportZip = new File(target.value / "ghrelease", "scoverage-report.zip")
    IO.zip(Path.allSubpaths(new File(crossTarget.value, "scoverage-report")), coverageReportZip)
    coverageReportZip
  }

  private def testReportsTask = Def.task {
    val log = sLog.value

    lazy val testReportZip = target.value / "ghrelease" / "test-reports.zip"
    val testReportHtml     = target.value / "ghrelease" / "test-reports.html"
    val xmlFiles           = target.all(aggregateFilter).value.flatMap(targetPath => Path.allSubpaths(targetPath / "test-reports"))

    // 1. include all xml files in single zip
    IO.zip(xmlFiles, testReportZip)
    // 2. generate html report from xml files
    IO.withTemporaryDirectory { dir =>
      // copy xml files from all projects to single directory
      xmlFiles.foreach { case (file, fileName) => Files.copy(file.toPath, (dir / fileName).toPath) }

      // 2.1 create single xml file by merging all xml's
      val xmlFilesDir     = dir.getAbsolutePath
      val mergedXmlReport = s"$xmlFilesDir/test-report.xml"
      log.info(s"Merging all xml files from dir: $xmlFilesDir using junit-merge command.")
      junitMergeCmd(xmlFilesDir, mergedXmlReport)

      // 2.2 create html test report from merged xml
      val htmlReportPath = testReportHtml.getAbsolutePath
      log.info(s"Generating HTML report at path: $htmlReportPath using junit-viewer command.")
      junitViewerCmd(mergedXmlReport, htmlReportPath)
    }
    (testReportZip, testReportHtml)
  }

  private def junitMergeCmd(inputPath: String, outputPath: String) = {
    val commandWithArgs = List("junit-merge", "-d", inputPath, "-o", outputPath)
    new ProcessBuilder(commandWithArgs.asJava).inheritIO.start.waitFor
  }

  private def junitViewerCmd(inputPath: String, outputPath: String) = {
    val commandWithArgs = List("junit-viewer", s"--results=$inputPath", s"--save=$outputPath")
    new ProcessBuilder(commandWithArgs.asJava).inheritIO.start.waitFor
  }

  private def stageAndZipTask(projects: Seq[ProjectReference]): Def.Initialize[Task[File]] = Def.task {
    val ghrleaseDir = target.value / "ghrelease"
    val log         = sLog.value
    val zipFileName = s"esw-apps-${version.value}"

    lazy val appsZip = new File(ghrleaseDir, s"$zipFileName.zip")

    log.info("Deleting staging directory ...")
    // delete older files from staging directory to avoid getting it included in zip
    // in order to delete directory first and then stage projects, below needs to be a task
    Def.task {
      IO.delete(target.value / "universal" / "stage")
    }.value

    log.info(s"Staging projects: [${projects.mkString(" ,")}]")
    val stagedFiles = projects
      .map(p => stage in Universal in p)
      .join
      .value
      .flatMap(x => Path.allSubpaths(x))
      .distinct
      .map {
        case (source, dest) => (source, s"$zipFileName/$dest")
      }

    ZipHelper.zipNative(stagedFiles, appsZip)
    appsZip
  }

  def githubReleases(projects: Seq[ProjectReference]): Setting[Task[Seq[sbt.File]]] =
    ghreleaseAssets := {
      val (testReportZip, testReportHtml) = testReportsKey.value
      Seq(
        stageAndZipTask(projects).value,
        coverageReportZipKey.value,
        testReportZip,
        testReportHtml
      )
    }
}
