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

    // The Mapper now outputs (word, docID)
    public static class IndexMapper extends Mapper<Object, Text, Text, Text> {

        private Text word = new Text();
        private Text docIDText = new Text();

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

            String line = value.toString();

            // Split the input line "docID \t contents"
            String[] parts = line.split("\\t", 2); // Split on the first tab only

            if (parts.length != 2) {
                // Skip badly formatted lines
                return;
            }

            String docID = parts[0];
            String contents = parts[1];

            // 1. Convert to lowercase
            contents = contents.toLowerCase();

            // 2. Replace all punctuation and numerals with a space
            //    This regex replaces anything that is NOT a lowercase letter (a-z) or whitespace (\s)
            contents = contents.replaceAll("[^a-z\\s]", " ");

            docIDText.set(docID);

            // Tokenize the cleaned contents
            StringTokenizer itr = new StringTokenizer(contents);
            while (itr.hasMoreTokens()) {
                word.set(itr.nextToken());
                // Emit (word, docID)
                context.write(word, docIDText);
            }
        }
    }

    // The Reducer now takes (word, [docID1, docID2, docID1, ...])
    // and outputs (word, "docID1:count1;docID2:count2;...")
    public static class IndexReducer extends Reducer<Text, Text, Text, Text> {

        private Text result = new Text();

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {

            // Use a HashMap as instructed to count docID occurrences
            Map<String, Integer> counts = new HashMap<String, Integer>();

            // Loop through all docIDs for this word
            for (Text val : values) {
                String docID = val.toString();
                // Get the current count (or 0) and increment it
                counts.put(docID, counts.getOrDefault(docID, 0) + 1);
            }

            // Build the final inverted index string, e.g., "docID1:2;docID2:1"
            StringBuilder postingList = new StringBuilder();
            boolean first = true;

            for (Map.Entry<String, Integer> entry : counts.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    postingList.append(";");
                }
                postingList.append(entry.getKey());   // docID
                postingList.append(":");
                postingList.append(entry.getValue()); // count
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

        // *** REMOVED THE COMBINER ***
        // A combiner cannot be used here because the intermediate (word, docID)
        // pairs cannot be pre-aggregated like (word, 1) could be.

        job.setReducerClass(IndexReducer.class);

        // The final output is (Text, Text)
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}