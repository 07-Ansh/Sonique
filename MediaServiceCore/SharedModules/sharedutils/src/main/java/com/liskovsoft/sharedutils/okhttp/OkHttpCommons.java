package com.liskovsoft.sharedutils.okhttp;

import android.os.Build;

import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.sharedutils.okhttp.interceptors.RateLimitInterceptor;
import com.liskovsoft.sharedutils.okhttp.interceptors.UnzippingInterceptor;
import com.liskovsoft.sharedutils.prefs.GlobalPreferences;
import com.localebro.okhttpprofiler.OkHttpProfilerInterceptor;
import okhttp3.CipherSuite;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Protocol;
import okhttp3.TlsVersion;
import okhttp3.logging.HttpLoggingInterceptor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.net.Authenticator;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class OkHttpCommons {
    private static final String TAG = OkHttpCommons.class.getSimpleName();
    public static final long CONNECT_TIMEOUT_MS = 20_000;
    public static final long READ_TIMEOUT_MS = 20_000;
    public static final long WRITE_TIMEOUT_MS = 20_000;
    public static boolean enableProfiler = true;

    private OkHttpCommons() {
        
    }

    // This is nearly equal to the cipher suites supported in Chrome 51, current as of 2016-05-25.
    // All of these suites are available on Android 7.0; earlier releases support a subset of these
    // suites. https://github.com/square/okhttp/issues/1972
    private static final CipherSuite[] APPROVED_CIPHER_SUITES = new CipherSuite[] {
            // TLSv1.3
            CipherSuite.TLS_AES_128_GCM_SHA256,
            CipherSuite.TLS_AES_256_GCM_SHA384,
            CipherSuite.TLS_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_AES_128_CCM_SHA256,
            // Robolectric error (no such field). Constructing manually.
            //CipherSuite.TLS_AES_256_CCM_8_SHA256,
            CipherSuite.forJavaName("TLS_AES_256_CCM_8_SHA256"),

            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
            CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,

            // Note that the following cipher suites are all on HTTP/2's bad cipher suites list. We'll
            // continue to include them until better suites are commonly available. For example, none
            // of the better cipher suites listed above shipped with Android 4.4 or Java 7.
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
            CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
            CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA, // should be commented out?
            CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA,

            // Change TLS fingerprint by altering default cipher list
            // From original fix
            CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256,
            // From NewPipe Downloader
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
            CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA
    };

    private static void setupConnectionParams(OkHttpClient.Builder okBuilder) {
        // Setup default timeout
        // https://stackoverflow.com/questions/39219094/sockettimeoutexception-in-retrofit
        okBuilder.connectTimeout(CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        okBuilder.readTimeout(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        okBuilder.writeTimeout(WRITE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        // Imitate 'keepAlive' = false (yt throttle fix? Cause slow video loading?)
        // https://stackoverflow.com/questions/70873186/how-to-disable-connection-pooling-and-make-a-new-connection-for-each-request-in
        // https://stackoverflow.com/questions/63047533/connection-pool-okhttp
        // NOTE: SocketTimeoutException fix: setup connection pool with 0 (!) idle connections!
        //okBuilder.connectionPool(new ConnectionPool(0, READ_TIMEOUT_MS, TimeUnit.MILLISECONDS));
        //okBuilder.connectionPool(new ConnectionPool(10, 24, TimeUnit.HOURS)); // Video unavailable fix???
        okBuilder.connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES)); // fix npe on pool dispose???
    }

    /**
     * Fixing SSL handshake timed out (probably provider issues in some countries)
     */
    private static void setupConnectionFix(OkHttpClient.Builder okBuilder) {
        // Alter cipher list to create unique TLS fingerprint
        ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .cipherSuites(APPROVED_CIPHER_SUITES)
                .build();
        okBuilder.connectionSpecs(Arrays.asList(cs, ConnectionSpec.CLEARTEXT));
    }

    /**
     * Fixing SSL handshake timed out (probably provider issues in some countries)
     */
    private static void setupConnectionFixOrigin(OkHttpClient.Builder okBuilder) {
        // TLS 1.2 not supported on pre Lollipop (fallback to TLS 1.0)
        // Note, TLS 1.0 doesn't have SNI support. So, fix should work.
        if (Build.VERSION.SDK_INT <= 19) {
            return;
        }

        ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.COMPATIBLE_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .cipherSuites(
                        CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                        CipherSuite.TLS_DHE_RSA_WITH_AES_128_GCM_SHA256
                )
                .build();
        okBuilder.connectionSpecs(Collections.singletonList(cs));
    }

    private static OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder builder) {
        //if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
        //    return custom builder;
        //}

        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, null, null);
            builder.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()));

            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build();

            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(cs);
            specs.add(ConnectionSpec.COMPATIBLE_TLS);
            specs.add(ConnectionSpec.CLEARTEXT);

            builder.connectionSpecs(specs);
        } catch (Exception exc) {
            Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
        }

        return builder;
    }

    private static OkHttpClient.Builder enableTls12OnPreLollipop2(OkHttpClient.Builder builder) {
        //if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 22) {
        //    return custom builder;
        //}

        try {
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init((KeyStore) null);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
            if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                throw new IllegalStateException("Unexpected default trust managers:"
                        + Arrays.toString(trustManagers));
            }
            X509TrustManager trustManager = (X509TrustManager) trustManagers[0];

            SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, new TrustManager[] { trustManager }, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            builder.sslSocketFactory(sslSocketFactory, trustManager);

            ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build();

            List<ConnectionSpec> specs = new ArrayList<>();
            specs.add(cs);
            specs.add(ConnectionSpec.COMPATIBLE_TLS);
            specs.add(ConnectionSpec.CLEARTEXT);

            builder.connectionSpecs(specs);
        } catch (Exception exc) {
            Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
        }

        return builder;
    }

    /**
     * Fix github updates on Android 4<br/>
     * Setting testMode configuration. If set as testMode, the connection will skip certification check
     */
    @SuppressWarnings("deprecation")
    private static void configureToIgnoreCertificate(OkHttpClient.Builder builder) {
        if (Build.VERSION.SDK_INT > 19) {
            return;
        }

        Log.w(TAG, "Ignore Ssl Certificate");
        try {

            // Create a trust manager that does not validate certificate chains
            final TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                                throws CertificateException {
                        }

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[]{};
                        }
                    }
            };

            // Install the all-trusting trust manager
            //final SSLContext sslContext = SSLContext.getInstance("SSL");
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = new Tls12SocketFactory(sslContext.getSocketFactory());

            //builder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            builder.sslSocketFactory(sslSocketFactory);
            //builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            Log.w(TAG, "Exception while configuring IgnoreSslCertificate: " + e, e);
        }
    }

    /**
     * Fix for {@link okhttp3.internal.http2.StreamResetException}: stream was reset: CANCEL<br/>
     * Force HTTP 1.1 protocol<br/>
     * Happen when frequently do interrupt/create stream<br/>
     * https://stackoverflow.com/questions/53648852/how-to-solve-okhttp3-internal-http2-streamresetexception-stream-was-reset-refu<br/>
     * https://github.com/square/okhttp/issues/3955
     */
    private static void fixStreamResetError(Builder okBuilder) {
        okBuilder.protocols(Collections.singletonList(Protocol.HTTP_1_1));
    }
    
    public static OkHttpClient.Builder setupBuilder(OkHttpClient.Builder okBuilder) {
        if (GlobalPreferences.sInstance != null && GlobalPreferences.sInstance.isIPv4DnsPreferred()) {
            // Cause hangs and crashes (especially on Android 8 devices or Dune HD)
            forceIPv4Dns(okBuilder);
            //preferIPv4Dns(okBuilder); // alt method
        }
        //setupProxy(okBuilder); // proxy configured in system props
        setupConnectionFix(okBuilder);
        setupConnectionParams(okBuilder);
        configureToIgnoreCertificate(okBuilder);
        fixStreamResetError(okBuilder); // Should I move the line to Retrofit utils?
        enableDecompression(okBuilder);
        //enableRateLimiter(okBuilder)

        //disableCache(okBuilder);
        debugSetup(okBuilder);

        return okBuilder;
    }

    private static void disableCache(OkHttpClient.Builder okBuilder) {
        // Disable cache (could help with dlfree error on Eltex)
        // Spoiler: no this won't help with dlfree error on Eltex
        okBuilder.cache(null);
    }

    /**
     * Checks that response is compressed and do uncompress if needed.
     */
    private static void enableDecompression(OkHttpClient.Builder builder) {
        // Add gzip/deflate/br support
        //builder.addInterceptor(BrotliInterceptor.INSTANCE);
        builder.addInterceptor(new UnzippingInterceptor());
    }

    private static void enableRateLimiter(OkHttpClient.Builder builder) {
        builder.addInterceptor(new RateLimitInterceptor());
    }

    private static void debugSetup(OkHttpClient.Builder okBuilder) {

    }

    private static void addProfiler(OkHttpClient.Builder okBuilder) {
        okBuilder.addInterceptor(new OkHttpProfilerInterceptor());
    }

    private static void addLogger(OkHttpClient.Builder okBuilder) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        okBuilder.addInterceptor(logging);
    }

    private static void preferIPv4Dns(OkHttpClient.Builder okBuilder) {
        okBuilder.dns(new OkHttpDNSSelector(OkHttpDNSSelector.IPvMode.IPV4_FIRST));
        //okBuilder.dns(new PreferIpv4Dns());
    }

    private static void forceIPv4Dns(OkHttpClient.Builder okBuilder) {
        okBuilder.dns(hostname -> {
            List<InetAddress> lookup = Dns.SYSTEM.lookup(hostname);
            List<InetAddress> filter = Helpers.filter(
                lookup, value -> value instanceof Inet4Address
            );
            return filter != null ? filter : lookup;
        });
    }

    /**
     * Usage: `OkHttpClient newClient = wrapDns(client)`<br></br>
     * https://github.com/square/okhttp/blob/master/okhttp-dnsoverhttps/src/test/java/okhttp3/dnsoverhttps/DohProviders.java
     */
    private static OkHttpClient wrapDnsOverHttps(OkHttpClient client) {
        return client.newBuilder().dns(DohProviders.buildGoogle(client)).build();
    }

    private static void setupProxy(OkHttpClient.Builder builder) {
        setupProxy(builder, "socksProxyHost", "socksProxyPort", "socksProxyUser", "socksProxyPassword", Proxy.Type.SOCKS);
        setupProxy(builder, "https.proxyHost", "https.proxyPort", "https.proxyUser", "https.proxyPassword", Proxy.Type.HTTP);
        setupProxy(builder, "http.proxyHost", "http.proxyPort", "http.proxyUser", "http.proxyPassword", Proxy.Type.HTTP);
    }

    // https://stackoverflow.com/questions/35554380/okhttpclient-proxy-authentication-how-to
    private static void setupProxy(Builder builder, String proxyHost, String proxyPort, String proxyUser, String proxyPassword, Proxy.Type proxyType) {
        String host = System.getProperty(proxyHost);
        String port = System.getProperty(proxyPort);
        String user = System.getProperty(proxyUser);
        String password = System.getProperty(proxyPassword);

        if (host == null || port == null) {
            return;
        }

        if (user != null && password != null) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, password.toCharArray());
                }
            });
        }

        builder.proxy(new Proxy(proxyType, new InetSocketAddress(host, Helpers.parseInt(port))));
    }
}
