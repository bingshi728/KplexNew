package kplexnew;

import loadbalance.loadBalanceStep;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import search.searchOneLeap;
public class countK_Plex {

	public static void main(String[] args) throws Exception
	{

		String in=args[0];
		String pre=args[1];
		int reducenum=Integer.valueOf(args[2]);
		Configuration conf = new Configuration();
		
		
		Job job4 = new Job(conf,"count kplex");
		job4.setNumReduceTasks(reducenum);
		job4.setJarByClass(countK_Plex.class);
		job4.setMapperClass(searchOneLeap.oneLeapFinderMapper.class);
		job4.setPartitionerClass(searchOneLeap.oneLeapFinderPartitioner.class);
		job4.setReducerClass(searchOneLeap.OneLeapFinderReducer.class);
		job4.setMapOutputKeyClass(IntWritable.class);
		job4.setMapOutputValueClass(Text.class);
		job4.setOutputKeyClass(IntWritable.class);
		job4.setOutputValueClass(Text.class);
		FileInputFormat.addInputPath(job4, new Path(in));
		FileOutputFormat.setOutputPath(job4, new Path(pre + "_common"));
		
		
		Job job5 = new Job(conf,"load balance");
		job5.setNumReduceTasks(reducenum);
		job5.setJarByClass(countK_Plex.class);
		job5.setMapperClass(loadBalanceStep.loadBalanceMapper.class);
		job5.setPartitionerClass(loadBalanceStep.loadBalancePartitioner.class);
		job5.setReducerClass(loadBalanceStep.loadBalanceReducer.class);
		job5.setMapOutputKeyClass(IntWritable.class);
		job5.setMapOutputValueClass(Text.class);
		job5.setOutputKeyClass(IntWritable.class);
		job5.setOutputValueClass(Text.class);
		//FileInputFormat.addInputPath(job5, new Path(pre+"5"));
		FileInputFormat.addInputPath(job5, new Path(pre + "_common"));
		FileOutputFormat.setOutputPath(job5, new Path(pre + "_balance"));
		
		long t1 = System.currentTimeMillis();		
		job4.waitForCompletion(true);
		long t2 = System.currentTimeMillis();			
		job5.waitForCompletion(true);
		long t3 = System.currentTimeMillis();

		System.out.println("computer kplex:"+(t2-t1));
		System.out.println("load balance:"+(t3-t2));
	
		 
	}
}
