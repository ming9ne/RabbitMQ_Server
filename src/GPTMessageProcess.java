import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;

import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

///////////////////////////////////////////////////////////////
//Request Queue�� ����ִ� �޽����� ������ ����Ʈ
//Response �޽ø� ���Ž� �� ����Ʈ���� ��û�� �޽����� ã�Ƽ� ������� ������
///////////////////////////////////////////////////////////////
class GPTMessageList {
	private LinkedList<GPTMessageInfo> list;
	
	GPTMessageList() {
		list = new LinkedList<>();
	}
	
	public synchronized int size() {
		return list.size();
	}
	
	public synchronized void add(GPTMessageInfo data) {
		list.add(data);
	}

	public synchronized GPTMessageInfo Get(String id) {
		GPTMessageInfo msginfo;
		
		ListIterator<GPTMessageInfo> lt = list.listIterator();
		while( lt.hasNext() ) {
			System.out.println("messageinfo finding.. " + id);
			System.out.flush();
			
			msginfo = lt.next();
			if ( msginfo.GetMsgID().equals(id)) {
				System.out.println("     messageinfo finded. " + id);
				System.out.flush();
				lt.remove();
				return msginfo;
			}
		}
		System.out.println("     messageinfo not found. " + id);
		System.out.flush();
		return null;
	}
}

///////////////////////////////////////////////////////////////
// Request Queue Receiver
///////////////////////////////////////////////////////////////
class GPTRqstConsumer extends DefaultConsumer {
	private Channel ch;
	private GPTMessageProcess proc;
	
	public GPTRqstConsumer(Channel channel, GPTMessageProcess process) {
		super(channel);
		this.ch = channel;
		this.proc = process;
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
			throws IOException {
		// TODO Auto-generated method stub
		super.handleDelivery(consumerTag, envelope, properties, body);
		
		String rqstmsg = new String(body);
		
		String id2 = "rqstconsumer thread=" + Thread.currentThread().getId();
		System.out.println("RQST Received : " + rqstmsg + " threadid=" + id2);
		System.out.flush();

		// GPT Communication
		String userid = properties.getContentType();	// UserID
		String RespMsg = proc.GPTComm(userid,rqstmsg);
		
		proc.RespSend(properties,RespMsg);
		
		System.out.println("RQST Received complete "  + id2);
		System.out.flush();
	}
}

///////////////////////////////////////////////////////////////
//Response Queue Receiver
///////////////////////////////////////////////////////////////
class GPTRespConsumer extends DefaultConsumer {
	private Channel ch;
	private GPTMessageProcess proc;
	
	public GPTRespConsumer(Channel channel,GPTMessageProcess process) {
		super(channel);
		this.ch = channel;
		this.proc = process;
	}

	@Override
	public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body)
			throws IOException {
		// TODO Auto-generated method stub
		super.handleDelivery(consumerTag, envelope, properties, body);
		
		String respmsg = new String(body);
		
		String id2 = "RESP consumer thread=" + Thread.currentThread().getId();
		System.out.println("RESP Received <" + respmsg + "> threadid=" + id2);
		System.out.flush();
		
		// Find Message
		String msgid = properties.getMessageId();
		GPTMessageInfo msginfo = proc.GetMessageInfo(msgid);
		
		if ( msginfo == null ) {
			System.out.println("RESP Received <"  + respmsg + "> ### Messageinfo Find Error ####");
			System.out.flush();
			return;
		}
		
		// ������ ��ٸ��� Thread�� WakeUp
		msginfo.SetRespMsg(respmsg);
		msginfo.UnLock();
		
		System.out.println("RESP Received Complete "  + id2);
		System.out.flush();
	}
}

///////////////////////////////////////////////////////////////
//
///////////////////////////////////////////////////////////////
public class GPTMessageProcess {
	private Connection cn;
	private Channel chrqst;
	private Channel chresp;
	private String  ServerID;
	private String  RQST_QUEUE;
	private String  RESP_EXCHG;
	private GPTMessageList gptlist;
	
	public GPTMessageProcess(String server,String user,String pwd) {
		ConnectionFactory factory = new ConnectionFactory();

		factory.setHost(server);
		factory.setUsername(user);
		factory.setPassword(pwd);
		try {
			 cn = factory.newConnection();
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		chrqst = null;
		chresp = null;
		ServerID = null;
		RQST_QUEUE = null;
		RESP_EXCHG = null;
		gptlist = null;
	}

	//////////////
	// Init
	//////////////
	public void Init(String serverid,String RQST_QUEUE,String RESP_EXCHG) {
		String respqueue = null;
		
		this.ServerID = serverid;
		this.gptlist = new GPTMessageList();
		this.RQST_QUEUE = RQST_QUEUE;
		this.RESP_EXCHG = RESP_EXCHG;
		
		try {
			chrqst = cn.createChannel();
			chrqst.queueDeclare(RQST_QUEUE, false, false, false, null);				

			// 1�� ���������� ��û�� �����Ͽ� GPT ������ ���
			if ( serverid.equals("01")) {
				chrqst.queueDeclare(RQST_QUEUE, false, false, false, null);				

				GPTRqstConsumer rqstconsumer = new GPTRqstConsumer(chrqst,this);
				chrqst.basicConsume(RQST_QUEUE, true, rqstconsumer);				
			}

			// GPT ������ ���� ������ �����ϸ� ���� �޽����� Queue�� ����
			// ���۵� �޽����� �������� �����Ͽ� ���� ó��
			chresp = cn.createChannel();
			chresp.exchangeDeclare(RESP_EXCHG, "direct");

			// ������ ���� ť�̸��� ExchangeName�� ���� ID�� �ٿ��� ���
			chresp.exchangeDeclare(RESP_EXCHG, "direct");
			respqueue = RESP_EXCHG + serverid;
			
			// Queue Exist Check
			boolean bqueueexist = true;
			try {
				Channel chtemp;
				
				chtemp = cn.createChannel();
				chtemp.queueDeclarePassive(respqueue);
				chtemp.close();
			} catch (Exception e) {
				bqueueexist = false;
			}
			if ( !bqueueexist ) {
				// �ش� ������ �޽����� ����
				chresp.queueDeclare(respqueue, false, false, false, null);				
				chresp.queueBind(respqueue, RESP_EXCHG, serverid);				
			}

			GPTRespConsumer respconsumer = new GPTRespConsumer(chresp,this);
			chresp.basicConsume(respqueue, true, respconsumer);				
					
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	} // Init
	
	//////////////
	// Close
	//////////////
	public void Close() {
		try {
			chresp.close();
			chrqst.close();
			cn.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	//////////////
	// Message Find
	//////////////
	public GPTMessageInfo GetMessageInfo(String msgid) {
		return gptlist.Get(msgid);
	}
	
	//////////////
	// Rqst Send
	//////////////
	public GPTMessageInfo RqstSend(String userid, String msgid, String Msg) {
		GPTMessageInfo	msginfo;
		
		msginfo = new GPTMessageInfo(this.ServerID, msgid, Msg);
		
		gptlist.add(msginfo);
		
		BasicProperties prop = new BasicProperties().builder()
									.messageId(msgid)
									.appId(this.ServerID)
									.contentType(userid)
									.build();

		System.out.println("rqst send waiting.." + Msg );
		System.out.flush();
		synchronized (this) {
			try {
				chrqst.basicPublish("", RQST_QUEUE, prop, Msg.getBytes() );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		System.out.println("rqst send ok.." + Msg );
		System.out.flush();
		
		return msginfo;
	} // RqstSend

	//////////////
	// Resp Send
	//////////////
	public void RespSend(BasicProperties prop, String Msg) {
		String serverid;
		
		serverid = prop.getAppId();
		
		try {
			chresp.basicPublish(RESP_EXCHG, serverid, prop, Msg.getBytes() );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}			
	} // RespSend

	/////////////////////////
	// GPT Commnucation
	/////////////////////////
	public String GPTComm(String UserID,String RqstMsg) {
		String RespMsg = null;
		
		System.out.println("@@@ GPT Processing.......threadid=" + Thread.currentThread().getId() );
		System.out.flush();

		// GPT�� ����ϰ� ������ ����
		try { Thread.sleep(3000);} catch (InterruptedException e) {}		
		RespMsg = "@@@[" + UserID + "] " + RqstMsg + "  .....Processed @@@";
		
		System.out.println(RespMsg);
		System.out.flush();

		return RespMsg;
	} // GPTComm
}
