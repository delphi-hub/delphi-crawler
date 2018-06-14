import de.upb.cs.swt.delphi.crawler.discovery.maven.MavenIdentifier
import de.upb.cs.swt.delphi.crawler.preprocessing.MavenDownloader
import org.scalatest.{FlatSpec, Matchers}
import de.upb.cs.swt.delphi.crawler.preprocessing.Common._
class MavenDownloaderSpec extends FlatSpec with Matchers {
  "MavenDownloader" should "save jar file" in {
    val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
    val downloader = new MavenDownloader(mavenIdentifier)
    val jarStream = downloader.downloadJar()
    checkJar(jarStream.is)
  }
  "MavenDownloader" should "save pom file" in {
    val mavenIdentifier = new MavenIdentifier("http://central.maven.org/maven2/", "junit", "junit", "4.12")
    val downloader = new MavenDownloader(mavenIdentifier)
    val pomStream = downloader.downloadPom()
    checkPom(pomStream.is)
  }

}
