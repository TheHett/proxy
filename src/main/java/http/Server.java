package http;

import com.Config;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Server {

    private final static Logger logger = LogManager.getLogger(Server.class);

    public void run() throws IOException {

        HttpServer httpServer = HttpServer.create();
        httpServer.bind(new InetSocketAddress(8081), 0);
        httpServer.setExecutor(null);

        int readBufferSize = Integer.parseInt(Config.getInstance().getProperty("read_buffer_size_bytes", "1024"));

        httpServer.createContext("/", httpExchange -> {
            try {
                URL url = new URL(httpExchange.getRequestURI().toString().substring(1));
                URLConnection urlConnection = url.openConnection();

                

                byte[] chunk = new byte[readBufferSize];
                int length;
                while ((length = urlConnection.getInputStream().read(chunk)) > 0) {

                }
            } catch (Exception e) {
                logger.error(e);
            }
        });
        httpServer.start();
    }

}
