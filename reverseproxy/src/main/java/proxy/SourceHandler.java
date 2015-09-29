package proxy;


import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.NHttpServerConnection;
import org.apache.http.nio.NHttpServerEventHandler;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.nio.util.SharedOutputBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class SourceHandler implements NHttpServerEventHandler {

    private ConnectingIOReactor connectingIOReactor;

    private InetSocketAddress remoteAddress;

    private int bufferSize;

    private int timeOut;


    public SourceHandler(ConnectingIOReactor connectingIOReactor, InetSocketAddress remoteAddress, int bufferSize,
                         int timeOut) {

        this.connectingIOReactor = connectingIOReactor;
        this.remoteAddress = remoteAddress;
        this.bufferSize = bufferSize;
        this.timeOut = timeOut;
    }

    @Override
    public void connected(NHttpServerConnection nHttpServerConnection) throws IOException, HttpException {


    }

    @Override
    public void requestReceived(NHttpServerConnection nHttpServerConnection) throws IOException, HttpException {
        Message message = new Message();
        message.setDefaultNHttpServerConnection(nHttpServerConnection);
        message.setHttpRequest(nHttpServerConnection.getHttpRequest());
        Pipe pipe = new Pipe(nHttpServerConnection, new HeapByteBufferAllocator(), bufferSize);
        nHttpServerConnection.getContext().setAttribute(Constants.MESSAGE, message);
        message.setTimeout(timeOut);
        message.setRequestPipe(pipe);
        connectingIOReactor.connect(remoteAddress, null, message, new ConnectCallBack());
    }

    @Override
    public void inputReady(NHttpServerConnection nHttpServerConnection, ContentDecoder contentDecoder)
               throws IOException, HttpException {
        Message message = (Message) nHttpServerConnection.getContext().getAttribute(Constants.MESSAGE);
        Pipe pipe = message.getRequestPipe();
        pipe.produce(contentDecoder);

    }

    @Override
    public void responseReady(NHttpServerConnection nHttpServerConnection) throws IOException, HttpException {
        Message message = (Message) nHttpServerConnection.getContext().getAttribute(Constants.MESSAGE);
        HttpResponse httpResponse = message.getHttpResponse();
        Pipe pipe = message.getResponsePipe();
        pipe.attachConsumer(nHttpServerConnection);
        nHttpServerConnection.submitResponse(httpResponse);

    }

    @Override
    public void outputReady(NHttpServerConnection nHttpServerConnection, ContentEncoder contentEncoder)
               throws IOException, HttpException {
        Message message = (Message) nHttpServerConnection.getContext().getAttribute(Constants.MESSAGE);
       Pipe pipe = message.getResponsePipe();
        pipe.consume(contentEncoder);
        if(contentEncoder.isCompleted()){
            nHttpServerConnection.close();
        }



    }

    @Override
    public void endOfInput(NHttpServerConnection nHttpServerConnection) throws IOException {
    nHttpServerConnection.close();

    }

    @Override
    public void timeout(NHttpServerConnection nHttpServerConnection) throws IOException {

           nHttpServerConnection.close();
    }

    @Override
    public void closed(NHttpServerConnection nHttpServerConnection) {

        try {
            nHttpServerConnection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exception(NHttpServerConnection nHttpServerConnection, Exception e) {
        try {
            nHttpServerConnection.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }
}
