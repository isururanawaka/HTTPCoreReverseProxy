package proxy;

import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.nio.DefaultNHttpClientConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class ClientConnectionFactory {

    private final HttpRequestFactory requestFactory;
    private final ByteBufferAllocator allocator;
    private final HttpParams params;


    public ClientConnectionFactory(
               final HttpRequestFactory requestFactory,
               final ByteBufferAllocator allocator,
               final HttpParams params) {
        super();
        this.requestFactory = requestFactory != null ? requestFactory : new RequestFactory();
        this.allocator = allocator != null ? allocator : new HeapByteBufferAllocator();
        this.params = params != null ? params : new BasicHttpParams();

    }


    public DefaultNHttpClientConnection createConnection(final IOSession iosession , Message message) {
        DefaultNHttpClientConnection conn = new DefaultNHttpClientConnection(iosession, new DefaultHttpResponseFactory(),
                                                                             allocator, params);
        int timeout = HttpConnectionParams.getSoTimeout(params);
        conn.setSocketTimeout(timeout);
        conn.getContext().setAttribute(Constants.MESSAGE,message);
        return conn;
    }
}
