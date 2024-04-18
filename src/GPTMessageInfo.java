import java.util.concurrent.Semaphore;

///////////////////////////////////////////////////////////////
// GPT ¸Þ½ÃÁö
///////////////////////////////////////////////////////////////
public class GPTMessageInfo {
	private String ServerID;
	private String MsgID;
	private String RqstMsg;
	private String RespMsg;
	private Semaphore	Waitsp;
	
	public GPTMessageInfo() {
		this.ServerID = null;
		this.MsgID = null;
		this.RqstMsg = null;
		this.RespMsg = null;
		
		this.Waitsp = new Semaphore(0);
	}

	public GPTMessageInfo(String serverid,String msgid,String rqstmsg) {
		this.ServerID = serverid;
		this.MsgID = msgid;
		this.RqstMsg = rqstmsg;
		this.RespMsg = null;
		
		this.Waitsp = new Semaphore(0);
	}
	
	public String GetMsgID() {
		return this.MsgID;
	}

	public void SetRespMsg(String msg) {
		this.RespMsg = msg;
	}

	public String GetRespMsg() {
		return this.RespMsg;
	}
	
	public void Lock() {
		try {
			Waitsp.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void UnLock() {
		try {
			Waitsp.release();;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
