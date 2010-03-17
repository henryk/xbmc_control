package ch.ploetzli.xbmc.api.mdns;

import java.io.IOException;

import javax.microedition.io.Datagram;

/* A wrapper around a datagram that keeps track of the current read position */
public class PositionDatagram implements Datagram {
	private int position; /* Keeps track of the current position */
	private boolean tilt; /* Is set when the track is lost because a function is called that reads an unknown number of bytes */ 
	private Datagram parent;
	
	public PositionDatagram(Datagram d)
	{
		this.parent = d;
		this.position = 0;
		this.tilt = false;
	}
	
	public int getPosition() throws IndexOutOfBoundsException
	{
		if(tilt) throw new IndexOutOfBoundsException("PositionDatagram lost track of position");
		return position;
	}
	
	public String getAddress() {
		return parent.getAddress();
	}
	public byte[] getData() {
		return parent.getData();
	}
	public int getLength() {
		return parent.getLength();
	}
	public int getOffset() {
		return parent.getOffset();
	}
	public boolean readBoolean() throws IOException {
		position += 1;
		return parent.readBoolean();
	}
	public byte readByte() throws IOException {
		position += 1;
		return parent.readByte();
	}
	public char readChar() throws IOException {
		position += 1;
		return parent.readChar();
	}
	public double readDouble() throws IOException {
		position += 8;
		return parent.readDouble();
	}
	public float readFloat() throws IOException {
		position += 4;
		return parent.readFloat();
	}
	public void readFully(byte[] arg0, int arg1, int arg2) throws IOException {
		position += arg2;
		parent.readFully(arg0, arg1, arg2);
	}
	public void readFully(byte[] arg0) throws IOException {
		position += arg0.length;
		parent.readFully(arg0);
	}
	public int readInt() throws IOException {
		position += 4;
		return parent.readInt();
	}
	public long readLong() throws IOException {
		position += 8;
		return parent.readLong();
	}
	public short readShort() throws IOException {
		position += 2;
		return parent.readShort();
	}
	public int readUnsignedByte() throws IOException {
		position += 1;
		return parent.readUnsignedByte();
	}
	public int readUnsignedShort() throws IOException {
		position += 2;
		return parent.readUnsignedShort();
	}
	public String readUTF() throws IOException {
		tilt = true;
		return parent.readUTF();
	}
	public void reset() {
		position = 0;
		parent.reset();
	}
	public void setAddress(Datagram arg0) {
		parent.setAddress(arg0);
	}
	public void setAddress(String arg0) throws IOException {
		parent.setAddress(arg0);
	}
	public void setData(byte[] arg0, int arg1, int arg2) {
		position = 0;
		parent.setData(arg0, arg1, arg2);
	}
	public void setLength(int arg0) {
		parent.setLength(arg0);
	}
	public int skipBytes(int arg0) throws IOException {
		position += arg0;
		return parent.skipBytes(arg0);
	}
	public void write(byte[] arg0, int arg1, int arg2) throws IOException {
		position += arg2;
		parent.write(arg0, arg1, arg2);
	}
	public void write(byte[] arg0) throws IOException {
		position += arg0.length;
		parent.write(arg0);
	}
	public void write(int arg0) throws IOException {
		position += 4;
		parent.write(arg0);
	}
	public void writeBoolean(boolean arg0) throws IOException {
		position += 1;
		parent.writeBoolean(arg0);
	}
	public void writeByte(int arg0) throws IOException {
		position += 1;
		parent.writeByte(arg0);
	}
	public void writeChar(int arg0) throws IOException {
		position += 1;
		parent.writeChar(arg0);
	}
	public void writeChars(String arg0) throws IOException {
		position += arg0.length();
		parent.writeChars(arg0);
	}
	public void writeDouble(double arg0) throws IOException {
		position += 8;
		parent.writeDouble(arg0);
	}
	public void writeFloat(float arg0) throws IOException {
		position += 4;
		parent.writeFloat(arg0);
	}
	public void writeInt(int arg0) throws IOException {
		position += 4;
		parent.writeInt(arg0);
	}
	public void writeLong(long arg0) throws IOException {
		position += 8;
		parent.writeLong(arg0);
	}
	public void writeShort(int arg0) throws IOException {
		position += 2;
		parent.writeShort(arg0);
	}
	public void writeUTF(String arg0) throws IOException {
		tilt = true;
		parent.writeUTF(arg0);
	}

}
