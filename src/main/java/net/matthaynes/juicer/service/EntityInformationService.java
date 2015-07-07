package net.matthaynes.juicer.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.matthaynes.juicer.Properties;
import net.matthaynes.juicer.service.EntityInformationService.DbpediaJsonResult.DbpediaResults.DbpediaBinding;

public class EntityInformationService {

	private final Gson gson;

	public EntityInformationService() {
		GsonBuilder gsonBuilder = new GsonBuilder();
		this.gson = gsonBuilder.create();
	}

	@Nonnull
	public Entity information(@Nonnull String entityName) throws UnsupportedEncodingException {
		String dbPediaUrl = getDbPediaUrl(entityName);
		String html = getHtml(dbPediaUrl);
		List<DbpediaBinding> results = gson.fromJson(html, DbpediaJsonResult.class).results.bindings;
		if (results.isEmpty()) {
			return new Entity(entityName, null);
		}

		String description = results.iterator().next().wikipedia_data_field_abstract.value;
		return new Entity(entityName, description);

	}

	private String getDbPediaUrl(String entityName) throws UnsupportedEncodingException {
		return "http://dbpedia.org/sparql?output=json&default-graph-uri=http%3A%2F%2Fdbpedia.org&query=PREFIX%20dbpedia-owl%3A%20%3Chttp%3A%2F%2Fdbpedia.org%2Fontology%2F%3E%0A%20%20%20%20SELECT%20%3Fwikipedia_data_field_name%20%3Fwikipedia_data_field_abstract%0A%20%20%20%20WHERE%20%7B%0A%20%20%20%20%20%20%20%20%3Fwikipedia_data%20foaf%3Aname%20%22"
				+ URLEncoder.encode(entityName, "UTF-8")
				+ "%22%40en%3B%20foaf%3Aname%20%0A%20%20%20%20%20%20%20%20%3Fwikipedia_data_field_name%3B%20dbpedia-owl%3Aabstract%20%3Fwikipedia_data_field_abstract.%0A%20%20%20%20%20%20%20%20FILTER%20langMatches(lang(%3Fwikipedia_data_field_abstract)%2C%27en%27)%0A%20%20%20%20%20%20%7D&endpoint=/sparql&maxrows=50&timeout=&default-graph-uri=http://dbpedia.org&view=1&raw_iris=true";
	}

	@CheckForNull
	private String getHtml(String address) {
		try {
			URL url = new URL(address);

			final URLConnection urlConnection;
			if (Properties.PROXY_HOST != null) {
				Proxy proxy = new Proxy(Proxy.Type.HTTP,
						new InetSocketAddress(Properties.PROXY_HOST, Properties.PROXY_PORT));
				urlConnection = url.openConnection(proxy);
			} else {
				urlConnection = url.openConnection();
			}

			return IOUtils.toString(urlConnection.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	class DbpediaJsonResult {
		DbpediaResults results;

		class DbpediaResults {
			List<DbpediaBinding> bindings;

			class DbpediaBinding {
				DbpediaResult wikipedia_data_field_abstract;

				class DbpediaResult {
					String type;
					String value;
				}
			}
		}
	}

	public class Entity {

		@Nonnull
		String name;

		@Nonnull
		String description;

		public Entity(@Nonnull String name, @Nonnull String description) {
			this.name = name;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public String getDescription() {
			return description;
		}
	}
}
