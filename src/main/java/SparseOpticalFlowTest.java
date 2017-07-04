import bolt.SparseBolt1;
import bolt.SparseBolt2;
import bolt.SparseBolt3;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.shade.org.apache.commons.io.FileUtils;
import org.apache.storm.topology.TopologyBuilder;
import org.bytedeco.javacpp.opencv_videoio;
import spout.SparseSpout;
import utils.FrameDisplay;
import utils.SerializableMat;

import java.io.File;

import static org.bytedeco.javacpp.opencv_videoio.CV_CAP_PROP_FRAME_COUNT;

public class SparseOpticalFlowTest {

    public static void main(String[] args) throws Exception {
        String frameFilePath = "/home/john/idea-data/SparseOpticalFlowTest/";
        String videoFilePath = "/home/john/idea/stormCaffe/pyrlk.avi";
        opencv_videoio.VideoCapture videoCapture = new opencv_videoio.VideoCapture(videoFilePath);
        if (!videoCapture.isOpened()) {
            System.out.println("Failed to open video!");
            System.exit(-1);
        }
        else {
            System.out.println("Succeeded to open video!");
        }
        long totalFrameNumber = (long)(videoCapture.get(CV_CAP_PROP_FRAME_COUNT));
        System.out.println("Total number of frames: " + totalFrameNumber);
        videoCapture.close();

        // delete all existing files in ./data folder
        FileUtils.deleteDirectory(new File("/home/john/idea-data/SparseOpticalFlowTest/"));
        FileUtils.forceMkdir(new File("/home/john/idea-data/SparseOpticalFlowTest/"));

        final String TOPOLOGY_NAME = "SparseOpticalFlowTest";
        String whereFlag = "local";

        if (args.length == 1)
            whereFlag = args[0];
        else {
            System.out.println("Incorrect mode argument. You should declare mode of running topology (local/cluster).");
            System.exit(-1);
        }

        Config config = new Config();
        config.registerSerialization(SerializableMat.class);
//        config.registerMetricsConsumer(org.apache.storm.metric.LoggingMetricsConsumer.class, 1);
        config.setNumWorkers(4);
        config.setDebug(false);
        config.setMessageTimeoutSecs(100);
        config.setMaxSpoutPending(100);

//        // storm-redis setting
//        String host = "127.0.0.1";
//        int port = 6379;
//        JedisPoolConfig poolConfig = new JedisPoolConfig.Builder().setHost(host).setPort(port).build();
//        RedisStoreMapper storeMapper = new FrameStoreMapper();
//        RedisStoreBolt storeBolt = new RedisStoreBolt(poolConfig, storeMapper);

        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("spout", new SparseSpout(), 1);
        builder.setBolt("bolt1", new SparseBolt1(), 1).shuffleGrouping("spout");
        builder.setBolt("bolt2", new SparseBolt2(), 1).shuffleGrouping("spout");
        builder.setBolt("bolt3", new SparseBolt3(), 1).shuffleGrouping("bolt1").shuffleGrouping("bolt2");
//        builder.setBolt("bolt4", storeBolt, 1).shuffleGrouping("bolt2");

        if (whereFlag.equals("local")) { // Run in local model
            Thread displayFrameThread = new Thread(new FrameDisplay(frameFilePath, totalFrameNumber));
            displayFrameThread.start();

            System.out.println("Running in local mode!");
            LocalCluster localCluster = new LocalCluster();
            localCluster.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
//            Utils.sleep(10000);
//            localCluster.killTopology(TOPOLOGY_NAME);
//            localCluster.shutdown();
        }
        else {
            System.out.println("Running in cluster mode!");
            StormSubmitter.submitTopology(TOPOLOGY_NAME, config, builder.createTopology());
        }
    }
}