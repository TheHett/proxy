package http;

/**
 * Created by Hett on 23.09.2017.
 */
public class ContentResolverResponse {

    private Long contentLength;
    private String contentType;

    ContentResolverResponse(Long contentLength, String contentType) {
        this.contentLength = contentLength;
        this.contentType = contentType;
    }


    public Long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }
}
