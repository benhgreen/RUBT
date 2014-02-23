import java.net.URL;


public class GetRequest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		System.out.println(sendGetRequest());
		
	}
	
	private static int sendGetRequest() throws Exception{
		String url ="http://www.mannyjl625.info/songfinder/getrequest.txt";
		URL obj = new URL(url);
		int contentLength = obj.openConnection().getContentLength();
		
		return contentLength;
	}

}
