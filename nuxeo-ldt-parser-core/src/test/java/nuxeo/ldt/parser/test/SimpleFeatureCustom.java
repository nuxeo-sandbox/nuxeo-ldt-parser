package nuxeo.ldt.parser.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.blob.s3.S3BlobProviderFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import org.junit.runners.model.FrameworkMethod;
/**
 * =======================================================================
 * <b>WARNING</b>
 * This actually does not work. Can't set the mimsc expected configuration vraiable to these system env. variables.
 * As for now, the only to run this test correctly is with maven. Can't debug it on Eclipse.
 * It is likely there is a way of making this work, but I don't know it.
 * <br>
 * <hr>
 * =======================================================================
 * The (native, nuxeo) S3BlobProviderFeature deploys 2 BlobProviders and they expect some env. variables to be set.
 * These variables have dot notation (nuxeo.test.s3storage.provider.test.bucket) which make it hard, on Mac OS/bash to
 * setup before launching Eclipse, so we workaround that.
 * In order for everything to work, and/or if you want to debug your unit test from Eclipse, then, set up the following
 * variables:
 * <br>
 * - Standard AWS variables: AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_SESSION_TOKEN and AWS_REGION
 * - Specific, used in the "test" blob provider: TEST_BUCKET and TEST_BUCKET_PREFIX
 * <br>
 * The Feature still tests if the misc dotted-variables are there and if yes, let them as they are. This allows for the
 * unit testing with maven, like in:
 * mvn -Dnuxeo.test.s3storage.awsid="ABCDEF123" -Dnuxeo.test.s3storage.awssecret="jkljkl" etc.
 * <br>
 * Also, we declare thhe same "test" S3 BlobProvider, forcing "nocache" (this contribution plus the one from
 * S3BlobProviderFeature are merged) to make sure we use S3 and the GetObjectRequest().withRange().
 * 
 * @since 2021
 */
@Deploy("org.nuxeo.runtime.aws")
public class SimpleFeatureCustom extends S3BlobProviderFeature {

    public static final Map<String, String> ENV_VARIABLES;

    static {
        Map<String, String> tempMap = new HashMap<>();
        tempMap.put("nuxeo.test.s3storage.awsid", "AWS_ACCESS_KEY_ID");
        tempMap.put("nuxeo.test.s3storage.awssecret", "AWS_SECRET_ACCESS_KEY");
        tempMap.put("nuxeo.test.s3storage.awstoken", "AWS_SESSION_TOKEN");
        tempMap.put("nuxeo.test.s3storage.region", "AWS_REGION");
        tempMap.put("nuxeo.test.s3storage.provider.test.bucket", "TEST_BUCKET");
        tempMap.put("nuxeo.test.s3storage.provider.test.bucket_prefix", "TEST_BUCKET_PREFIX");
        ENV_VARIABLES = Collections.unmodifiableMap(tempMap);
    }
    
    @Override
    public void beforeSetup(FeaturesRunner runner, FrameworkMethod method, Object test) {
        String toto = null;
        if(toto == null) {
            
        }
        super.beforeSetup(runner, method, test);
    }

    @Override
    public void start(FeaturesRunner runner) {

        for (Map.Entry<String, String> entry : ENV_VARIABLES.entrySet()) {
            String envVar = entry.getKey();
            String systemEnvVar = entry.getValue();

            addSystemEnvironmentVariable(envVar, System.getenv(systemEnvVar));
        }
        super.start(runner);

    }

    public static boolean hasAllKeys() {
        for (Map.Entry<String, String> entry : ENV_VARIABLES.entrySet()) {
            if (StringUtils.isBlank(Framework.getProperty(entry.getKey()))) {
                return false;
            }
        }
        return true;

    }

    protected void addSystemEnvironmentVariable(String key, String value) {

        if (System.getProperty(key) == null) {
            if (StringUtils.isBlank(value)) {
                System.out.println("Missing key for unit test: " + key);
            } else {
                System.setProperty(key, value);
            }
        }
    }

    @Override
    public void stop(FeaturesRunner runner) throws Exception {

        Properties p = System.getProperties();
        for (Map.Entry<String, String> entry : ENV_VARIABLES.entrySet()) {
            p.remove(entry.getKey());
        }

        super.stop(runner);
    }

}
