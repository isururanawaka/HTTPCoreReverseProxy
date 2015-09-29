package proxy;


import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.util.SharedInputBuffer;
import org.apache.http.nio.util.SharedOutputBuffer;

public class Message {

   private NHttpServerConnection defaultNHttpServerConnection;



    private NHttpClientConnection defaultNHttpClientConnection;


    private HttpRequest httpRequest;

    private HttpResponse httpResponse;

    private Pipe requestPipe;

    private Pipe ResponsePipe;

    public Pipe getRequestPipe() {
        return requestPipe;
    }

    public void setRequestPipe(Pipe requestPipe) {
        this.requestPipe = requestPipe;
    }

    public Pipe getResponsePipe() {
        return ResponsePipe;
    }

    public void setResponsePipe(Pipe responsePipe) {
        ResponsePipe = responsePipe;
    }

    private int timeout;

    public NHttpServerConnection getDefaultNHttpServerConnection() {
        return defaultNHttpServerConnection;
    }

    public void setDefaultNHttpServerConnection(NHttpServerConnection defaultNHttpServerConnection) {
        this.defaultNHttpServerConnection = defaultNHttpServerConnection;
    }

    public NHttpClientConnection getDefaultNHttpClientConnection() {
        return defaultNHttpClientConnection;
    }

    public void setDefaultNHttpClientConnection(NHttpClientConnection defaultNHttpClientConnection) {
        this.defaultNHttpClientConnection = defaultNHttpClientConnection;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public HttpResponse getHttpResponse() {

        return httpResponse;
    }

    public void setHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }


}
