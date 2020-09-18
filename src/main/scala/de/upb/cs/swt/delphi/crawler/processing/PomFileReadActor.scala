// Copyright (C) 2018 The Delphi Team.
// See the LICENCE file distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.upb.cs.swt.delphi.crawler.processing

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import de.upb.cs.swt.delphi.crawler.Configuration
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.{ArtifactLicense, IssueManagementData, MavenArtifact, MavenArtifactMetadata, PomFile}
import de.upb.cs.swt.delphi.crawler.tools.HttpDownloader
import org.apache.maven.model.{Dependency, Model}
import org.apache.maven.model.io.xpp3.MavenXpp3Reader

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

/**
  * An Actor that receives MavenArtifacts and extracts metadata from its POM file. If successful, an
  * MavenMetadata object is attached to the artifact and the artifact is returned. If failures occurr,
  * the artifact is returned without metadata.
  *
  * @author Johannes Düsing
  */
class PomFileReadActor(configuration: Configuration) extends Actor with ActorLogging{

  val pomReader: MavenXpp3Reader = new MavenXpp3Reader()
  implicit val system : ActorSystem = context.system

  override def receive: Receive = {
    case artifact@MavenArtifact(identifier, _ ,PomFile(pomStream), _, _) =>

      val pomObject = Try(pomReader.read(pomStream))
      pomStream.close()

      pomObject match {
        case Success(pom) =>

          val issueManagement = if (pom.getIssueManagement != null) {
            Some(IssueManagementData(pom.getIssueManagement.getSystem, pom.getIssueManagement.getUrl))
          } else {
            None
          }

          val dependencies = getDependencies(pom, identifier)

          val metadata = MavenArtifactMetadata(pom.getName,
            pom.getDescription,
            pom.getDevelopers.asScala.map(_.getId).toList,
            pom.getLicenses.asScala.map(l => ArtifactLicense(l.getName, l.getUrl)).toList,
            issueManagement,
            dependencies)

          sender() ! Success(MavenArtifact.withMetadata(artifact, metadata))

        case Failure(ex) =>
          log.error(s"Failed to parse POM file for artifact $identifier",ex )
          // Best effort semantics: If parsing fails, artifact is returned without metadata
          sender() ! artifact
      }

  }

  /**
    * Tries to resolve, download and parse the parent POM file of the given POM.
    * @param pomContent Content of a POM file to resolve parent for
    * @return Content of Parent POM, or None if no parent is specified or an error occurred
    */
  private def getParentPomModel(implicit pomContent: Model): Option[Model] = {
    val parentDef = pomContent.getParent

    if (parentDef != null && parentDef.getGroupId != null && parentDef.getArtifactId != null && parentDef.getVersion != null){
      val parentIdentifier = MavenIdentifier(configuration.mavenRepoBase.toString, parentDef.getGroupId,
        parentDef.getArtifactId, parentDef.getVersion)

      new HttpDownloader().downloadFromUri(parentIdentifier.toPomLocation.toString) match {
        case Success(pomStream) =>
          val parentPom = pomReader.read(pomStream)
          pomStream.close()

          Some(parentPom)
        case Failure(x) =>
          log.error(x, s"Failed to download parent POM")
          None
      }
    }
    else {
      None
    }
  }

  private def buildParentHierarchy(implicit pomContent: Model): List[Model] = {
    getParentPomModel(pomContent) match {
      case Some(parentContent) =>
        List(parentContent) ++ buildParentHierarchy(parentContent)
      case _ =>
        List()
    }
  }

  private def buildParentIdentifier(implicit pomContent:Model): MavenIdentifier = {
    MavenIdentifier(configuration.mavenRepoBase.toString, pomContent.getParent.getGroupId,
      pomContent.getParent.getArtifactId, pomContent.getParent.getVersion)
  }

  /**
    * Retrieve all dependencies specified in the given POM file as MavenIdentifiers. Try to resolve variables as well.
    * Only returns successfully resolved dependencies, omits failures.
    * @param pomContent Object holding POM file contents
    * @param identifier Maven identifier, as sometimes version / groupID is not part of POM file!
    * @return Set of MavenIdentifiers for each successfully parsed dependency
    */
  private def getDependencies(implicit pomContent: Model, identifier: MavenIdentifier): Set[MavenIdentifier] = {

    implicit lazy val parentHierarchy: List[Model] = buildParentHierarchy

    //implicit val parentContent: Option[Model] = getParentPomModel(pomContent)

    val dependencies = pomContent
      .getDependencies
      .asScala
      .toSet[Dependency]
      .map(resolveDependency(_))

    for ( Success(identifier) <- dependencies) yield identifier
  }

  /**
    * Process raw dependency specification from POM file, validate text values and try to resolve project variables.
    * @param dependency Raw dependency specification as given in the POM file
    * @param pomContent Contents of the POM file
    * @param identifier Artifact identifier, as sometimes version / groupID is not part of POM file
    * @return Try object holding the dependency's MavenIdentifier if successful
    */
  private def resolveDependency(dependency: Dependency)
                               (implicit pomContent: Model, identifier: MavenIdentifier, parentHierarchy: List[Model])
  : Try[MavenIdentifier] = {
    Try {
      val groupId = resolveProperty(dependency.getGroupId, "groupID")
      val artifactId = resolveProperty(dependency.getArtifactId, "artifactID")

      // Often dependency versions are left empty, as they are specified in the parent!
      val version: String = if(dependency.getVersion == null && parentHierarchy.nonEmpty){
        // Will recurse parent hierarchy to resolve missing version
        resolveDependencyVersion(dependency, pomContent, identifier)
      } else {
        resolveProperty(dependency.getVersion, "version")
      }

      MavenIdentifier(configuration.mavenRepoBase.toString, groupId, artifactId, version)
    }
  }

  @scala.annotation.tailrec
  private def resolveDependencyVersion(dependency: Dependency, pomContent: Model, identifier: MavenIdentifier, level: Int = 0)
                                      (implicit parentHierarchy: List[Model]): String = {
    if(pomContent.getDependencyManagement != null){
      pomContent
        .getDependencyManagement.getDependencies
        .asScala.toSet[Dependency]
        .filter(d => d.getGroupId.equals(dependency.getGroupId) && d.getArtifactId.equals(dependency.getArtifactId))
        .map(_.getVersion)
        .find(_ != null) match {
        case Some(version) =>
          // Found something, try to resolve it if its a variable
          resolveProperty(version, "version", level)(pomContent, identifier, parentHierarchy)
        case None if level < parentHierarchy.length =>
          // Recursive call to find version definition in upper parent definitions
          resolveDependencyVersion(dependency, parentHierarchy(level), buildParentIdentifier(pomContent), level + 1)
        case None if level >= parentHierarchy.length =>
          // No parent left to recurse, so this really is a dependency without a version
          throw new NullPointerException(s"Version was null and could not be resolved in parent")
      }
    }
    else if(level < parentHierarchy.length) {
      // Recursive call to find version definition in upper parent definitions
      resolveDependencyVersion(dependency, parentHierarchy(level), buildParentIdentifier(pomContent), level + 1)
    }
    else {
      // No parent left to recurse, so this really is a dependency without a version
      throw new NullPointerException(s"Version was null and could not be resolved in parent")
    }


  }

  /**
    * Resolve the given property value of an dependency specification and do input validation
    * @param propValue Value to resolve
    * @param propName Name of the property (for error logging)
    * @param pomContent Contents of the POM file
    * @return Fully resolved string value of the property if successful
    * @throws NullPointerException If a null values was found for a required property
    * @throws RuntimeException If actor failed to resolve a variable inside the POM file
    */
  private def resolveProperty(propValue: String, propName: String, level: Int = 0)
                             (implicit pomContent:Model, identifier:MavenIdentifier, parentHierarchy: List[Model])
  : String = {
    if(propValue == null){
      throw new NullPointerException(s"Property '$propName' must not be null for dependencies")
    }
    else if (propValue.startsWith("$")){
      resolveProjectVariable(propValue, level)
        .getOrElse(throw new RuntimeException(s"Failed to resolve variable '$propValue' for property '$propName'"))
    }
    else {
      propValue
    }
  }

  //noinspection ScalaStyle
  @scala.annotation.tailrec
  private def resolveProjectVariable(variableName: String, level: Int)
                                    (implicit pomContent: Model, identifier: MavenIdentifier, parentHierarchy: List[Model])
  : Option[String] = {
    // Drop Maven Syntax from variable reference (e.g. ${varname})
    val rawVariableName = variableName.drop(2).dropRight(1)

    // Split dot-separated variable names
    val variableParts = rawVariableName.split("\\.", 2)

    var result: Option[String] = None

    // Resolve special references to POM attributes
    if (variableParts(0).equals("project") || variableParts(0).equals("pom")) {
          result = variableParts(1) match {
            // groupID always present in identifier, but not always explicit in POM
            case "groupId" => Some(identifier.groupId)
            // artifactID always present in POM
            case "artifactId" => Some(pomContent.getArtifactId)
            // Version always present in identifier, but not always explicit in POM
            case "version" => Some(identifier.version)
            // Can only extract parent version if explicitly stated
            case "parent.version" if pomContent.getParent != null && pomContent.getParent.getVersion != null =>
              Some(pomContent.getParent.getVersion)
            case _ => None
          }
      }
      else {
          // All other formats are interpreted as POM property names
          result = Option(pomContent.getProperties.getProperty(rawVariableName))
      }

    // If not resolved -> try to resolve in parent!
    if (result.isEmpty && level <= parentHierarchy.length){
      resolveProjectVariable(variableName, level + 1)(parentHierarchy(level), buildParentIdentifier(pomContent), parentHierarchy)
    }
    else {
      result
    }

  }


}

object PomFileReadActor {
  def props(configuration: Configuration):Props = Props(new PomFileReadActor(configuration))
}
