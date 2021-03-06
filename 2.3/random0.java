import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.Random;

public class LoadBalancer {
	private static final int THREAD_POOL_SIZE = 4;
	private final ServerSocket socket;
	private final DataCenterInstance[] instances;
	private static int ROUND = 100;

	public LoadBalancer(ServerSocket socket, DataCenterInstance[] instances) {
		this.socket = socket;
		this.instances = instances;
	}

	// Complete this function
	public void start() throws IOException {
		ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

		URL cpuCheck1 = new URL("http://ec2-54-174-245-17.compute-1.amazonaws.com:8080/info/cpu");
		URL cpuCheck2 = new URL("http://ec2-54-175-253-59.compute-1.amazonaws.com:8080/info/cpu");
		URL cpuCheck3 = new URL("http://ec2-54-175-253-93.compute-1.amazonaws.com:8080/info/cpu");
		
		BufferedReader br1 = null;
		BufferedReader br2 = null;
		BufferedReader br3 = null;
		
		float cpuUtilization1 = Float.MAX_VALUE;
		float cpuUtilization2 = Float.MAX_VALUE;
		float cpuUtilization3 = Float.MAX_VALUE;
		
		System.out.println("Test Start!");

		int[] portion = new int[3];
		int currentInstance = 0;
		int cnt = 0;
		Random random = new Random();
		int randomNumber = 0;
		int[][] excuteArray = {{0,1,2},{0,2,1},{1,0,2},{1,2,0},{2,0,1},{2,1,0}};

		while (true) {
			// By default, it will send all requests to the first instance
			
			br1 = new BufferedReader(new InputStreamReader(cpuCheck1.openStream()));
			br2 = new BufferedReader(new InputStreamReader(cpuCheck2.openStream()));
			br3 = new BufferedReader(new InputStreamReader(cpuCheck3.openStream()));
			
			boolean isGotten1 = false;
			while (isGotten1 == false){				
				try {
					cpuUtilization1 = getCpuUtilization(br1.readLine());
					isGotten1 = true;
				} catch (NullPointerException e) {
					System.out.println(br1.readLine());
				}
			}
			
			boolean isGotten2 = false;
			while (isGotten2 == false){				
				try {
					cpuUtilization2 = getCpuUtilization(br2.readLine());
					isGotten2 = true;
				} catch (NullPointerException e) {
					System.out.println(br2.readLine());
				}
			}
			
			boolean isGotten3 = false;
			while (isGotten3 == false){				
				try {
					cpuUtilization3 = getCpuUtilization(br3.readLine());
					isGotten3 = true;
				} catch (NullPointerException e) {
					System.out.println(br3.readLine());
				}
			}
			
			br1.close();
			br2.close();
			br3.close();
				
			portion = findUnderUtilization(cpuUtilization1, cpuUtilization2, cpuUtilization3);
		//	for (int i : portion) {
		//		System.out.println(i);
		//	}
			for (cnt = 0; cnt < ROUND; cnt++) {
				randomNumber = random.nextInt(ROUND);
				if (randomNumber < portion[0]) {
					Runnable requestHandler = new RequestHandler(socket.accept(), instances[0]);
					executorService.execute(requestHandler);					
				} else if (randomNumber < portion[0] + portion[1] && randomNumber >= portion[0]) {
					Runnable requestHandler = new RequestHandler(socket.accept(), instances[1]);
					executorService.execute(requestHandler);
				} else {
					Runnable requestHandler = new RequestHandler(socket.accept(), instances[2]);
					executorService.execute(requestHandler);
				}
				
			}
			
			
		}
	}
	
	private int[] findUnderUtilization(float c1, float c2, float c3) {
		int[] portion = new int[3];
		float[] helper = new float[3];
		
		if (c1 == 0) {
			c1 = 1;
		}
		
		if (c2 == 0) {
			c2 = 1;
		}
		
		if (c3 == 0) {
			c3 = 1;
		}
		
		float sum = (c1 + c2 + c3);
		helper[0] = (sum / c1);
		helper[1] = (sum / c2);
		helper[2] = (sum / c3);
		
		float sum2 = helper[0] + helper[1] + helper[2];
		portion[0] = (int)(helper[0] / sum2 * ROUND);
		portion[1] = (int)(helper[1] / sum2 * ROUND);
		portion[2] = (int)(helper[2] / sum2 * ROUND);
		return portion;
	}
	
	private float getCpuUtilization(String input) {
		Pattern p = Pattern.compile("<");
		String[] string1 = p.split(input);
		
		Pattern p2 = Pattern.compile(">");
		String[] string2 = p2.split(string1[7]);				
		
		float result = Float.parseFloat(string2[1]);
		
		return result;
	}
}
