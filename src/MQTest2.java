

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.WebServlet;

/**
 * Servlet implementation class MQTest2
 */
@WebServlet("/MQTest2")
public class MQTest2 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String MQServerIP = null;
	private String MQRequest = null;
	private String MQResponse = null;
	private String MQUser = null;
	private String MQPassword = null;
	private String MQServerID = null;
	private GPTMessageProcess gptproc = null;

       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public MQTest2() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see Servlet#init(ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		// TODO Auto-generated method stub
		//ServletContext ctx = getServletContext();
		
		MQServerIP = config.getInitParameter("MQServerIP");
		MQRequest = config.getInitParameter("MQRequest");
		MQResponse = config.getInitParameter("MQResponse");
		MQUser = config.getInitParameter("MQUser");
		MQPassword = config.getInitParameter("MQPassword");
		MQServerID = config.getInitParameter("MQServerID");
		
		gptproc = new GPTMessageProcess(MQServerIP, MQUser, MQPassword);
		gptproc.Init(MQServerID, MQRequest, MQResponse);

	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
		gptproc.Close();
	}

    
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		PrintWriter out = null;

		String msgid = request.getParameter("msgid");
		String msg = request.getParameter("msg");
		String userid = request.getParameter("userid");
		GPTMessageInfo msginfo;

		if ( msgid == null )
			msgid = "msg0101";
		if ( msg == null )
			msg = "Test Message";
		if ( userid == null )
			userid = "USER001";
		
		msginfo = gptproc.RqstSend(userid, msgid, msg);
		msginfo.Lock();
		String respmsg = msginfo.GetRespMsg();
		
		System.out.println("###### Do Get :" + respmsg );
		System.out.flush();
		 
		out = response.getWriter();
		
		out.println("<html><body>");
		out.println("<h1 align='center'>" + "context path=" + request.getContextPath() + "</h1>");
		out.println("<h3 align='center'>" + "serverip=" + MQServerIP + "</h3>");
		out.println("<h3 align='center'>" + "Requst Que=" + MQRequest + "</h3>");
		out.println("<h3 align='center'>" + "msgid=" + msgid + "</h3>");
		out.println("<h3 align='center'>" + "msg=" + msg + "</h3>");
		out.println("<h3 align='center'>" + "respmsg=" + respmsg + "</h3>");
		out.println("</body></html>");
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}
}
