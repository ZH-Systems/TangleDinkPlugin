package tccrewplugin.dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import tccrewplugin.util.TextSanitizer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class LocalMockServer implements AutoCloseable
{
	private final HttpServer server;
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private final AtomicReference<String> lastManifestRequest = new AtomicReference<>();
	private final AtomicReference<String> lastSubmission = new AtomicReference<>();
	private final AtomicReference<String> lastWebhook = new AtomicReference<>();
	private final AtomicInteger manifestStatus = new AtomicInteger(200);
	private final AtomicInteger submitStatus = new AtomicInteger(200);
	private final AtomicInteger webhookStatus = new AtomicInteger(200);

	private LocalMockServer(HttpServer server)
	{
		this.server = server;
		this.server.setExecutor(executor);
	}

	public static LocalMockServer start(int port) throws IOException
	{
		HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
		LocalMockServer fixture = new LocalMockServer(server);
		server.createContext("/api/sync/manifest", fixture.new ManifestHandler());
		server.createContext("/api/sync/submit", fixture.new SubmitHandler());
		server.createContext("/api/clan/webhook", fixture.new WebhookHandler());
		server.start();
		return fixture;
	}

	public String manifestJson()
	{
		return "{\"version\":1,\"varbits\":[4101,4102],\"varps\":[1,2],\"collectionLogItems\":[11832,11834]}";
	}

	public void setManifestStatus(int status)
	{
		manifestStatus.set(status);
	}

	public void setSubmitStatus(int status)
	{
		submitStatus.set(status);
	}

	public void setWebhookStatus(int status)
	{
		webhookStatus.set(status);
	}

	public String getLastManifestRequest()
	{
		return lastManifestRequest.get();
	}

	public String getLastSubmission()
	{
		return lastSubmission.get();
	}

	public String getLastWebhook()
	{
		return lastWebhook.get();
	}

	private final class ManifestHandler implements HttpHandler
	{
		@Override
		public void handle(HttpExchange exchange) throws IOException
		{
			if (!"GET".equalsIgnoreCase(exchange.getRequestMethod()))
			{
				send(exchange, 405, "");
				return;
			}
			lastManifestRequest.set(exchange.getRequestURI().toString());
			send(exchange, manifestStatus.get(), manifestJson());
		}
	}

	private final class SubmitHandler implements HttpHandler
	{
		@Override
		public void handle(HttpExchange exchange) throws IOException
		{
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			lastSubmission.set(sanitize(body));
			if (!"Bearer dev-token".equals(exchange.getRequestHeaders().getFirst("Authorization")))
			{
				send(exchange, 401, "{\"error\":\"invalid token\"}");
				return;
			}
			send(exchange, submitStatus.get(), "{\"ok\":true}");
		}
	}

	private final class WebhookHandler implements HttpHandler
	{
		@Override
		public void handle(HttpExchange exchange) throws IOException
		{
			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			lastWebhook.set(sanitize(body));
			if (!"Bearer dev-secret".equals(exchange.getRequestHeaders().getFirst("Authorization"))
				&& !"dev-secret".equals(exchange.getRequestHeaders().getFirst("X-Clan-Webhook-Secret")))
			{
				send(exchange, 401, "{\"error\":\"invalid secret\"}");
				return;
			}
			send(exchange, webhookStatus.get(), "{\"ok\":true}");
		}
	}

	private void send(HttpExchange exchange, int status, String body) throws IOException
	{
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream outputStream = exchange.getResponseBody())
		{
			outputStream.write(bytes);
		}
	}

	private String sanitize(String body)
	{
		return TextSanitizer.redactUrls(TextSanitizer.stripTags(body));
	}

	@Override
	public void close()
	{
		server.stop(0);
		executor.shutdownNow();
	}
}
