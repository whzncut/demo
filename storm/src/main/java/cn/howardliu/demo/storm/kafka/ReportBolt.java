package cn.howardliu.demo.storm.kafka;

import backtype.storm.topology.BasicOutputCollector;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseBasicBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <br/>create at 16-1-20
 *
 * @author liuxh
 * @since 1.0.0
 */
public class ReportBolt extends BaseBasicBolt {
    @Override
    public void execute(Tuple input, BasicOutputCollector collector) {
        String word = input.getStringByField("word");
        Long count = input.getLongByField("count");
        String reportMessage = "{'word': '" + word + "', 'count': '" + count + "'}";
        collector.emit(new Values(reportMessage));
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("message"));
    }
}
