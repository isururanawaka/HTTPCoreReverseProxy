package proxy;


import org.apache.http.impl.nio.reactor.IOReactorConfig;

import java.util.Properties;

public class Util {


    public static IOReactorConfig buildIOReactorConfig(Properties properties) {
        IOReactorConfig config = new IOReactorConfig();
        config.setIoThreadCount(Integer.parseInt(properties.getProperty(Constants.REACTOR_WORKERS, "4")));
        config.setSoTimeout(Integer.parseInt(properties.getProperty(Constants.TIME_OUT, "60000")));
        config.setConnectTimeout(Integer.parseInt(properties.getProperty(Constants.CONNECTION_TIMEOUT, "60000")));
        config.setTcpNoDelay(true);
        config.setSoLinger(-1);
        config.setSoReuseAddress(false);
        config.setInterestOpQueued(false);
        config.setSelectInterval(Integer.parseInt(properties.getProperty(Constants.SELECT_INTERVAL, "1000")));
        return config;
    }


}
