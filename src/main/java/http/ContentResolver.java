package http;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Created by Hett on 03.09.2017.
 */
public class ContentResolver {

    private static final Cache<String, ContentResolverResponse> contentLengthCache = CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.MINUTES)
            .build();

    public static ContentResolverResponse get(String url) throws ExecutionException {
        return contentLengthCache.get(url, () -> {
            HttpURLConnection urlConnection = null;
            try {
                URL url_ = new URL(url);
                urlConnection = (HttpURLConnection) url_.openConnection();
                urlConnection.setRequestMethod("HEAD");
                try (InputStream inputStream = urlConnection.getInputStream()) {
                    return new ContentResolverResponse(
                            Long.parseLong(urlConnection.getHeaderField("Content-Length"), 10),
                            urlConnection.getHeaderField("Content-Type")
                    );
                }
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }

}
