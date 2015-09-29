package proxy;


import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.DefaultListeningIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.nio.reactor.ListenerEndpoint;
import org.apache.http.nio.reactor.ListeningIOReactor;
import org.apache.http.params.BasicHttpParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

public class HttpListener {

    public static void main(String[] args) {

        if (args.length > 0) {

            File propFile = new File(args[0]);

            Properties props = new Properties();
            try {
                FileInputStream fis = new FileInputStream(propFile);
                props.load(fis);
                start(props);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        } else {
            Properties props = new Properties();
            props.setProperty(Constants.REMOTE_HOST,"localhost");
            props.setProperty(Constants.REMOTE_PORT,"9000");
            props.setProperty(Constants.PORT,"8080");
            props.setProperty(Constants.TIME_OUT,"120000");
            props.setProperty(Constants.BUFFER_SIZE,"8192");
            start(props);
        }
    }


    private static void start(Properties properties) {
        int port = Integer.parseInt(properties.getProperty(Constants.PORT));
        String host = properties.getProperty(Constants.REMOTE_HOST);
        int remotePort = Integer.parseInt(properties.getProperty(Constants.REMOTE_PORT));
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, remotePort);

        int bufferSize = Integer.parseInt(properties.getProperty(Constants.BUFFER_SIZE));

        int so_timeout = Integer.parseInt(properties.getProperty(Constants.TIME_OUT));


        BasicHttpParams params = new BasicHttpParams();
        params.setIntParameter(Constants.TIME_OUT, so_timeout);
        ServerConnectionFactory serverConnectionFactory = new ServerConnectionFactory(null, null, params);
        final ClientConnectionFactory clientConnectionFactory = new ClientConnectionFactory(null, null, params);
        final ClientIODispatch clientIODispatch = new ClientIODispatch(bufferSize, clientConnectionFactory);
        IOReactorConfig ioReactorConfig = Util.buildIOReactorConfig(properties);

        try {
            final ConnectingIOReactor connectingIOReactor =
                       new DefaultConnectingIOReactor(ioReactorConfig,
                                                      new ReactorThreadFactory
                                                                 (new ThreadGroup("http" + " thread group"), "http"));
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connectingIOReactor.execute(clientIODispatch);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
            SourceHandler sourceHandler = new SourceHandler(connectingIOReactor, inetSocketAddress, bufferSize, so_timeout);
            final ServerIODispatch serverIODispatch = new ServerIODispatch(sourceHandler, serverConnectionFactory);
            final ListeningIOReactor listeningIOReactor = new DefaultListeningIOReactor(ioReactorConfig, new ReactorThreadFactory
                       (new ThreadGroup("http" + " thread group"), "http"));
            Thread lisT = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        listeningIOReactor.execute(serverIODispatch);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            lisT.start();

            Thread.sleep(3000);

            ListenerEndpoint listenerEndpoint = listeningIOReactor.listen(new InetSocketAddress("localhost", port));
            listenerEndpoint.waitFor();
            System.out.println("Listening started on port " + port);
        } catch (IOReactorException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


}
