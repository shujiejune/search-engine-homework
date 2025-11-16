import java.io.IOException;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class InvertedIndex {

    // The Mapper outputs (word, docID)
    public static class IndexMapper extends Mapper<Object, Text, Text, Text> {

        private Map<String, Integer> wordCounts;
        private String currDocID = null;
        private Text word = new Text();
        private Text docIDText = new Text();

        private long wordCounter = 0;
        private final static long MAX_WORDS = 50000;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            wordCounts = new HashMap<>();
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            if (wordCounter >= MAX_WORDS) {
                return;
            }

            String line = value.toString();

            // Split the input line "docID \t contents"
            String[] parts = line.split("\\t", 2);

            if (parts.length != 2) {
                return;
            }

            if (currDocID == null) {
                currDocID = parts[0];
            }

            String contents = parts[1];

            contents = contents.toLowerCase();

            // Replace all punctuation (not a lowercase letter or whitespace) and numerals with a space
            contents = contents.replaceAll("[^a-z\\s]", " ");

            StringTokenizer itr = new StringTokenizer(contents);
            while (itr.hasMoreTokens()) {
                if (wordCounter >= MAX_WORDS) {
                    break;
                }
                word.set(itr.nextToken());
                wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                wordCounter++;
            }
        }

        @Override
        public void cleanup(Context context) throws IOException, InterruptedException {
            if (currDocID == null) {
                return;
            }

            for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                wordText.set(entry.getKey());
                docCountText.set(currentDocID + ":" + entry.getValue());
                context.write(wordText, docCountText);
            }
        }
    }

    // The Reducer takes (word, [docID1, docID2, docID1, ...]) and outputs (word, "docID1:count1;docID2:count2;...")
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
        Job job = Job.getInstance(conf, "inverted index");

        job.setJarByClass(InvertedIndex.class);
        job.setMapperClass(IndexMapper.class);

        job.setReducerClass(IndexReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}