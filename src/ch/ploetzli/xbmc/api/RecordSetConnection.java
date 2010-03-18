package ch.ploetzli.xbmc.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import ch.ploetzli.xbmc.Utils;

public class RecordSetConnection implements Enumeration {
	private InputStream is;
	private boolean finished;
	private boolean haveRecordStart = false;
	private boolean isUtf8;

	protected RecordSetConnection(InputStream is, boolean isUtf8)
	{
		this.is = is;
		this.finished = false;
		this.isUtf8 = isUtf8;
	}

	protected RecordSetConnection(InputStream is)
	{
		this(is, false);
	}
	
	public boolean hasMoreElements() {
		return !finished;
	}

	private static final String[] tokens = new String[]{"<html>", "</html>", "<record>", "</record>", "<field>", "</field>"}; 
	public Object nextElement() {
		Vector fields = new Vector();
		boolean done = false;
		boolean haveUnreturned = false;
		boolean fieldStarted = false;
		
		if(!hasMoreElements()) {
			throw new NoSuchElementException();
		}
		
		while(!done) {
			try {
				String r[] = Utils.findRead(is, tokens, isUtf8);
				if(r[1] == tokens[0]) { /* <html> */
					/* Ignore */
				} else if(r[1] == tokens[1] || r[1] == "") { /* </html> */
					/* End of input, discard half-baked record and return nothing, or return 
					 * finished unreturned record */
					
					this.finished = true;
					try {
						this.is.close();
					} catch(IOException e) {
						/* Ignore */
					}
					
					if(!haveUnreturned)
						fields = new Vector();
					done = true;
				} else if(r[1] == tokens[2]) { /* <record> */
					/* Start a record */
					if(!haveRecordStart) {
						haveRecordStart = true;
					}
					
					if(haveUnreturned) {
						/* Return that record now */
						done = true;
					} else {
						/* Otherwise prepare to read a new one. */
						fields = new Vector();
					}
					
				} else if(r[1]== tokens[3]) { /* </record> */
					haveRecordStart = false;
					
					/* We're going to postpone returning this record (now in fields)
					 * until we either see the next record start or stream end. This way
					 * we won't return until the status of hasMoreElements() is accurate.
					 */
					haveUnreturned = true;
				} else if(r[1] == tokens[4]) { /* <field> */
					if(haveRecordStart) {
						fieldStarted = true;
					}
				} else if(r[1] == tokens[5]) { /* </field> */
					if(haveRecordStart) {
						if(fieldStarted) {
							fields.addElement(r[0]);
						}
						fieldStarted = false;
					}
				}
			} catch (IOException e) {
				this.finished = true;
				try {
					this.is.close();
				} catch(IOException e1) {
					/* Ignore */
				}
				return e;
			}
		}
		
		String result[] = new String[fields.size()];
		fields.copyInto(result);
		return result;
	}
	
	public void shutdown() throws IOException
	{
		finished = true;
		is.close();
	}
}
