import java.io.IOException;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class BigramIndex {

    public static class IndexMapper extends Mapper<Object, Text, Text, Text> {

        private Map<String, Integer> bigramCounts;
        private String currDocID = null;
        private Text bigramText = new Text();
        private Text docCountText = new Text();
        private Set<String> selectedBigrams;

        @Override
        protected void setup(Context context) {
            bigramCounts = new HashMap<String, Integer>();

            selectedBigrams = new HashSet<String>();
            selectedBigrams.add("computer science");
            selectedBigrams.add("information retrieval");
            selectedBigrams.add("power politics");
            selectedBigrams.add("los angeles");
            selectedBigrams.add("bruce willis");
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();
            String[] parts = line.split("\\s+", 2);

            if (parts.length != 2) {
                return;
            }

            if (currDocID == null) {
                currDocID = parts[0];
            }

            String contents = parts[1];
            contents = contents.toLowerCase();
            contents = contents.replaceAll("[^a-z\\s]", " ");

            StringTokenizer itr = new StringTokenizer(contents);

            String prevWord = null;

            while (itr.hasMoreTokens()) {
                String currWord = itr.nextToken();

                if (prevWord != null) {
                    String bigram = prevWord + " " + currWord;

                    if (selectedBigrams.contains(bigram)) {
                        bigramCounts.put(bigram, bigramCounts.getOrDefault(bigram, 0) + 1);
                    }
                }

                prevWord = currWord;
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            if (currentDocID == null) {
                return;
            }

            for (Map.Entry<String, Integer> entry : bigramCounts.entrySet()) {
                bigramText.set(entry.getKey());
                docCountText.set(currentDocID + ":" + entry.getValue());
                context.write(bigramText, docCountText);
            }
        }
    }

    public static class IndexReducer extends Reducer<Text, Text, Text, Text> {

        private Text result = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            StringBuilder postingList = new StringBuilder();
            boolean first = true;

            for (Text val : values) {
                if (first) {
                    first = false;
                } else {
                    postingList.append(";");
                }
                postingList.append(val.toString());
            }

            result.set(postingList.toString());
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();

        Job job = Job.getInstance(conf, "bigram index");

        job.setJarByClass(BigramIndex.class);
        job.setMapperClass(IndexMapper.class);

        job.setReducerClass(IndexReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}