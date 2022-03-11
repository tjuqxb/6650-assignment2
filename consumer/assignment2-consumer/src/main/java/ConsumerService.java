import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import domain.SkierPost;
import javax.xml.bind.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.*;

public class ConsumerService {
    private static int numThreads;
    private final static String QUEUE_NAME = "threadExQ";
    private final static String IP_MQ = "172.31.27.60";
    private static ConcurrentMap<Integer, Vector<SkierPost>> skierRecordMap = new ConcurrentHashMap<>();

    /**
     * Entry point for the program. Can read arguments either from commandline or from "src/main/java/parameters.xml" file
     *
     * @param args user specified arguments.
     *              * arg0: umber of threads to run (numThreads >= 1)
     */
    public static void main(String[] args) throws JAXBException, IOException, TimeoutException {
        Integer[] argsRec = new Integer[1];
        if (args.length == 0) {
            JAXBContext jc = JAXBContext.newInstance(Parameters.class);
            Unmarshaller unmarshaller = jc.createUnmarshaller();
            unmarshaller.setEventHandler(new ValidationEventHandler() {
                @Override
                public boolean handleEvent(ValidationEvent event) {
                    System.out.println(event.getMessage());
                    return true;
                }}

            );
            File xml = new File("src/main/java/parameters.xml");
            Parameters root = (Parameters) unmarshaller.unmarshal(xml);
            argsRec[0] = root.numThreads;
        } else if (args.length == 1){
            argsRec[0] = Utils.parseSingleNumber(args[0]);
        } else {
            System.err.println("wrong number of arguments");
            return;
        }
        if (argsRec[0] == null || argsRec[0] <= 0) {
            System.err.println("wrong arguments");
            return;
        }
        numThreads = argsRec[0];
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_MQ);
        Connection conn = factory.newConnection();
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int i = 0; i < numThreads; i++) {
            Channel channel = conn.createChannel();
            channel.basicQos(1);
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            executor.execute(new SingleConsumerThread(skierRecordMap, channel, QUEUE_NAME));
        }
        executor.shutdown();
    }



}
