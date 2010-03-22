package ch.ploetzli.xbmc.j2me;

public class Logger {
	private static Logger logger;
	
	public synchronized static Logger getLogger() {
		if(logger == null)
			logger = new Logger();
		return logger;
	}
	
	protected Logger() {
		
	}
	
	public void info(String s) {
		System.out.println(s);
	}
	
	
	public void error(String s) {
		System.err.println(s);
	}

	public void info(Exception e) { info(e.toString()); }
	public void error(Exception e) { error(e.toString()); }

}
