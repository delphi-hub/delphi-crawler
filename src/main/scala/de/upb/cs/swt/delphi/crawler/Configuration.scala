package de.upb.cs.swt.delphi.crawler

import java.net.URI
import com.sksamuel.elastic4s.ElasticsearchClientUri


class Configuration(val elasticsearchClientUri: ElasticsearchClientUri = ElasticsearchClientUri("localhost", 9200),
                    val mavenRepoBase: URI = new URI("http://localhost:8881/maven2/"),
                    val controlServerPort : Int = 8882)  {

}
