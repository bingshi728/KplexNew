package main;
import java.io.IOException;

import loadbalance.loadBalanceStep;
import loadbalance.loadBalanceStep.loadBalanceMapper;
import loadbalance.loadBalanceStep.loadBalancePartitioner;
import loadbalance.loadBalanceStep.loadBalanceReducer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import search.searchOneLeap;

public class RunOver {
	public static final String usr = "wzhuo";
	public static final String passwd = "111";
	public static final String masterhost = "test164:19000";
	public static final String spillPath = "/home/"+RunOver.usr+"/CliqueHadoop/outresult/binary/";
	public static final String hadoophome = "/home/"+usr+"/hadoopyouli/";
	public static final boolean spillRes = false;
	int arglen;
	int pre = 0;
	int reducenum;

	/**
	 * @param args
	 */
	public void doStep1(String[] args) throws Exception {
		
		Configuration conf = new Configuration();

		Job job = new Job(conf, "Our kplex step 0");

		job.setJarByClass(searchOneLeap.class);
		job.setMapperClass(searchOneLeap.oneLeapFinderMapper.class);
		job.setReducerClass(searchOneLeap.OneLeapFinderReducer.class);// 换Reducer
		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(reducenum);
		for (int i = 0; i < arglen - 1; i++)
			FileInputFormat.addInputPath(job, new Path(args[i]));
		FileOutputFormat.setOutputPath(job, new Path(pre + "_result_binary"));

		long t1 = System.currentTimeMillis();
		job.waitForCompletion(true);
		long t2 = System.currentTimeMillis();
		System.out.println(pre + "-phase cost:" + (t2 - t1));

	}

	public void doStep2(String[] args) throws IOException,
			InterruptedException, ClassNotFoundException {

		Configuration conf = new Configuration();

		String in = "binaryinputR";

		Job job = new Job(conf, "Our kplex step "+pre);

		job.setJarByClass(loadBalanceStep.class);
		job.setMapperClass(loadBalanceMapper.class);
		job.setPartitionerClass(loadBalancePartitioner.class);
		job.setReducerClass(loadBalanceReducer.class);// 换Reducer

		job.setMapOutputKeyClass(IntWritable.class);
		job.setMapOutputValueClass(Text.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);
		job.setNumReduceTasks(reducenum);
		FileInputFormat.addInputPath(job, new Path(in));
		FileOutputFormat.setOutputPath(job, new Path(pre + "_result_binary"));

		long t1 = System.currentTimeMillis();
		job.waitForCompletion(true);
		long t2 = System.currentTimeMillis();
		System.out.println(pre + "-phase cost:" + (t2 - t1));
	}

	public void dojob(String[] args) throws Exception {
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args)
				.getRemainingArgs();
		if (otherArgs.length < 2) {
			System.err.println("Usage: CliqueMain <in> <reducenum>");
			System.exit(2);
		}
		// String in=args[0];
		arglen = args.length;
		pre = 0;
		reducenum = Integer.valueOf(args[arglen - 1]);
		long t1 = System.currentTimeMillis();
		doStep1(args);
		long t2 = System.currentTimeMillis();
		all += (t2 -t1);
		synchronized (this) {
			long thisphasesize = 0;
			this.wait(5000);
			emitfilesize = (long) RemoteSSH.getRemoteFilesSize();
			thisphasesize = emitfilesize;
			while (thisphasesize != 0) {
				System.out.println("emit file size "+ thisphasesize/1024/1024+"M");
				Process p = Runtime
						.getRuntime()
						.exec(new String[] { "/bin/sh", "-c",
								hadoophome+"bin/hadoop fs -rmr binaryinputR/" });
				p.waitFor();
				p.destroy();
				RemoteSSH.batch();
				this.wait(15000); 
				pre++;
				long t11 = System.currentTimeMillis();
				doStep2(args);
				long t12 = System.currentTimeMillis();
				all += (t12 - t11);
				this.wait(5000);
				thisphasesize = (long) RemoteSSH.getRemoteFilesSize();
				emitfilesize += thisphasesize;
			}
		}

	}
	static long all = 0;
	static long emitfilesize =0;
	public static void main(String[] args) throws Exception {
		
		new RunOver().dojob(args);
		
		System.out.println("all:" + all +" emitfilesize "+emitfilesize/1024/1024+"M");
	}

}
