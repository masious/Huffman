import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;


public class Main {
	private static String HEADER_SPLITTER = "\r\n==========\r\n";
	public static void main(String[] args) {
		compress("file.txt","compressed.txt");
		uncompress("compressed.txt","uncompressed.txt");
	}
	static String readFile(String path){
		byte[] encoded = null;
		try {
			encoded = Files.readAllBytes(Paths.get(path));
		} catch (IOException e) {}
		return new String(encoded, StandardCharsets.ISO_8859_1);
	}
	public static void compress(String inputFile,String outputFile){
		int [] repeats = new int[256];
		CData [] sorted = new CData[256];
		String fileString = readFile(inputFile);
		for(int i=0;i<fileString.length();i++)
			repeats[(char) fileString.charAt(i)]++;
		for(int i=0;i<repeats.length;i++)
			if(repeats[i]!=0)
				addTo( sorted , new CharData( (char) i,repeats[i] ) );
		int i=0;
		while( sorted[i+1] != null ){
			StrData n = new StrData(sorted[i++],sorted[i++]);
			addTo(sorted, n);
		}
		StrData tree = (StrData) sorted[i];
		String output = "";
		for(i=0;i<fileString.length();i++)
			output += tree.getPathTo(fileString.charAt(i));
		try {
			byte[] data =  decodeBinary(output);
			String head = "";
			String [] chars = tree.getCachedPaths();
			for(i=0;i<chars.length;i++)
				if(chars[i]!=null)
					head += ((char)i) +""+ ((char)18) + chars[i] + ((char)17);
			data = new String(head + HEADER_SPLITTER + new String(data)).getBytes();
			java.nio.file.Files.write(new File(outputFile).toPath(),data);
		} catch (IOException e) {}
	}
	static void uncompress(String inputFile,String outputFile){
		String fileString = readFile(inputFile);
		String[] headerAndBody = fileString.split(HEADER_SPLITTER);
		String header = headerAndBody[0];
		String body = headerAndBody[1];
		String[] codes = header.split(""+(char)17);
		StdObject tree = new StdObject();
		String output = "";
		for(String c:codes){
			String[] keyVal = c.split(""+(char)18);
			tree.append(keyVal[0].charAt(0), keyVal[1]);
			System.out.println(keyVal[0].charAt(0)+"=>"+ keyVal[1]);
		}
		StdObject p = tree;
		for(int i=0;i<body.length();i++){
			byte curr = (byte) body.charAt(i);
			System.out.println((char)curr);
			for(int j=7;j>=0;j--){
				if(p.isChar()){
					output += p.getChar();
					p = tree;
				}
				boolean bit = (curr & (1<<j))==0?false:true;
				System.out.println("curr: " + curr + " bit: " + bit + " 1<<j: " + (1<<j));
				curr = (byte) (curr % (1<<j));
				if(bit){
					p = p.getRight();
				} else { // if(bit == 0)
					p = p.getLeft();
				}
			}
		}
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(output);
			writer.flush();
			writer.close();
		} catch (IOException e) {}
	}
	static byte[] decodeBinary(String s) {
	    byte[] data = new byte[s.length() / 8 + 1];
	    for (int i = 0; i < s.length(); i++) {
	        char c = s.charAt(i);
	        if (c == '1') {
	            data[i >> 3] |= 0x80 >> (i & 0x7);
	        } else if (c != '0') {
	            throw new IllegalArgumentException("Invalid char in binary string: "+i);
	        }
	    }
	    return data;
	}

	private static void addTo(CData[] sorted, CData temp) {
		int last = 0;
		for( int i = 0 ; sorted [ i ] != null ; i ++ ){
			if(sorted[i].getRepeats()>temp.getRepeats()){
				CData t = sorted[i];
				sorted[i] = temp;
				temp = t;
			}
			last ++; 
		}
		sorted[last] = temp;
	}
}
class StdObject{
	private String contents = "";
	private boolean isChar = false;
	private StdObject leftChild,rightChild;
	public void append(char c,String path){
		contents += c;
		if(!path.equals("")){
			if(path.charAt(0) == '0')
				addToLeft(c,path.substring(1));
			 else if( path.charAt(0) == '1')
				addToRight(c,path.substring(1));
		} else
			isChar = true;
	}
	private void addToLeft(char c, String substring) {
		if(leftChild == null)
			leftChild = new StdObject();
		leftChild.append(c,substring);
	}
	private void addToRight(char c, String substring) {
		if(rightChild == null)
			rightChild = new StdObject();
		rightChild.append(c,substring);
	}
	public String getChar(String path){
		if(path.length() == 0)
			return contents;
		if(path.charAt(0) == '0')
			return leftChild.getChar(path.substring(1));
		else //if(path.charAt(0) == '1')
			return rightChild.getChar(path.substring(1));
	}
	public char getChar(){
		if(isChar())
			return contents.charAt(0);
		return 0;
	}
	public StdObject getLeft(){
		return leftChild;
	}
	public StdObject getRight(){
		return rightChild;
	}
	public boolean isChar() {
		return isChar;
	}
}
abstract class CData{
	private int repeats;
	public CData(int r) {
		this.repeats = r;
	}
	public void addRepeats(){
		repeats++;
	}
	public int getRepeats(){
		return repeats;
	}
	public abstract String getStr();
}
class StrData extends CData{
	private String str;
	private CData leftChild,rightChild;
	private String [] cachedData = new String[256];
	public StrData(String s,int r) {
		super(r);
		this.str = s;
	}
	public StrData(CData left,CData right){
		this(left.getStr() + right.getStr(),left.getRepeats()+right.getRepeats());
		assignChildren(left, right);
	}
	public String getStr(){
		return str;
	}
	public void assignChildren(CData left,CData right){
		leftChild = left;
		rightChild = right;
	}
	public CData getLeft(){
		return leftChild;
	}
	public CData getRight(){
		return rightChild;
	}
	public String getPathTo(char c){
		if(cachedData[c]!=null)
			return cachedData[c];
		String result = "";
		if(leftChild.getStr().equals(""+c))
			result = "0";
		else if(rightChild.getStr().equals(""+c))
			result = "1";
		if(!result.equals("")){
			cachedData[(int)c] = result;
			return result;
		}
		if(leftChild.getStr().indexOf(c)>=0)
			result = "0" + ((StrData) leftChild).getPathTo(c);
		else if(rightChild.getStr().indexOf(c)>=0)
			result = "1" + ((StrData) rightChild).getPathTo(c);
		cachedData[(int)c] = result;
		return result;
	}
	public String [] getCachedPaths() {
		return cachedData;
	}
}
class CharData extends CData{
	private char ch;
	public CharData(char ch,int r) {
		super(r);
		this.ch = ch;
	}
	public char getChar(){
		return ch;
	}
	public String getStr() {
		return ""+getChar();
	}
}