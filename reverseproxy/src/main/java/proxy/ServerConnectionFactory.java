package proxy;


import org.apache.http.HttpRequestFactory;
import org.apache.http.impl.nio.DefaultNHttpServerConnection;
import org.apache.http.nio.reactor.IOSession;
import org.apache.http.nio.util.ByteBufferAllocator;
import org.apache.http.nio.util.HeapByteBufferAllocator;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class ServerConnectionFactory {

    private final HttpRequestFactory requestFactory;
    private final ByteBufferAllocator allocator;
    private final HttpParams params;

    public ServerConnectionFactory(
               final HttpRequestFactory requestFactory,
               final ByteBufferAllocator allocator,
               final HttpParams params) {
        super();
        this.requestFactory = requestFactory != null ? requestFactory : new RequestFactory();
        this.allocator = allocator != null ? allocator : new HeapByteBufferAllocator();
        this.params = params != null ? params : new BasicHttpParams();

    }


    public DefaultNHttpServerConnection createConnection(final IOSession iosession) {

        DefaultNHttpServerConnection conn = new DefaultNHttpServerConnection(iosession, requestFactory, allocator, params);
        int timeout = HttpConnectionParams.getSoTimeout(params);
        conn.setSocketTimeout(timeout);
        return conn;
    }


}

