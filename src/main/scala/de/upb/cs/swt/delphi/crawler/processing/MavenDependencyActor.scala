package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{MavenDownloader, PomFile}
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.versioning.ComparableVersion
import org.apache.maven.model.{Dependency, Model}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._
import scala.util.Try

/*
 * This class extracts the dependencies for a project from a POM file.
 * It uses Maven's Xpp3 to read XML files, and validates the coordinates extracted against Maven Central
 * metadata to ensure that the coordinates are valid.
 *
 * Currently, it resolves project-level variables and soft version requirements.
 * Any more exotic formats are seen as invalid and dropped.
 */

class MavenDependencyActor(configuration: Configuration) extends Actor with ActorLogging {
  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()
  val metaReader: MetadataXpp3Reader = new MetadataXpp3Reader()

  override def receive: Receive = {
    case pf: PomFile =>
      Try(getDependencies(pf)) match {
        case util.Success(dx) => sender() ! dx
        case util.Failure(e) =>{
          log.warning("Dependency mapper threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
      }
  }

  def resolveProperty(str: String, pomObj: Model): Option[String] = {
    if (str == null || !str.startsWith("$")){
      Option(str)
    } else {
      val extractedVar = str.drop(2).dropRight(1)
      val splitVar = extractedVar.split("\\.", 2)
      if (splitVar(0) == "project" || splitVar(0) == "pom") {
        val evaluatedVar = splitVar(1) match {
          case "groupId" => Some(pomObj.getGroupId)
          case "artifactId" => Some(pomObj.getArtifactId)
          case "version" => Some(pomObj.getVersion)
          case _ => None
        }
        evaluatedVar
      } else {
        val evaluatedVar = pomObj.getProperties.getProperty(extractedVar)
        Option(evaluatedVar)
      }
    }
  }

  def resolveIdentifier(d: Dependency, pomObj: Model): Try[MavenIdentifier] = {
    Try {
      val groupId = resolveProperty(d.getGroupId, pomObj).getOrElse(throw new Exception)
      val artifactId = resolveProperty(d.getArtifactId, pomObj).getOrElse(throw new Exception)
      val versionSpec = resolveProperty(d.getVersion, pomObj).getOrElse(throw new Exception)

      val tempId = MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, versionSpec)

      val downloader = new MavenDownloader(tempId)
      val metaFile = downloader.downloadMeta()
      val metaObj = metaReader.read(metaFile.is)
      metaFile.is.close()
      val versionList = metaObj.getVersioning.getVersions.asScala
      if (versionList.contains(versionSpec) || versionSpec == null) {
        tempId
      } else {
        val versionComp = new ComparableVersion(versionSpec)
        val versionNum = versionList.indexWhere(v => new ComparableVersion(v).compareTo(versionComp) >= 0)
        val version = versionNum match {
          case -1 => versionList.lastOption
          case 0 => versionList.headOption
          case _ => Option(versionList(versionNum - 1))
        }
        version match {
          case Some(v) =>
            MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, v)
          case None =>
            throw new Exception
        }
      }
    }
  }

  def getDependencies(pomFile: PomFile): Set[MavenIdentifier] = {
    val pomObj = pomReader.read(pomFile.is)
    pomFile.is.close()

    val pomSet = pomObj.getDependencies.asScala.toSet[Dependency].map(resolveIdentifier(_, pomObj))
    for (util.Success(id) <- pomSet) yield id
  }
}

object MavenDependencyActor {
  def props(configuration: Configuration) = Props(new MavenDependencyActor(configuration))
}