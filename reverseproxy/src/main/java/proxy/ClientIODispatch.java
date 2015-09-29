package proxy;


import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.impl.nio.reactor.AbstractIODispatch;
import org.apache.http.nio.reactor.IOSession;

import java.io.IOException;

public class ClientIODispatch extends AbstractIODispatch<DefaultNHttpClientConnection> {

    private int bufferSize;

    private TargetHandler handler;

    private ClientConnectionFactory clientConnectionFactory;

    public ClientIODispatch(int bufferSize , ClientConnectionFactory clientConnectionFactory){
      this.bufferSize = bufferSize;
        handler = new TargetHandler(bufferSize);
        this.clientConnectionFactory = clientConnectionFactory;

    }

    @Override
    protected DefaultNHttpClientConnection createConnection(final IOSession session) {
        Message message = (Message) session.getAttribute(IOSession.ATTACHMENT_KEY);
        return this.clientConnectionFactory.createConnection(session , message);
    }

    @Override
    protected void onConnected(final DefaultNHttpClientConnection conn) {
        try {
            this.handler.connected(conn, conn.getContext().getAttribute(Constants.MESSAGE));
        } catch (final Exception ex) {
            this.handler.exception(conn, ex);
        }
    }

    @Override
    protected void onClosed(final DefaultNHttpClientConnection conn) {
        this.handler.closed(conn);
    }

    @Override
    protected void onException(final DefaultNHttpClientConnection conn, final IOException ex) {
        this.handler.exception(conn, ex);
    }

    @Override
    protected void onInputReady(final DefaultNHttpClientConnection conn) {
        conn.consumeInput(this.handler);
    }

    @Override
    protected void onOutputReady(final DefaultNHttpClientConnection conn) {
        conn.produceOutput(this.handler);
    }

    @Override
    protected void onTimeout(final DefaultNHttpClientConnection conn) {
        try {
            this.handler.timeout(conn);
        } catch (final Exception ex) {
            this.handler.exception(conn, ex);
        }
    }
}
