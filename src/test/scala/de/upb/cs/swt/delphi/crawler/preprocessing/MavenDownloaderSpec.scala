import java.io.{ByteArrayOutputStream, File, InputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers, WordSpecLike}
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloader

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source;

class MavenDownloaderSpec extends FlatSpec with Matchers {
  "MavenDownloader" should "save jar file" in {
    val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
    val downloader = new MavenDownloader(mavenIdentifier)
    val jarStream = downloader.downloadJar()
    val jarBytes = inputStreamToBytes(jarStream)
    val tmpDir = System.getProperty("java.io.tmpdir")
    val jarPath = Paths.get(tmpDir).resolve("junit.jar")
    Files.write(jarPath, jarBytes)
    assert(jarPath.toFile.exists())
    assert(jarPath.toFile.length() > 0)
  }
  "MavenDownloader" should "save pom file" in {
    val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
    val downloader = new MavenDownloader(mavenIdentifier)
    val pomStream = downloader.downloadPom()
    val pomBytes = inputStreamToBytes(pomStream)
    val tmpDir = System.getProperty("java.io.tmpdir")
    val pomPath = Paths.get(tmpDir).resolve("pom.xml")
    Files.write(pomPath, pomBytes)
    assert(pomPath.toFile.exists())
    assert(pomPath.toFile.length() > 0)
  }

  def inputStreamToBytes(stream: InputStream): Array[Byte] = {
    val buffer = new ByteArrayOutputStream()
    val bytes = new Array[Byte](4096)
    var len: Int = 0
    while ( {
      len = stream.read(bytes);
      len != -1
    }) {
      buffer.write(bytes, 0, len)
    }
    buffer.flush()
    buffer.toByteArray
  }


}