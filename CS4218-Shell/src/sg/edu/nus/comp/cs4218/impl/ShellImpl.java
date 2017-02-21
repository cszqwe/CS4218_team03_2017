package sg.edu.nus.comp.cs4218.impl;

import java.io.*;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

import sg.edu.nus.comp.cs4218.Application;
import sg.edu.nus.comp.cs4218.Environment;
import sg.edu.nus.comp.cs4218.Shell;
import sg.edu.nus.comp.cs4218.exception.AbstractApplicationException;
import sg.edu.nus.comp.cs4218.exception.ShellException;
import sg.edu.nus.comp.cs4218.impl.app.CatApplication;
import sg.edu.nus.comp.cs4218.impl.app.EchoApplication;
import sg.edu.nus.comp.cs4218.impl.app.HeadApplication;
import sg.edu.nus.comp.cs4218.impl.app.TailApplication;
import sg.edu.nus.comp.cs4218.impl.cmd.CallCommand;
import sg.edu.nus.comp.cs4218.impl.app.PwdApplication;
import sg.edu.nus.comp.cs4218.impl.app.CdApplication;
import sg.edu.nus.comp.cs4218.impl.app.DateApplication;
import sg.edu.nus.comp.cs4218.impl.app.WcApplication;

/**
 * A Shell is a command interpreter and forms the backbone of the entire
 * program. Its responsibility is to interpret commands that the user type and
 * to run programs that the user specify in her command lines.
 * 
 * <p>
 * <b>Command format:</b>
 * <code>&lt;Pipe&gt; | &lt;Sequence&gt; | &lt;Call&gt;</code>
 * </p>
 */

public class ShellImpl implements Shell {
	public static String[] cmdArgs;
	public static final String SPACE_CHAR = "&*^%SpaceChar";
	public static final String EXP_INVALID_APP = "Invalid app.";
	public static final String EXP_SYNTAX = "Invalid syntax encountered.";
	public static final String EXP_REDIR_PIPE = "File output redirection and "
			+ "pipe operator cannot be used side by side.";
	public static final String EXP_SAME_REDIR = "Input redirection file same "
			+ "as output redirection file.";
	public static final String EXP_STDOUT = "Error writing to stdout.";
	public static final String EXP_NOT_SUPPORTED = " not supported yet";

	/**
	 * Searches for and processes the commands enclosed by back quotes for
	 * command substitution.If no back quotes are found, the argsArray from the
	 * input is returned unchanged. If back quotes are found, the back quotes
	 * and its enclosed commands substituted with the output from processing the
	 * commands enclosed in the back quotes.
	 * Note that the back quotes enclosed by single quotes would not work. They would be viewed as normal characters.
	 * @param argsArray
	 *            String of the individual commands.
	 * 
	 * @return String with the back quotes command processed.
	 * 
	 * @throws AbstractApplicationException
	 *             If an exception happens while processing the content in the
	 *             back quotes.
	 * @throws ShellException
	 *             If an exception happens while processing the content in the
	 *             back quotes.
	 */
	public static String processBQ(String stmt)
			throws AbstractApplicationException, ShellException {
		// echo "this is space `echo "nbsp"`"
		// echo "this is space `echo "nbsp"` and `echo "2nd space"`"
		// Back quoted: any char except \n,`
		boolean isQuoted = false;

		while (true){
			int start = -1;
			int end = -1;
			int len = stmt.length();
			for (int i = 0; i < len; i++){ 
				if (stmt.charAt(i) == '\''){
					if (i != 0){
						if (stmt.charAt(i-1) == '\\'){
							continue;
						}else{
							isQuoted = !isQuoted;
						}
					}
				}else if (stmt.charAt(i) == '`' && !isQuoted){
					if (i != 0) {
						if (stmt.charAt(i-1) == '\\') continue;
					}
					start = i;
					end = -1;
					for (int j = i+1; j < len; j++){
						if (stmt.charAt(j) == '`' && stmt.charAt(j-1) != '\\'){
							end = j;
							break;
						}
					}
					break;
				}
			}
			
			if (start == -1) return stmt;
			if (end == -1) throw new ShellException("Shell not completed!");
			String bqStr = stmt.substring(start + 1, end);
			OutputStream bqOutputStream = new ByteArrayOutputStream();
			ShellImpl shell = new ShellImpl();
			ByteArrayOutputStream outByte = (ByteArrayOutputStream) bqOutputStream;
			shell.parseAndEvaluate(bqStr, bqOutputStream);
			byte[] byteArray = outByte.toByteArray();
			String bqResult = new String(byteArray).replace("\n", "")
					.replace("\r", "").replace("\\", "\\\\");
			
			String replacedStr = stmt.replaceFirst("`" + bqStr + "`",
					bqResult);
			stmt = replacedStr;
		}
	}
	
	
	/**
	 * Searches for and processes the commands enclosed by double quotes.If no double quotes are found, the argsArray from the
	 * input is returned unchanged. If double quotes are found, the double quotes
	 * and its enclosed contents substituted with the enclosed contents. 
	 * Note that the double quotes enclosed by single quotes would not work. They would be viewed as normal characters.
	 * @param argsArray
	 *            String of the individual commands.
	 * 
	 * @return String with the double quotes command processed.
	 * 
	 * @throws AbstractApplicationException
	 *             If an exception happens while processing the content in the
	 *             double quotes.
	 * @throws ShellException
	 *             If an exception happens while processing the content in the
	 *             double quotes.
	 */
	public static String processDQ(String stmt)
			throws AbstractApplicationException, ShellException {
		// echo "this is space `echo "nbsp"`"
		// echo "this is space `echo "nbsp"` and `echo "2nd space"`"
		// Back quoted: any char except \n,`
		boolean isQuoted = false;

		while (true){
			int start = -1;
			int end = -1;
			int len = stmt.length();
			for (int i = 0; i < len; i++){ 
				if (stmt.charAt(i) == '\''){
					if (i != 0){
						if (stmt.charAt(i-1) == '\\'){
							continue;
						}else{
							isQuoted = !isQuoted;
						}
					}
				}else if (stmt.charAt(i) == '"' && !isQuoted){
					if (i != 0) {
						if (stmt.charAt(i-1) == '\\') continue;
					}
					start = i;
					end = -1;
					for (int j = i+1; j < len; j++){
						if (stmt.charAt(j) == '"' && stmt.charAt(j-1) != '\\'){
							end = j;
							break;
						}
					}
					break;
				}
			}
			
			if (start == -1) return stmt;
			if (end == -1) throw new ShellException("Shell not completed!");
			String bqStr = stmt.substring(start + 1, end);
			String replacedOne = bqStr.replace(" ", SPACE_CHAR);
			String replacedStr = stmt.replaceFirst('"' + bqStr + '"',
					replacedOne);
			stmt = replacedStr;
		}
	}
	
	/**
	 * Searches for and processes the commands enclosed by single quotes.If no double quotes are found, the argsArray from the
	 * input is returned unchanged. If single quotes are found, the single quotes
	 * and its enclosed contents substituted with the enclosed contents. 
	 * @param argsArray
	 *            String of the individual commands.
	 * 
	 * @return String with the single quotes command processed.
	 * 
	 * @throws AbstractApplicationException
	 *             If an exception happens while processing the content in the
	 *             single  quotes.
	 * @throws ShellException
	 *             If an exception happens while processing the content in the
	 *             single quotes.
	 */
	public static String processSQ(String stmt)
			throws AbstractApplicationException, ShellException {
		// echo "this is space `echo "nbsp"`"
		// echo "this is space `echo "nbsp"` and `echo "2nd space"`"
		// Back quoted: any char except \n,`

		while (true){
			int start = -1;
			int end = -1;
			int len = stmt.length();
			for (int i = 0; i < len; i++){ 
				if (stmt.charAt(i) == '\''){
					if (i != 0) {
						if (stmt.charAt(i-1) == '\\') continue;
					}
					start = i;
					end = -1;
					for (int j = i+1; j < len; j++){
						if (stmt.charAt(j) == '\'' && stmt.charAt(j-1) != '\\'){
							end = j;
							break;
						}
					}
					break;
				}
			}
			
			if (start == -1) return stmt;
			if (end == -1) throw new ShellException("Shell not completed!");
			String bqStr = stmt.substring(start + 1, end);
			String replacedOne = bqStr.replace(" ", SPACE_CHAR);
			String replacedStr = stmt.replaceFirst('\'' + bqStr + '\'',
					replacedOne);
			stmt = replacedStr;
		}
	}	

	/**
	 * Searches for and processes the Semicolon sign. Read in a string, return an array of strings 
	 * @param stmt
	 *            String of the individual commands.
	 * 
	 * @return Array of strings which means multiple commands.
	 * 
	 * @throws AbstractApplicationException
	 *             If an exception happens while processing the content in the
	 *             single  quotes.
	 * @throws ShellException
	 *             If an exception happens while processing the content in the
	 *             single quotes.
	 */
	public static String[] processSemi(String stmt)
			throws AbstractApplicationException, ShellException {
		stmt = stmt+";";
		int numOfDQ = 0;
		int numOfSQ = 0;
		int startIndex = 0;
		ArrayList<String> stmts = new ArrayList<String>();
		stmts.clear();
		int len = stmt.length();
		for (int i = 0 ; i < len; i++){
			if (stmt.charAt(i) =='\''){
				numOfSQ++;
			}else if (stmt.charAt(i) == '"'){
				numOfDQ++;
			}else if (stmt.charAt(i) == ';'){
				if (numOfDQ % 2 == 0 && numOfSQ %2 ==0){
					String newCmd = stmt.substring(startIndex, i);
					stmts.add(newCmd);
					startIndex = i+1;
				}
			}
		}
		String[] commands = new String[stmts.size()];
		stmts.toArray(commands);
		return commands;
	}	

	/**
	 * Searches for and processes the pipe sign. Read in a string, return an array of strings 
	 * @param stmt
	 *            String of the individual commands.
	 * 
	 * @return Array of strings which means multiple commands.
	 * 
	 * @throws AbstractApplicationException
	 *             If an exception happens while processing the content in the
	 *             single  quotes.
	 * @throws ShellException
	 *             If an exception happens while processing the content in the
	 *             single quotes.
	 */
	public static String[] processPipe(String stmt)
			throws AbstractApplicationException, ShellException {
		stmt = stmt+"|";
		int numOfDQ = 0;
		int numOfSQ = 0;
		int startIndex = 0;
		ArrayList<String> stmts = new ArrayList<String>();
		stmts.clear();
		int len = stmt.length();
		for (int i = 0 ; i < len; i++){
			if (stmt.charAt(i) =='\''){
				numOfSQ++;
			}else if (stmt.charAt(i) == '"'){
				numOfDQ++;
			}else if (stmt.charAt(i) == '|'){
				if (numOfDQ % 2 == 0 && numOfSQ %2 ==0){
					String newCmd = stmt.substring(startIndex, i);
					stmts.add(newCmd);
					startIndex = i+1;
				}
			}
		}
		String[] commands = new String[stmts.size()];
		stmts.toArray(commands);
		return commands;
	}	


	
	/**
	 * Static method to run the application as specified by the application
	 * command keyword and arguments.
	 * 
	 * @param app
	 *            String containing the keyword that specifies what application
	 *            to run.
	 * @param args
	 *            String array containing the arguments to pass to the
	 *            applications for running.
	 * @param inputStream
	 *            InputputStream for the application to get arguments from, if
	 *            needed.
	 * @param outputStream
	 *            OutputStream for the application to print its output to.
	 * 
	 * @throws AbstractApplicationException
	 *             If an exception happens while running any of the
	 *             application(s).
	 * @throws ShellException
	 *             If an unsupported or invalid application command is detected.
	 */
	public static void runApp(String app, String[] argsArray,
			InputStream inputStream, OutputStream outputStream)
			throws AbstractApplicationException, ShellException {
		Application absApp = null;
		if (("cat").equals(app)) {// cat [FILE]...
			absApp = new CatApplication();
		} else if (("echo").equals(app)) {// echo [args]...
			absApp = new EchoApplication();
		} else if (("head").equals(app)) {// head [OPTIONS] [FILE]
			absApp = new HeadApplication();
		} else if (("tail").equals(app)) {// tail [OPTIONS] [FILE]
			absApp = new TailApplication();
		} else if (("cd").equals(app)) {
			absApp = new CdApplication();
		} else if (("pwd").equals(app)) {
			absApp = new PwdApplication();
		} else if (("date").equals(app)) {
			absApp = new DateApplication();
		} else if (("wc").equals(app)) {
			absApp = new WcApplication();
		} else { // invalid command
			throw new ShellException(app + ": " + EXP_INVALID_APP);
		}
		absApp.run(argsArray, inputStream, outputStream);
	}

	/**
	 * Static method to creates an inputStream based on the file name or file
	 * path.
	 * 
	 * @param inputStreamS
	 *            String of file name or file path
	 * 
	 * @return InputStream of file opened
	 * 
	 * @throws ShellException
	 *             If file is not found.
	 */
	public static InputStream openInputRedir(String inputStreamS)
			throws ShellException {
		File inputFile = new File(inputStreamS);
		FileInputStream fInputStream = null;
		try {
			fInputStream = new FileInputStream(inputFile);
		} catch (FileNotFoundException e) {
			throw new ShellException(e.getMessage());
		}
		return fInputStream;
	}

	/**
	 * Static method to creates an outputStream based on the file name or file
	 * path.
	 * 
	 * @param onputStreamS
	 *            String of file name or file path.
	 * 
	 * @return OutputStream of file opened.
	 * 
	 * @throws ShellException
	 *             If file destination cannot be opened or inaccessible.
	 */
	public static OutputStream openOutputRedir(String outputStreamS)
			throws ShellException {
		File outputFile = new File(outputStreamS);
		FileOutputStream fOutputStream = null;
		try {
			fOutputStream = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			throw new ShellException(e.getMessage());
		}
		return fOutputStream;
	}

	/**
	 * Static method to close an inputStream.
	 * 
	 * @param inputStream
	 *            InputStream to be closed.
	 * 
	 * @throws ShellException
	 *             If inputStream cannot be closed successfully.
	 */
	public static void closeInputStream(InputStream inputStream)
			throws ShellException {
		if (inputStream != System.in) {
			try {
				inputStream.close();
			} catch (IOException e) {
				throw new ShellException(e.getMessage());
			}
		}
	}

	/**
	 * Static method to close an outputStream. If outputStream provided is
	 * System.out, it will be ignored.
	 * 
	 * @param outputStream
	 *            OutputStream to be closed.
	 * 
	 * @throws ShellException
	 *             If outputStream cannot be closed successfully.
	 */
	public static void closeOutputStream(OutputStream outputStream)
			throws ShellException {
		if (outputStream != System.out) {
			try {
				outputStream.close();
			} catch (IOException e) {
				throw new ShellException(e.getMessage());
			}
		}
	}

	/**
	 * Static method to write output of an outputStream to another outputStream,
	 * usually System.out.
	 * 
	 * @param outputStream
	 *            Source outputStream to get stream from.
	 * @param stdout
	 *            Destination outputStream to write stream to.
	 * @throws ShellException
	 *             If exception is thrown during writing.
	 */
	public static void writeToStdout(OutputStream outputStream,
			OutputStream stdout) throws ShellException {
		if (outputStream instanceof FileOutputStream) {
			return;
		}
		try {
			stdout.write(((ByteArrayOutputStream) outputStream).toByteArray());
		} catch (IOException e) {
			throw new ShellException(EXP_STDOUT);
		}
	}

	/**
	 * Static method to pipe data from an outputStream to an inputStream, for
	 * the evaluation of the Pipe Commands.
	 * 
	 * @param outputStream
	 *            Source outputStream to get stream from.
	 * 
	 * @return InputStream with data piped from the outputStream.
	 * 
	 * @throws ShellException
	 *             If exception is thrown during piping.
	 */
	public static InputStream outputStreamToInputStream(
			OutputStream outputStream) throws ShellException {
		return new ByteArrayInputStream(
				((ByteArrayOutputStream) outputStream).toByteArray());
	}

	/**
	 * Main method for the Shell Interpreter program.
	 * 
	 * @param args
	 *            List of strings arguments, unused.
	 */

	public static void main(String[] args) {
		ShellImpl shell = new ShellImpl();
		ShellImpl.cmdArgs = args;

		BufferedReader bReader = new BufferedReader(new InputStreamReader(
				System.in));
		String readLine = null;
		String currentDir;

		while (true) {
			try {
				currentDir = Environment.currentDirectory;
				System.out.print(currentDir + ">");
				readLine = bReader.readLine();
				if (readLine == null) {
					break;
				}
				if (("").equals(readLine)) {
					continue;
				}
				String[] stmts = processSemi(readLine);
				for (int i = 0; i < stmts.length; i++){
					shell.parseAndEvaluate(stmts[i], System.out);
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	@Override
	public void parseAndEvaluate(String cmdline, OutputStream stdout)
			throws AbstractApplicationException, ShellException {
		String[] allStmts = processPipe(cmdline);
		PipedInputStream in = new PipedInputStream();  
		PipedOutputStream out;
		try{
			out = new PipedOutputStream(in);  
		}catch (Exception e){
			throw new ShellException("Wrong when generating piped output stream");
		}
		PipedInputStream tmpIn = null;  
		PipedOutputStream tmpout = null;

		InputStream actualIn;
		OutputStream actualOut;
		for (int i = 0; i < allStmts.length; i++){
			CallCommand callCommand = new CallCommand(allStmts[i]);
			callCommand.parse();
			if (i == 0){
				actualIn = System.in;
			}else if (i == 1){
				actualIn = in;
			}else{
				actualIn = tmpIn;
			}
			if (i == allStmts.length -1){
				actualOut = stdout;
			}else{
				tmpIn = new PipedInputStream();
				try{
					tmpout = new PipedOutputStream(tmpIn);  
				}catch (Exception e){
					throw new ShellException("Wrong when generating piped output stream");
				}	
				if (i == 0){
					actualOut = out;
				}else{
					actualOut = tmpout;
			
				}
			}
			callCommand.evaluate(actualIn, actualOut);
			
		}
	}

	@Override
	public String pipeTwoCommands(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String pipeMultipleCommands(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String pipeWithException(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String globNoPaths(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String globOneFile(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String globFilesDirectories(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String globWithException(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String redirectInput(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String redirectOutput(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String redirectInputWithNoFile(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String redirectOutputWithNoFile(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String redirectInputWithException(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String redirectOutputWithException(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String performCommandSubstitution(String args) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String performCommandSubstitutionWithException(String args) {
		// TODO Auto-generated method stub
		return null;
	}
}
