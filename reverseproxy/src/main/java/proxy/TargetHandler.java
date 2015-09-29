package proxy;


import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BufferedHeader;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.NHttpClientEventHandler;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SharedInputBuffer;
import org.apache.http.nio.util.SharedOutputBuffer;

import java.io.IOException;

public class TargetHandler implements NHttpClientEventHandler {

    private int bufferSize;

    public TargetHandler(int bufferSize) {
        this.bufferSize = bufferSize;

    }

    @Override
    public void connected(NHttpClientConnection nHttpClientConnection, Object o) throws IOException, HttpException {
        if (o != null && o instanceof Message) {
            Message message = (Message) o;
            message.setDefaultNHttpClientConnection(nHttpClientConnection);
            nHttpClientConnection.getContext().setAttribute(Constants.MESSAGE, message);
            nHttpClientConnection.requestOutput();

        }

    }

    @Override
    public void requestReady(NHttpClientConnection nHttpClientConnection) throws IOException, HttpException {
        Message message = (Message) nHttpClientConnection.getContext().getAttribute(Constants.MESSAGE);
        HttpRequest httpRequest = message.getHttpRequest();
        httpRequest.removeHeaders("Connection");
        httpRequest.addHeader("Connection","Close");
        nHttpClientConnection.setSocketTimeout(message.getTimeout());
        nHttpClientConnection.submitRequest(httpRequest);
        Pipe pipe = message.getRequestPipe();
        pipe.attachConsumer(nHttpClientConnection);

    }

    @Override
    public void responseReceived(NHttpClientConnection nHttpClientConnection) throws IOException, HttpException {

        Message message = (Message) nHttpClientConnection.getContext().getAttribute(Constants.MESSAGE);
        HttpResponse httpResponse = nHttpClientConnection.getHttpResponse();
//        httpResponse.removeHeaders("Connection");
//        httpResponse.addHeader("Connection","Keep-Alive");
        message.setHttpResponse(httpResponse);
        NHttpServerConnection nHttpServerConnection = message.getDefaultNHttpServerConnection();
        nHttpServerConnection.getContext().setAttribute(Constants.MESSAGE, message);
        Pipe pipe = new Pipe(nHttpClientConnection, new HeapByteBufferAllocator(), bufferSize);
        message.setResponsePipe(pipe);
        nHttpServerConnection.requestOutput();
    }

    @Override
    public void inputReady(NHttpClientConnection nHttpClientConnection, ContentDecoder contentDecoder)
               throws IOException, HttpException {
        Message message = (Message) nHttpClientConnection.getContext().getAttribute(Constants.MESSAGE);
        Pipe pipe = message.getResponsePipe();
        pipe.produce(contentDecoder);
        if (contentDecoder.isCompleted()) {
            nHttpClientConnection.close();
        }

    }

    @Override
    public void outputReady(NHttpClientConnection nHttpClientConnection, ContentEncoder contentEncoder)
               throws IOException, HttpException {

        Message message = (Message) nHttpClientConnection.getContext().getAttribute(Constants.MESSAGE);
        Pipe pipe = message.getRequestPipe();
        pipe.consume(contentEncoder);


    }

    @Override
    public void endOfInput(NHttpClientConnection nHttpClientConnection) throws IOException {
       // nHttpClientConnection.close();
    }

    @Override
    public void timeout(NHttpClientConnection nHttpClientConnection) throws IOException, HttpException {
    //    nHttpClientConnection.close();
    }

    @Override
    public void closed(NHttpClientConnection nHttpClientConnection) {
        try {
            nHttpClientConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exception(NHttpClientConnection nHttpClientConnection, Exception e) {
        try {
            nHttpClientConnection.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
