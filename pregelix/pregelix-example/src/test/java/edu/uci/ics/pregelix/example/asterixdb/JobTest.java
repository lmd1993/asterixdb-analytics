/*
 * Copyright 2009-2013 by The Regents of the University of California
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * you may obtain a copy of the License from
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.uci.ics.pregelix.example.asterixdb;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.MiniDFSCluster;
import org.apache.hadoop.mapred.JobConf;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import edu.uci.ics.asterix.api.common.AsterixHyracksIntegrationUtil;
import edu.uci.ics.asterix.common.config.AsterixPropertiesAccessor;
import edu.uci.ics.asterix.common.config.AsterixTransactionProperties;
import edu.uci.ics.asterix.common.config.GlobalConfig;
import edu.uci.ics.asterix.external.dataset.adapter.FileSystemBasedAdapter;
import edu.uci.ics.asterix.external.util.IdentitiyResolverFactory;
import edu.uci.ics.asterix.test.aql.TestsUtils;
import edu.uci.ics.asterix.testframework.context.TestCaseContext;
import edu.uci.ics.pregelix.api.job.PregelixJob;
import edu.uci.ics.pregelix.core.base.IDriver.Plan;
import edu.uci.ics.pregelix.core.driver.Driver;
import edu.uci.ics.pregelix.core.jobgen.clusterconfig.ClusterConfig;
import edu.uci.ics.pregelix.core.util.PregelixHyracksIntegrationUtil;

/**
 * Runs the runtime test cases under 'asterix-app/src/test/resources/runtimets'.
 */
@RunWith(Parameterized.class)
public class JobTest {

    private static final Logger LOGGER = Logger.getLogger(JobTest.class.getName());

    private static final String PATH_ACTUAL = "rttest" + File.separator;
    private static final String PATH_BASE = StringUtils.join(new String[] { "src", "test", "resources", "runtimets" },
            File.separator);

    private static final String TEST_CONFIG_FILE_NAME = "asterix-build-configuration.xml";
    private static final String[] ASTERIX_DATA_DIRS = new String[] { "nc1data", "nc2data" };

    private static AsterixTransactionProperties txnProperties;

    private static final String ACTUAL_RESULT_DIR = "actual";
    private static final String PATH_TO_HADOOP_CONF = "src/test/resources/hadoop/conf";
    private static final String HADOOP_CONF_PATH = ACTUAL_RESULT_DIR + File.separator + "conf.xml";
    private static final String PATH_TO_JOBS = "src/test/resources/jobs-asterixdb/";
    private static MiniDFSCluster dfsCluster;
    private static JobConf conf = new JobConf();
    private static int numberOfNC = 2;

    private static final Driver driver = new Driver(JobTest.class);

    private static final String PATH_TO_CLUSTER_STORE = "src/test/resources/cluster/stores.properties";
    private static final String PATH_TO_CLUSTER_PROPERTIES = "src/test/resources/cluster/cluster.properties";

    @BeforeClass
    public static void setUp() throws Exception {
        System.out.println("Starting setup");
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting setup");
        }
        System.setProperty(GlobalConfig.CONFIG_FILE_PROPERTY, TEST_CONFIG_FILE_NAME);
        File outdir = new File(PATH_ACTUAL);
        outdir.mkdirs();

        AsterixPropertiesAccessor apa = new AsterixPropertiesAccessor();
        txnProperties = new AsterixTransactionProperties(apa);

        deleteTransactionLogs();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("initializing pseudo cluster");
        }
        AsterixHyracksIntegrationUtil.init();

        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("initializing HDFS");
        }

        // Set the node resolver to be the identity resolver that expects node names
        // to be node controller ids; a valid assumption in test environment.
        System.setProperty(FileSystemBasedAdapter.NODE_RESOLVER_FACTORY_PROPERTY,
                IdentitiyResolverFactory.class.getName());

        // Starts HDFS
        setUpPregelix();
    }

    private static void setUpPregelix() throws Exception {
        ClusterConfig.setStorePath(PATH_TO_CLUSTER_STORE);
        ClusterConfig.setClusterPropertiesPath(PATH_TO_CLUSTER_PROPERTIES);
        cleanupStores();
        PregelixHyracksIntegrationUtil.init();

        System.setProperty(GlobalConfig.CONFIG_FILE_PROPERTY, TEST_CONFIG_FILE_NAME);

        LOGGER.info("Hyracks mini-cluster started");
        FileUtils.forceMkdir(new File(ACTUAL_RESULT_DIR));
        FileUtils.cleanDirectory(new File(ACTUAL_RESULT_DIR));
        startHDFS();
    }

    private static void tearDownPregelix() throws Exception {
        PregelixHyracksIntegrationUtil.deinit();
        LOGGER.info("Hyracks mini-cluster shut down");
        cleanupHDFS();
        cleanupStores();
    }

    private static void cleanupStores() throws IOException {
        FileUtils.forceMkdir(new File("teststore"));
        FileUtils.forceMkdir(new File("build"));
        FileUtils.forceDelete(new File("teststore"));
        FileUtils.forceDelete(new File("build"));
    }

    @AfterClass
    public static void tearDown() throws Exception {
        ClusterConfig.setClusterPropertiesPath(PATH_TO_CLUSTER_PROPERTIES);
        ClusterConfig.setStorePath(PATH_TO_CLUSTER_STORE);
        // Run all Pregel jobs.
        runPregelAll(PATH_TO_JOBS);

        AsterixHyracksIntegrationUtil.deinit();
        File outdir = new File(PATH_ACTUAL);
        File[] files = outdir.listFiles();
        if (files == null || files.length == 0) {
            outdir.delete();
        }
        // clean up the files written by the ASTERIX storage manager
        for (String d : ASTERIX_DATA_DIRS) {
            TestsUtils.deleteRec(new File(d));
        }
        // Cleans up HDFS
        tearDownPregelix();
    }

    private static void deleteTransactionLogs() throws Exception {
        for (String ncId : AsterixHyracksIntegrationUtil.getNcNames()) {
            File log = new File(txnProperties.getLogDirectory(ncId));
            if (log.exists()) {
                FileUtils.deleteDirectory(log);
            }
        }
    }

    @Parameters
    public static Collection<Object[]> tests() throws Exception {
        Collection<Object[]> testArgs = buildTestsInXml(TestCaseContext.ONLY_TESTSUITE_XML_NAME);
        if (testArgs.size() == 0) {
            testArgs = buildTestsInXml(TestCaseContext.DEFAULT_TESTSUITE_XML_NAME);
        }
        return testArgs;
    }

    private static Collection<Object[]> buildTestsInXml(String xmlfile) throws Exception {
        Collection<Object[]> testArgs = new ArrayList<Object[]>();
        TestCaseContext.Builder b = new TestCaseContext.Builder();
        for (TestCaseContext ctx : b.build(new File(PATH_BASE), xmlfile)) {
            testArgs.add(new Object[] { ctx });
        }
        return testArgs;

    }

    private TestCaseContext tcCtx;

    public JobTest(TestCaseContext tcCtx) {
        this.tcCtx = tcCtx;
    }

    @Test
    public void test() throws Exception {
        TestsUtils.executeTest(PATH_ACTUAL, tcCtx, null, false);
    }

    @SuppressWarnings("deprecation")
    private static void startHDFS() throws IOException {
        conf.addResource(new Path(PATH_TO_HADOOP_CONF + "/core-site.xml"));
        conf.addResource(new Path(PATH_TO_HADOOP_CONF + "/mapred-site.xml"));
        conf.addResource(new Path(PATH_TO_HADOOP_CONF + "/hdfs-site.xml"));
        FileSystem lfs = FileSystem.getLocal(new Configuration());
        lfs.delete(new Path("build"), true);
        System.setProperty("hadoop.log.dir", "logs");
        dfsCluster = new MiniDFSCluster(conf, numberOfNC, true, null);
        DataOutputStream confOutput = new DataOutputStream(new FileOutputStream(new File(HADOOP_CONF_PATH)));
        conf.writeXml(confOutput);
        confOutput.flush();
        confOutput.close();
    }

    /**
     * cleanup hdfs cluster
     */
    private static void cleanupHDFS() throws Exception {
        dfsCluster.shutdown();
    }

    private static void runPregelAll(String jobFilePath) throws Exception {
        File jobFileDirectory = new File(jobFilePath);
        if (!jobFileDirectory.isDirectory()) {
            throw new IllegalStateException(jobFilePath + " is not a directory.");
        }
        File[] files = jobFileDirectory.listFiles();
        for (File file : files) {
            runPregelTest(file.getAbsolutePath());
        }
    }

    private static void runPregelTest(String jobFile) throws Exception {
        System.out.println("TEST: " + jobFile);
        PregelixJob job = new PregelixJob("test");
        job.getConfiguration().addResource(new Path(jobFile));
        job.getConfiguration().addResource(new Path(HADOOP_CONF_PATH));
        Plan[] plans = new Plan[] { Plan.OUTER_JOIN };
        for (Plan plan : plans) {
            job.setMergeConnector(true);
            driver.runJob(job, plan, PregelixHyracksIntegrationUtil.CC_HOST,
                    PregelixHyracksIntegrationUtil.TEST_HYRACKS_CC_CLIENT_PORT, false);
        }
    }
}
