package de.sldk.mc;

import org.eclipse.jetty.compression.gzip.GzipCompression;
import org.eclipse.jetty.compression.server.CompressionHandler;
import org.eclipse.jetty.http.pathmap.PathSpec;
import de.sldk.mc.health.HealthChecks;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.PathMappingsHandler;

import java.net.InetSocketAddress;

public class MetricsServer {

    private final String host;
    private final int port;
    private final PrometheusExporter prometheusExporter;
	private final HealthChecks healthChecks;

    private Server server;

    public MetricsServer(String host, int port, PrometheusExporter prometheusExporter, HealthChecks healthChecks) {
        this.host = host;
        this.port = port;
        this.prometheusExporter = prometheusExporter;
    this.healthChecks = healthChecks;
	}

    public void start() throws Exception {
        var pathMappings = new PathMappingsHandler();
        pathMappings.addMapping(PathSpec.from("/metrics"), MetricsController.create(prometheusExporter));
        pathMappings.addMapping(PathSpec.from("/health"), HealthController.create(healthChecks));

        // CompressionHandler finds compressions through the context class loader. In a plugin, that is the server's
        // class loader, which does not see the plugin's classes.
        var compressionHandler = new CompressionHandler(pathMappings);
        compressionHandler.putCompression(new GzipCompression());

        InetSocketAddress address = new InetSocketAddress(host, port);
        server = new Server(address);
        server.setHandler(compressionHandler);

        server.start();
    }

    public void stop() throws Exception {
        if (server == null) {
            return;
        }

        server.stop();
    }
}
