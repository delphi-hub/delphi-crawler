package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{JarFile, MavenDownloader, PomFile}
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader
import org.apache.maven.artifact.versioning.ComparableVersion
import org.apache.maven.model.Dependency
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._

class MavenDependencyActor(configuration: Configuration) extends Actor with ActorLogging {
  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()
  val metaReader: MetadataXpp3Reader = new MetadataXpp3Reader()

  override def receive: Receive = {
    case pf: PomFile =>
      try {
        sender() ! getDependencies(pf)
      } catch {
        case e: Exception =>{
          log.warning("Dependency mapper threw exception " + e)
          sender() ! akka.actor.Status.Failure(e)
        }
      }
  }

  def getDependencies(pomFile: PomFile): Set[MavenIdentifier] = {
    val pomObj = pomReader.read(pomFile.is)
    pomFile.is.close()

    def resolveProperty(str: String) = {
      if(str == null || !str.startsWith("$")) {
        str
      } else {
        val extractedVar = str.drop(2).dropRight(1)
        val splitVar = extractedVar.split("\\.", 2)
        if(splitVar(0) == "project" || splitVar(0) == "pom"){
          val evaluatedVar = splitVar(1) match {
            case "groupId" => pomObj.getGroupId
            case "artifactId" => pomObj.getArtifactId
            case "version" => pomObj.getVersion
            case _ => null
          }
          evaluatedVar
        } else {
          val evaluatedVar = pomObj.getProperties.getProperty(extractedVar)
          evaluatedVar
        }
      }
    }

    def resolveIdentifier(d: Dependency): MavenIdentifier = {
      val groupId = resolveProperty(d.getGroupId)
      val artifactId = resolveProperty(d.getArtifactId)

      if (groupId == null || artifactId == null) {
        MavenIdentifier(null, null, null, null)
      } else {
        val versionSpec = resolveProperty(d.getVersion)
        val tempId = MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, versionSpec)

        try {
          val downloader = new MavenDownloader(tempId)
          val metaFile = downloader.downloadMeta
          val metaObj = metaReader.read(metaFile.is)
          metaFile.is.close
          val versionList = metaObj.getVersioning.getVersions.asScala
          if (versionList.contains(versionSpec) || versionSpec == null) {
            tempId
          } else {
            val versionComp = new ComparableVersion(versionSpec)
            val versionNum = versionList.indexWhere(v => new ComparableVersion(v).compareTo(versionComp) >= 0)
            val version = if (versionNum == -1) versionList.last else if (versionNum == 0) versionList.head else versionList(versionNum - 1)
            MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, version)
          }
        } catch {
          case e: java.io.FileNotFoundException => {
            log.warning("Dependency {}:{} does not exist", groupId, artifactId)
            MavenIdentifier(null, null, null, null)
          }
        }
      }
    }

    val pomSet = pomObj.getDependencies()
      .asScala.toSet[Dependency].map(resolveIdentifier).filter(
      m => !(m.version == null || m.groupId == null || m.artifactId == null)
    )
    pomSet
  }
}

object MavenDependencyActor {
  def props(configuration: Configuration) = Props(new MavenDependencyActor(configuration))
}