package com.amazonaws;

import com.amazonaws.operators.PeriodicPerPartitionLoadEnrichmentData;
import com.amazonaws.pojo.Customer;
import com.amazonaws.utils.AppProperties;
import com.amazonaws.utils.KinesisStreamInitialiser;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProcessStreamPeriodicPartitionPreLoadReferenceData {
    private static final Logger LOG = LoggerFactory.getLogger(ProcessStreamPeriodicPartitionPreLoadReferenceData.class);
    private static final String DATA_STREAM_NAME = "event-data-enrichment";


    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        //read the parameters specified from the command line
        ParameterTool parameter = ParameterTool.fromArgs(args);

        //read Kinesis stream name from config
        AppProperties properties = new AppProperties();
        final String dataStream = properties.getProperty("kinesis.stream");

        Properties kinesisConsumerConfig = KinesisStreamInitialiser.getKinesisConsumerConfig(parameter);

        //create Kinesis source
        DataStream<Customer> customerStream = KinesisStreamInitialiser.getKinesisStream(env, kinesisConsumerConfig, dataStream);

        customerStream = customerStream
                //remove all events that aren't CustomerEvent
                .filter(event -> Customer.class.isAssignableFrom(event.getClass()))
                .keyBy(event -> event.getRole())
                .process(new PeriodicPerPartitionLoadEnrichmentData());

        //print customerStream to stdout
        customerStream.print();


        LOG.info("Reading events from stream {}", parameter.get("InputStreamName", dataStream));

        env.execute();
    }

}
