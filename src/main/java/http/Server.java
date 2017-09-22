package http;

import com.Config;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Server {

    private final static Logger logger = LogManager.getLogger(Server.class);

    final int port = 8081;
    final byte[] boundary = "aaf60b3612a0376c".getBytes(); //todo
    final byte[] eol = "\r\n".getBytes();

    public void run() throws IOException {

        HttpServer httpServer = HttpServer.create();
        logger.debug("Run server on port " + port);
        httpServer.bind(new InetSocketAddress(port), 0);
        httpServer.setExecutor(null);

        int readBufferSize = Integer.parseInt(Config.getInstance().getProperty("read_buffer_size_bytes", "1024"));

        httpServer.createContext("/", exchange -> {
            final Headers responseHeaders = exchange.getResponseHeaders();

            try (OutputStream outputStream = exchange.getResponseBody()) {

                final String requestedUrl = exchange.getRequestURI().toString().substring(1);
                final String rangeHeader = exchange.getRequestHeaders().getFirst("Range");
                final ContentResolverResponse contentResolverResponse = ContentResolver.get(requestedUrl);

                Range[] ranges = null;
                if (rangeHeader != null) {
                    ranges = Range.parseRangeHttpHeader(rangeHeader, contentResolverResponse.getContentLength());
                }

                if (ranges == null) {

                    responseHeaders.set("Content-Length", contentResolverResponse.getContentLength().toString());
                    responseHeaders.set("Content-Type", contentResolverResponse.getContentType());

                    if (exchange.getRequestMethod().equals("GET")) {
                        exchange.sendResponseHeaders(200, contentResolverResponse.getContentLength());
                        HttpURLConnection urlConnection = null;
                        try {
                            URL url = new URL(requestedUrl);
                            byte[] chunk = new byte[readBufferSize];
                            // content
                            urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setRequestMethod("GET");
                            int length;
                            while ((length = urlConnection.getInputStream().read(chunk)) > 0) {
                                outputStream.write(chunk, 0, length);
                            }
                        } finally {
                            if (urlConnection != null) {
                                urlConnection.disconnect();
                            }
                        }
                    } else {
//                        assert ("HEAD".equals(exchange.getRequestMethod()));
                        exchange.sendResponseHeaders(200, -1);
                    }

                } else {
                    /*
                        Content-Length: 232
                        Content-Type: multipart/byteranges; boundary=4147906ccaafd9e2


                        --4147906ccaafd9e2
                        Content-type: application/x-rar-compressed
                        Content-range: bytes 0-1/2443138

                        Ra
                        --4147906ccaafd9e2
                        Content-type: application/x-rar-compressed
                        Content-range: bytes 6-7/2443138

                        ╧
                        --4147906ccaafd9e2--
                     */
//                    contentLength += ranges.length * (boundary.length() + 3) + boundary.length() + 4;

                    // creation boundary header
                    List<Headers> boundaryHeaders = new ArrayList<>();
                    long contentLength = 0;
                    for (Range range : ranges) {
                        Headers h = new Headers();
                        h.add("Content-Type", contentResolverResponse.getContentType());
                        h.add("Content-Range", String.format("bytes %d-%d/%d",
                                range.getStart(), range.getEnd(), contentResolverResponse.getContentLength()));
                        boundaryHeaders.add(h);

                        contentLength += eol.length;
                        // Starting boundary. Const 2 is prefix `--`
                        contentLength += 2 + boundary.length;
                        contentLength += eol.length;

                        for (Map.Entry<String, List<String>> header : h.entrySet()) {

                            for (String value : header.getValue()) {
                                // Header name
                                contentLength += header.getKey().length();
                                // Header separator `: `
                                contentLength += 2;
                                // Header value
                                contentLength += value.length();
                                // Header line ending
                                contentLength += eol.length;
                                // ------------------------- DEBUG -----------------------
//                                if (logger.isDebugEnabled()) {
//                                    logger.debug("BOUNDARY HEADER\t\t\t" + header.getKey() + ": " + value);
//                                }
                                // -------------------------------------------------------
                            }
                        }
                        // Content data
                        contentLength += eol.length + range.getContentLength() + eol.length;
                    }
                    // Ending boundary 4 is boundary prefix `--` and boundary suffix `--`
                    contentLength += boundary.length + 4;

                    responseHeaders.set("Content-Length", String.valueOf(contentLength));
                    responseHeaders.set("Accept-Ranges", "ranges");
                    responseHeaders.set("Content-Type", "multipart/byteranges; boundary=" + new String(boundary));
                    responseHeaders.set("Content-Range", "bytes " + Arrays.stream(ranges)
                            .map(p -> p.getStart() + "-" + p.getEnd())
                            .collect(Collectors.joining(",")));
                    exchange.sendResponseHeaders(206, contentLength);
                    outputStream.write(eol);
                    HttpURLConnection urlConnection = null;

                    try {
                        URL url = new URL(requestedUrl);
                        byte[] chunk = new byte[readBufferSize];

                        int index = 0;
                        for (Range range : ranges) {
                            // boundary
                            outputStream.write("--".getBytes());
                            outputStream.write(boundary);
                            outputStream.write(eol);

                            // headers
                            for (Map.Entry<String, List<String>> header : boundaryHeaders.get(index).entrySet()) {
                                for (String value : header.getValue()) {
                                    outputStream.write(header.getKey().getBytes());
                                    outputStream.write(": ".getBytes());
                                    outputStream.write(value.getBytes());
                                    outputStream.write(eol);
                                }
                            }
                            outputStream.write(eol);

                            // content
                            urlConnection = (HttpURLConnection) url.openConnection();
                            urlConnection.setRequestMethod("GET");
                            urlConnection.setRequestProperty("Range",
                                    String.format("bytes=%s-%s", range.getStart(), range.getEnd()));
                            int length;
                            while ((length = urlConnection.getInputStream().read(chunk)) > 0) {
                                outputStream.write(chunk, 0, length);
                            }
//                            urlConnection.disconnect();

                            outputStream.write(eol);
                            index++;
                        }

                        outputStream.write("--".getBytes());
                        outputStream.write(boundary);
                        outputStream.write("--".getBytes());
                        outputStream.write(eol);

//                    if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_PARTIAL) {
//                        //Your code here to read response data
//                    }

                    } finally {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    }

                }

                // ------------------------- DEBUG -----------------------
                if (logger.isDebugEnabled()) {
                    for (Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
                        for (String value : header.getValue()) {
                            logger.debug("HEADER\t\t" + header.getKey() + ": " + value);
                        }
                    }
                }
                // -------------------------------------------------------

            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e);
            }
        });
        httpServer.start();
    }

}
