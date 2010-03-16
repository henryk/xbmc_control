package ch.ploetzli.xbmc.j2me;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class ExampleCanvas extends Canvas {

	/* (non-Javadoc)
	 * @see javax.microedition.lcdui.Canvas#paint(javax.microedition.lcdui.Graphics)
	 */
	private String message = "MTJ Rocks...";
	protected void paint(Graphics g) {
		g.setColor(0xFFFFFF);
		g.fillRect(0x00, 0x00, getWidth(), getHeight());

		Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD | Font.STYLE_ITALIC | Font.STYLE_UNDERLINED, Font.SIZE_LARGE);
		g.setFont(font);
		
		g.setColor(0x000000);
		g.drawString(message, getWidth()/2, getHeight()/2, Graphics.HCENTER | Graphics.TOP);
	}

	public void setMessage(String string) {
		this.message = string;
	}
	
}
