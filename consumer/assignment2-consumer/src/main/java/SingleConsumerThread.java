import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import domain.SkierPost;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleConsumerThread implements Runnable{
    private ConcurrentMap<Integer, Vector<SkierPost>> skierRecordMap;
    private final Channel channel;
    private final Gson gson = new Gson();
    private final String queueName;

    public SingleConsumerThread(ConcurrentMap<Integer, Vector<SkierPost>> skierRecordMap, Channel channel, String queueName) {
        this.skierRecordMap =  skierRecordMap;
        this.channel = channel;
        this.queueName = queueName;
    }

    @Override
    public void run() {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            Map<String, String> data = gson.fromJson(message, Map.class);
            for (String k: data.keySet()) {
                String str = data.get(k);
                SkierPost post = gson.fromJson(str, SkierPost.class);
                Vector<SkierPost> vector = new Vector<>();
                Integer key = Utils.parseSingleNumber(k);
                skierRecordMap.putIfAbsent(key, vector);
                Vector<SkierPost> vector1 = skierRecordMap.get(key);
                //System.out.println(key + ": " + post.getLiftID() + " " + post.getTime() + " " + post.getWaitTime());
                vector1.add(post);
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        try {
            channel.basicConsume(queueName, false, deliverCallback, consumerTag -> { });
        } catch (IOException e) {
            Logger.getLogger(SingleConsumerThread.class.getName()).log(Level.SEVERE, null, e);
        }

    }
}
